package com.example.springstudentmanagergrade.service;

import com.example.springstudentmanagergrade.model.Student;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
public class HibernateStudentService implements IStudentService {

    private static SessionFactory sessionFactory;
    private static EntityManager entityManager;

    @Value("${upload.path}")   // đọc từ application.properties
    private String uploadPath;

    static {
        try {
            sessionFactory = new Configuration()
                    .configure("hibernate.conf.xml")
                    .buildSessionFactory();
            entityManager = sessionFactory.createEntityManager();
        } catch (HibernateException e) {
            throw new RuntimeException("Không thể tạo SessionFactory", e);
        }
    }

    @Override
    public List<Student> findAll(String q, String sort, String dir, int page, int size) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("FROM Student s WHERE 1=1 ");

            if (q != null && !q.trim().isEmpty()) {
                hql.append("AND (LOWER(s.mssv) LIKE :q OR LOWER(s.hoTen) LIKE :q) ");
            }

            String sortField;
            switch (sort != null ? sort : "") {
                case "name":
                    sortField = "s.hoTen";
                    break;
                case "gpa":
                    sortField = "s.diemTongKet";
                    break;
                default:
                    sortField = "s.mssv";
            }
            String direction = "desc".equalsIgnoreCase(dir) ? "DESC" : "ASC";
            hql.append("ORDER BY ").append(sortField).append(" ").append(direction);

            Query<Student> queryObj = session.createQuery(hql.toString(), Student.class);

            if (q != null && !q.trim().isEmpty()) {
                queryObj.setParameter("q", "%" + q.trim().toLowerCase() + "%");
            }

            int pageIndex = (page <= 0) ? 1 : page;
            int pageSize = (size <= 0) ? 5 : size;
            queryObj.setFirstResult((pageIndex - 1) * pageSize);
            queryObj.setMaxResults(pageSize);

            return queryObj.list();
        }
    }

    @Override
    public Student findById(int id) {
        String jpql = "SELECT s FROM Student s WHERE s.id = :id";
        TypedQuery<Student> query = entityManager.createQuery(jpql, Student.class);
        query.setParameter("id", id);

        try {
            return query.getSingleResult();
        } catch (NoResultException e) {
            return null; // hoặc throw custom exception nếu muốn
        }
    }

    @Override
    public void create(Student s) {
        Transaction transaction = null;
        Student origin;
        if (s.getId() == 0) {
            origin = new Student();
        } else {
            origin = findById(s.getId());
        }
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            origin.setMssv(s.getMssv());
            origin.setHoTen(s.getHoTen());
            origin.setDiemTongKet(s.getDiemTongKet());
            origin.setAvatar(s.getAvatar());
            session.save(origin);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }

    @Override
    public void update(Student s) {
        Transaction transaction = null;
        Student origin;
        if (s.getId() == 0) {
            origin = new Student();
        } else {
            origin = findById(s.getId());
        }
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            origin.setMssv(s.getMssv());
            origin.setHoTen(s.getHoTen());
            origin.setDiemTongKet(s.getDiemTongKet());
            origin.setAvatar(s.getAvatar());
            session.update(origin);
            transaction.commit();
        } catch (Exception e) {
            e.printStackTrace();
            if (transaction != null) {
                transaction.rollback();
            }
        }
    }

    @Override
    public void delete(int id) {
        Student student = findById(id);
        if (student != null) {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                session.delete(student);
                transaction.commit();
            } catch (Exception e) {
                e.printStackTrace();
                if (transaction != null) {
                    transaction.rollback();
                }
            }
        }
    }

    @Override
    public boolean existsById(String mssv) {
        try (Session session = sessionFactory.openSession()) {
            String hql = "SELECT COUNT(s) FROM Student s WHERE s.mssv = :mssv";
            Query<Long> query = session.createQuery(hql, Long.class);
            query.setParameter("mssv", mssv);
            Long count = query.uniqueResult();
            return count != null && count > 0;
        }
    }

    @Override
    public long count(String q) {
        try (Session session = sessionFactory.openSession()) {
            StringBuilder hql = new StringBuilder("SELECT COUNT(s) FROM Student s WHERE 1=1 ");
            if (q != null && !q.trim().isEmpty()) {
                hql.append("AND (LOWER(s.mssv) LIKE :q OR LOWER(s.hoTen) LIKE :q) ");
            }

            Query<Long> query = session.createQuery(hql.toString(), Long.class);
            if (q != null && !q.trim().isEmpty()) {
                query.setParameter("q", "%" + q.trim().toLowerCase() + "%");
            }

            return query.uniqueResult();  // trả về 1 Long
        }
    }

    @Override
    public String saveFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }
        try {
            // tạo tên file duy nhất
            String originalFileName = file.getOriginalFilename();
            String extension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String newFileName = System.currentTimeMillis() + "_" + UUID.randomUUID() + extension;

            // lưu file vào thư mục uploadPath
            Path path = Paths.get(uploadPath, newFileName);
            Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

            // trả về path để lưu trong DB (đường dẫn web)
            return newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi upload file: " + e.getMessage(), e);
        }
    }
}
