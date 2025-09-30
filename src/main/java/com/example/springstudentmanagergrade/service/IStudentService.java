package com.example.springstudentmanagergrade.service;

import com.example.springstudentmanagergrade.model.Student;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IStudentService {
    List<Student> findAll(String q, String sort, String dir, int page, int size);

//    Student findById(String id);

    Student findById(int id);

    void create(Student s);

    void update(Student s);

//    void delete(String id);

    void delete(int id);

    boolean existsById(String id);

    long count(String q);

    String saveFile(MultipartFile file);
}

