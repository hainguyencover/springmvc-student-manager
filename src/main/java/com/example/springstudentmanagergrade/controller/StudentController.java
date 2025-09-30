package com.example.springstudentmanagergrade.controller;

import com.example.springstudentmanagergrade.model.Student;
import com.example.springstudentmanagergrade.service.IStudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/students")
public class StudentController {
    @Value("${upload.path}")
    private String uploadPath;

    @Autowired
    private IStudentService studentService;

    // --- Hàm xử lý upload file ---
    private void handleFileUpload(Student student) {
        MultipartFile avatarFile = student.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String originalFileName = avatarFile.getOriginalFilename();
                String extension = "";
                if (originalFileName != null && originalFileName.contains(".")) {
                    extension = originalFileName.substring(originalFileName.lastIndexOf("."));
                }
                // Tạo tên file mới, duy nhất để tránh bị ghi đè
                String newFileName = UUID.randomUUID().toString() + extension;
                Path filePath = Paths.get(uploadPath, newFileName);

                // Lưu file vào thư mục đã cấu hình
                Files.copy(avatarFile.getInputStream(), filePath);

                // Lưu đường dẫn web vào đối tượng student
                student.setAvatar("/uploads/" + newFileName);
            } catch (IOException e) {
                // In ra lỗi và bỏ qua, không set avatar nếu có lỗi
                e.printStackTrace();
            }
        }
    }

    @GetMapping
    public ModelAndView list(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "mssv") String sort,
            @RequestParam(defaultValue = "asc") String dir,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        List<Student> students = studentService.findAll(q, sort, dir, page, size);
        long total = studentService.count(q);
        int totalPages = (int) Math.ceil((double) total / size);

        ModelAndView mav = new ModelAndView("students/list");
        mav.addObject("students", students);
        mav.addObject("q", q);
        mav.addObject("sort", sort);
        mav.addObject("dir", dir);
        mav.addObject("page", page);
        mav.addObject("size", size);
        mav.addObject("totalPages", totalPages);
        mav.addObject("total", total);
        return mav;
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable("id") String id,
            Model model,
            RedirectAttributes redirectAttributes) {
        Student s = studentService.findById(id); // gọi service để tìm sinh viên theo MSSV
        if (s == null) {
            redirectAttributes.addFlashAttribute("message",
                    "Không tìm thấy sinh viên có MSSV = " + id);
            return "redirect:/students";
        }
        model.addAttribute("student", s);
        // Chỉ cần trả về "students/student-detail" (KHÔNG có .jsp, KHÔNG redirect)
        return "students/detail";
    }

    @GetMapping("/add")
    public String showAddForm(Model model) {
        // Truyền object trống để binding với form JSP
        model.addAttribute("studentForm", new Student());
        return "students/add";
    }

    @PostMapping("/add")
    public String doAdd(
            @ModelAttribute("studentForm") Student studentForm,
            BindingResult binding,
            RedirectAttributes ra,
            Model model) {
        // Validation (giữ nguyên code của bạn)
        if (studentForm.getMssv() == null || studentForm.getMssv().trim().isEmpty()) {
            binding.rejectValue("mssv", "mssv.empty", "MSSV không được để trống");
        } else if (studentService.existsById(studentForm.getMssv())) {
            binding.rejectValue("mssv", "mssv.exists", "MSSV đã tồn tại");
        }
        if (studentForm.getHoTen() == null || studentForm.getHoTen().trim().isEmpty()) {
            binding.rejectValue("hoTen", "hoTen.empty", "Họ tên không được để trống");
        }
        if (studentForm.getDiemTongKet() < 0.0 || studentForm.getDiemTongKet() > 10.0) {
            binding.rejectValue("diemTongKet", "gpa.invalid", "Điểm tổng kết phải từ 0.0 đến 10.0");
        }
        if (binding.hasErrors()) {
            return "students/add";
        }
        // Xử lý upload file
        handleFileUpload(studentForm);
        studentService.create(studentForm);
        ra.addFlashAttribute("message", "Thêm sinh viên thành công!");
        return "redirect:/students";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable("id") String id,
                               Model model,
                               RedirectAttributes ra) {
        Student s = studentService.findById(id);
        if (s == null) {
            ra.addFlashAttribute("message", "Không tìm thấy sinh viên có MSSV = " + id);
            return "redirect:/students";
        }
        model.addAttribute("studentForm", s);
        return "students/edit";
    }

    @PostMapping("/{id}/edit")
    public String doEdit(@PathVariable("id") String id,
                         @ModelAttribute("studentForm") Student studentForm,
                         BindingResult binding,
                         RedirectAttributes ra) {
        if (!id.equals(studentForm.getMssv()) || !studentService.existsById(id)) {
            ra.addFlashAttribute("message", "Không tìm thấy sinh viên");
            return "redirect:/students";
        }

        if (studentForm.getHoTen() == null || studentForm.getHoTen().trim().isEmpty()) {
            binding.rejectValue("hoTen", "hoTen.empty", "Họ tên không được để trống");
        }
        if (studentForm.getDiemTongKet() < 0.0 || studentForm.getDiemTongKet() > 10.0) {
            binding.rejectValue("diemTongKet", "gpa.invalid", "Điểm tổng kết phải từ 0.0 đến 10.0");
        }

        if (binding.hasErrors()) {
            return "students/edit";
        }

        // update dữ liệu
        Student existing = studentService.findById(id);
        // cập nhật thông tin cơ bản
        existing.setHoTen(studentForm.getHoTen());
        existing.setDiemTongKet(studentForm.getDiemTongKet());

        // xử lý file mới (nếu có)
        MultipartFile avatarFile = studentForm.getAvatarFile();
        if (avatarFile != null && !avatarFile.isEmpty()) {
            String fileName = studentService.saveFile(avatarFile); // tự viết hàm saveFile() lưu file vào thư mục uploads
            existing.setAvatar("/uploads/" + fileName);
        }

        studentService.update(existing);

        ra.addFlashAttribute("message", "Cập nhật sinh viên thành công!");
        return "redirect:/students";
    }

    @PostMapping("/{id}/delete")
    public String doDelete(@PathVariable("id") String id,
                           RedirectAttributes ra) {
        if (!studentService.existsById(id)) {
            ra.addFlashAttribute("message", "Không tìm thấy sinh viên có MSSV = " + id);
            return "redirect:/students";
        }
        studentService.delete(id);
        ra.addFlashAttribute("message", "Xóa sinh viên thành công!");
        return "redirect:/students";
    }
}

