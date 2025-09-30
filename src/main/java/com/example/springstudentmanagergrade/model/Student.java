package com.example.springstudentmanagergrade.model;

import org.springframework.web.multipart.MultipartFile;

import javax.persistence.*;

@Entity
@Table(name = "students")
public class Student {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String mssv;
    private String hoTen;
    private double diemTongKet;
    // 1. Dùng String để lưu đường dẫn (PATH)
    private String avatar;

    // 2. Dùng MultipartFile để nhận dữ liệu file từ form (TRANSIENT)
    @Transient
    private MultipartFile avatarFile;

    public Student() {
    }

    public Student(int id, String mssv, String hoTen, double diemTongKet, String avatar) {
        this.id = id;
        this.mssv = mssv;
        this.hoTen = hoTen;
        this.diemTongKet = diemTongKet;
        this.avatar = avatar;
    }

    public Student(String mssv, String hoTen, double diemTongKet, String avatar) {
        this.mssv = mssv;
        this.hoTen = hoTen;
        this.diemTongKet = diemTongKet;
        this.avatar = avatar;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMssv() {
        return mssv;
    }

    public String getHoTen() {
        return hoTen;
    }

    public double getDiemTongKet() {
        return diemTongKet;
    }

    public void setMssv(String mssv) {
        this.mssv = mssv;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public void setDiemTongKet(double diemTongKet) {
        this.diemTongKet = diemTongKet;
    }

    public XepLoai getXepLoai() {
        return XepLoai.fromGpa(diemTongKet);
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public MultipartFile getAvatarFile() {
        return avatarFile;
    }

    public void setAvatarFile(MultipartFile avatarFile) {
        this.avatarFile = avatarFile;
    }
}
