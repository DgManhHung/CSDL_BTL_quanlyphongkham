-- =====================================================
-- PHÂN HỆ 5: HÓA ĐƠN - DOANH THU - THANH TOÁN
-- Project 1: Hệ thống Quản lý Phòng khám tư nhân
-- Fixed for MySQL 5.7 / 8.0 (no problematic triggers)
-- =====================================================

CREATE DATABASE IF NOT EXISTS phongkham_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE phongkham_db;

-- =====================================================
-- BẢNG CƠ BẢN
-- =====================================================

CREATE TABLE IF NOT EXISTS BacSi (
    MaBS VARCHAR(10) PRIMARY KEY,
    CMT VARCHAR(12) UNIQUE NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    NgaySinh DATE,
    DiaChi VARCHAR(255),
    BacNghe VARCHAR(50),
    ThamNien INT,
    TrinhDo VARCHAR(50),
    ChuyenMon VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS YTa (
    MaNV VARCHAR(10) PRIMARY KEY,
    CMT VARCHAR(12) UNIQUE NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    TrinhDo VARCHAR(50),
    ThamNien INT,
    NgaySinh DATE,
    DiaChi VARCHAR(255),
    SDT VARCHAR(15)
);

CREATE TABLE IF NOT EXISTS BenhNhan (
    MaBN VARCHAR(10) PRIMARY KEY,
    CMT VARCHAR(12) UNIQUE NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    NgaySinh DATE,
    DiaChi VARCHAR(255),
    SDT VARCHAR(15)
);

CREATE TABLE IF NOT EXISTS DanhMucBenh (
    MaBenh VARCHAR(10) PRIMARY KEY,
    TenBenh VARCHAR(100) NOT NULL,
    MoTa TEXT
);

CREATE TABLE IF NOT EXISTS LanKham (
    MaLanKham VARCHAR(15) PRIMARY KEY,
    MaBN VARCHAR(10) NOT NULL,
    MaBS VARCHAR(10) NOT NULL,
    MaBenh VARCHAR(10) NOT NULL,
    NgayVao DATE NOT NULL,
    NgayRa DATE,
    TongTienKham DECIMAL(12,0) DEFAULT 0,
    TrangThai ENUM('DangDieuTri', 'DaKhoi', 'ChuyenVien') DEFAULT 'DangDieuTri',
    GhiChu TEXT,
    FOREIGN KEY (MaBN) REFERENCES BenhNhan(MaBN),
    FOREIGN KEY (MaBS) REFERENCES BacSi(MaBS),
    FOREIGN KEY (MaBenh) REFERENCES DanhMucBenh(MaBenh)
);

CREATE TABLE IF NOT EXISTS Thuoc (
    MaThuoc VARCHAR(10) PRIMARY KEY,
    TenThuoc VARCHAR(100) NOT NULL,
    DonVi VARCHAR(20),
    GiaBan DECIMAL(12,0) NOT NULL,
    SoLuongTon INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS DonThuoc (
    MaDonThuoc VARCHAR(15) PRIMARY KEY,
    MaLanKham VARCHAR(15) NOT NULL,
    NgayKeDon DATE NOT NULL,
    FOREIGN KEY (MaLanKham) REFERENCES LanKham(MaLanKham)
);

CREATE TABLE IF NOT EXISTS ChiTietDonThuoc (
    MaDonThuoc VARCHAR(15),
    MaThuoc VARCHAR(10),
    SoLuong INT NOT NULL,
    DonGia DECIMAL(12,0) NOT NULL,
    ThanhTien DECIMAL(12,0) NOT NULL,
    PRIMARY KEY (MaDonThuoc, MaThuoc),
    FOREIGN KEY (MaDonThuoc) REFERENCES DonThuoc(MaDonThuoc),
    FOREIGN KEY (MaThuoc) REFERENCES Thuoc(MaThuoc)
);

-- =====================================================
-- BẢNG PHÂN HỆ 5
-- =====================================================

CREATE TABLE IF NOT EXISTS HoaDon (
    MaHoaDon VARCHAR(15) PRIMARY KEY,
    MaLanKham VARCHAR(15) NOT NULL,
    NgayLap DATETIME DEFAULT CURRENT_TIMESTAMP,
    TienKham DECIMAL(12,0) DEFAULT 0,
    TienThuoc DECIMAL(12,0) DEFAULT 0,
    TongTien DECIMAL(12,0) DEFAULT 0,
    TrangThai ENUM('ChuaThanhToan', 'DaThanhToan', 'Huy') DEFAULT 'ChuaThanhToan',
    NguoiLap VARCHAR(50),
    GhiChu TEXT,
    FOREIGN KEY (MaLanKham) REFERENCES LanKham(MaLanKham)
);

CREATE TABLE IF NOT EXISTS ThanhToan (
    MaThanhToan VARCHAR(15) PRIMARY KEY,
    MaHoaDon VARCHAR(15) NOT NULL,
    NgayThanhToan DATETIME DEFAULT CURRENT_TIMESTAMP,
    SoTien DECIMAL(12,0) NOT NULL,
    PhuongThuc ENUM('TienMat', 'ChuyenKhoan', 'The', 'ViDienTu') DEFAULT 'TienMat',
    NguoiThu VARCHAR(50),
    GhiChu TEXT,
    FOREIGN KEY (MaHoaDon) REFERENCES HoaDon(MaHoaDon)
);

CREATE TABLE IF NOT EXISTS NguoiDung (
    Username VARCHAR(50) PRIMARY KEY,
    PasswordHash VARCHAR(255) NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    VaiTro ENUM('Admin', 'LeTan', 'BacSi', 'YTa') NOT NULL,
    MaLienKet VARCHAR(10),
    TrangThai ENUM('Active', 'Inactive') DEFAULT 'Active',
    NgayTao DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =====================================================
-- DỮ LIỆU MẪU
-- =====================================================

INSERT IGNORE INTO BacSi (MaBS, CMT, HoTen, NgaySinh, DiaChi, BacNghe, ThamNien, TrinhDo, ChuyenMon) VALUES
('BS001', '001234567890', 'Nguyễn Văn An', '1980-05-15', 'Hà Nội', 'Bác sĩ chính', 15, 'Thạc sĩ', 'Nội khoa'),
('BS002', '001234567891', 'Trần Thị Bình', '1985-08-20', 'Hà Nội', 'Bác sĩ', 10, 'Đại học', 'Nhi khoa');

INSERT IGNORE INTO YTa (MaNV, CMT, HoTen, TrinhDo, ThamNien, NgaySinh, DiaChi, SDT) VALUES
('YT001', '001234567892', 'Lê Thị Cúc', 'Cao đẳng', 8, '1990-03-10', 'Hà Nội', '0912345678'),
('YT002', '001234567893', 'Phạm Văn Dũng', 'Trung cấp', 5, '1995-07-22', 'Hà Nội', '0987654321');

INSERT IGNORE INTO BenhNhan (MaBN, CMT, HoTen, NgaySinh, DiaChi, SDT) VALUES
('BN001', '001111111111', 'Hoàng Văn Em', '1992-01-15', 'Cầu Giấy, Hà Nội', '0901111111'),
('BN002', '001111111112', 'Ngô Thị Hoa', '1988-11-30', 'Đống Đa, Hà Nội', '0902222222'),
('BN003', '001111111113', 'Vũ Minh Khoa', '2000-06-05', 'Hai Bà Trưng, Hà Nội', '0903333333');

INSERT IGNORE INTO DanhMucBenh (MaBenh, TenBenh, MoTa) VALUES
('B001', 'Cảm cúm', 'Cảm lạnh thông thường'),
('B002', 'Viêm họng', 'Viêm họng cấp'),
('B003', 'Đau dạ dày', 'Viêm loét dạ dày'),
('B004', 'Cao huyết áp', 'Tăng huyết áp');

INSERT IGNORE INTO LanKham (MaLanKham, MaBN, MaBS, MaBenh, NgayVao, NgayRa, TongTienKham, TrangThai) VALUES
('LK001', 'BN001', 'BS001', 'B001', '2026-09-05', '2026-09-07', 350000, 'DaKhoi'),
('LK002', 'BN001', 'BS001', 'B002', '2026-09-15', '2026-09-16', 280000, 'DaKhoi'),
('LK003', 'BN002', 'BS002', 'B003', '2026-09-10', '2026-09-14', 520000, 'DaKhoi'),
('LK004', 'BN003', 'BS001', 'B001', '2026-09-20', '2026-09-22', 350000, 'DaKhoi'),
('LK005', 'BN002', 'BS002', 'B004', '2026-10-01', NULL, 450000, 'DangDieuTri'),
('LK006', 'BN001', 'BS001', 'B003', '2026-10-03', '2026-10-05', 480000, 'DaKhoi');

INSERT IGNORE INTO Thuoc (MaThuoc, TenThuoc, DonVi, GiaBan, SoLuongTon) VALUES
('T001', 'Paracetamol 500mg', 'Viên', 2000, 5000),
('T002', 'Amoxicillin 500mg', 'Viên', 3500, 3000),
('T003', 'Omeprazole 20mg', 'Viên', 4500, 2000),
('T004', 'Vitamin C 1000mg', 'Viên', 1500, 4000),
('T005', 'Losartan 50mg', 'Viên', 6000, 1500);

INSERT IGNORE INTO DonThuoc (MaDonThuoc, MaLanKham, NgayKeDon) VALUES
('DT001', 'LK001', '2026-09-05'),
('DT002', 'LK002', '2026-09-15'),
('DT003', 'LK003', '2026-09-10'),
('DT004', 'LK004', '2026-09-20'),
('DT005', 'LK006', '2026-10-03');

INSERT IGNORE INTO ChiTietDonThuoc (MaDonThuoc, MaThuoc, SoLuong, DonGia, ThanhTien) VALUES
('DT001', 'T001', 20, 2000, 40000),
('DT001', 'T004', 10, 1500, 15000),
('DT002', 'T002', 15, 3500, 52500),
('DT003', 'T003', 30, 4500, 135000),
('DT003', 'T001', 10, 2000, 20000),
('DT004', 'T001', 20, 2000, 40000),
('DT004', 'T004', 15, 1500, 22500),
('DT005', 'T003', 28, 4500, 126000);

INSERT IGNORE INTO HoaDon (MaHoaDon, MaLanKham, NgayLap, TienKham, TienThuoc, TongTien, TrangThai, NguoiLap) VALUES
('HD001', 'LK001', '2026-09-07 10:30:00', 350000, 55000, 405000, 'DaThanhToan', 'letan01'),
('HD002', 'LK002', '2026-09-16 14:00:00', 280000, 52500, 332500, 'DaThanhToan', 'letan01'),
('HD003', 'LK003', '2026-09-14 09:15:00', 520000, 155000, 675000, 'DaThanhToan', 'admin'),
('HD004', 'LK004', '2026-09-22 16:45:00', 350000, 62500, 412500, 'DaThanhToan', 'letan01'),
('HD005', 'LK006', '2026-10-05 11:20:00', 480000, 126000, 606000, 'ChuaThanhToan', 'letan01');

INSERT IGNORE INTO ThanhToan (MaThanhToan, MaHoaDon, NgayThanhToan, SoTien, PhuongThuc, NguoiThu) VALUES
('TT001', 'HD001', '2026-09-07 10:35:00', 405000, 'TienMat', 'letan01'),
('TT002', 'HD002', '2026-09-16 14:05:00', 332500, 'ChuyenKhoan', 'letan01'),
('TT003', 'HD003', '2026-09-14 09:20:00', 675000, 'The', 'admin'),
('TT004', 'HD004', '2026-09-22 16:50:00', 412500, 'TienMat', 'letan01');

-- Password: 123456 (MD5)
INSERT IGNORE INTO NguoiDung (Username, PasswordHash, HoTen, VaiTro, MaLienKet) VALUES
('admin', 'e10adc3949ba59abbe56e057f20f883e', 'Nguyễn Quản Trị', 'Admin', NULL),
('letan01', 'e10adc3949ba59abbe56e057f20f883e', 'Trần Thị Lễ Tân', 'LeTan', NULL),
('bsan', 'e10adc3949ba59abbe56e057f20f883e', 'Nguyễn Văn An', 'BacSi', 'BS001'),
('bsbinh', 'e10adc3949ba59abbe56e057f20f883e', 'Trần Thị Bình', 'BacSi', 'BS002');

-- =====================================================
-- VIEW
-- =====================================================

CREATE OR REPLACE VIEW vw_HoaDonChuaThanhToan AS
SELECT 
    hd.MaHoaDon,
    hd.MaLanKham,
    bn.HoTen AS TenBenhNhan,
    bn.SDT,
    hd.NgayLap,
    hd.TienKham,
    hd.TienThuoc,
    hd.TongTien,
    DATEDIFF(CURDATE(), DATE(hd.NgayLap)) AS SoNgayNo
FROM HoaDon hd
JOIN LanKham lk ON hd.MaLanKham = lk.MaLanKham
JOIN BenhNhan bn ON lk.MaBN = bn.MaBN
WHERE hd.TrangThai = 'ChuaThanhToan'
ORDER BY hd.NgayLap;

SELECT '=== IMPORT THANH CONG ===' AS ThongBao;
SELECT 'Database: phongkham_db' AS Info;
SHOW TABLES;