-- =====================================================
-- PHÒNG KHÁM TƯ NHÂN - NHÓM 5
-- DB chung cho cả 5 thành viên
-- =====================================================
DROP DATABASE IF EXISTS phongkham_db;
CREATE DATABASE phongkham_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE phongkham_db;

-- ========== TV1: BÁC SĨ ==========
CREATE TABLE BacSi (
    MaBS VARCHAR(10) PRIMARY KEY,
    CMT VARCHAR(12) UNIQUE NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    NgaySinh DATE,
    DiaChi VARCHAR(255),
    BacNghe VARCHAR(50),
    ThamNien INT DEFAULT 0,
    TrinhDo VARCHAR(50),
    ChuyenMon VARCHAR(100)
);

-- ========== TV2: Y TÁ + PHÂN CÔNG ==========
CREATE TABLE YTa (
    MaNV VARCHAR(10) PRIMARY KEY,
    CMT VARCHAR(12) UNIQUE NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    TrinhDo VARCHAR(50),
    ThamNien INT DEFAULT 0,
    NgaySinh DATE,
    DiaChi VARCHAR(255),
    SDT VARCHAR(15)
);

-- ========== TV3: BỆNH NHÂN + LẦN KHÁM ==========
CREATE TABLE BenhNhan (
    MaBN VARCHAR(10) PRIMARY KEY,
    CMT VARCHAR(12) UNIQUE NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    NgaySinh DATE,
    DiaChi VARCHAR(255),
    SDT VARCHAR(15)
);

CREATE TABLE DanhMucBenh (
    MaBenh VARCHAR(10) PRIMARY KEY,
    TenBenh VARCHAR(100) NOT NULL,
    MoTa TEXT
);

CREATE TABLE LanKham (
    MaLanKham VARCHAR(15) PRIMARY KEY,
    MaBN VARCHAR(10) NOT NULL,
    MaBS VARCHAR(10) NOT NULL,
    MaBenh VARCHAR(10) NOT NULL,
    NgayVao DATE NOT NULL,
    NgayRa DATE,
    TongTienKham DECIMAL(12,0) DEFAULT 0,
    TrangThai VARCHAR(20) DEFAULT 'DangDieuTri',
    GhiChu TEXT,
    FOREIGN KEY (MaBN) REFERENCES BenhNhan(MaBN),
    FOREIGN KEY (MaBS) REFERENCES BacSi(MaBS),
    FOREIGN KEY (MaBenh) REFERENCES DanhMucBenh(MaBenh)
);

CREATE TABLE PhanCongYT (
    MaPC INT AUTO_INCREMENT PRIMARY KEY,
    MaLanKham VARCHAR(15) NOT NULL,
    MaNV VARCHAR(10) NOT NULL,
    NgayPhanCong DATE NOT NULL,
    LoaiHoTro VARCHAR(50) DEFAULT 'ChamSoc',
    FOREIGN KEY (MaLanKham) REFERENCES LanKham(MaLanKham),
    FOREIGN KEY (MaNV) REFERENCES YTa(MaNV)
);

-- ========== TV4: THUỐC + ĐƠN THUỐC ==========
CREATE TABLE Thuoc (
    MaThuoc VARCHAR(10) PRIMARY KEY,
    TenThuoc VARCHAR(100) NOT NULL,
    DonVi VARCHAR(20) DEFAULT 'Viên',
    GiaBan DECIMAL(12,0) NOT NULL,
    SoLuongTon INT DEFAULT 0
);

CREATE TABLE DonThuoc (
    MaDonThuoc VARCHAR(15) PRIMARY KEY,
    MaLanKham VARCHAR(15) NOT NULL,
    NgayKeDon DATE NOT NULL,
    FOREIGN KEY (MaLanKham) REFERENCES LanKham(MaLanKham)
);

CREATE TABLE ChiTietDonThuoc (
    MaDonThuoc VARCHAR(15),
    MaThuoc VARCHAR(10),
    SoLuong INT NOT NULL,
    DonGia DECIMAL(12,0) NOT NULL,
    ThanhTien DECIMAL(12,0) NOT NULL,
    PRIMARY KEY (MaDonThuoc, MaThuoc),
    FOREIGN KEY (MaDonThuoc) REFERENCES DonThuoc(MaDonThuoc),
    FOREIGN KEY (MaThuoc) REFERENCES Thuoc(MaThuoc)
);

-- ========== TV5: HÓA ĐƠN + THANH TOÁN + USER ==========
CREATE TABLE HoaDon (
    MaHoaDon VARCHAR(15) PRIMARY KEY,
    MaLanKham VARCHAR(15) NOT NULL,
    NgayLap DATETIME DEFAULT CURRENT_TIMESTAMP,
    TienKham DECIMAL(12,0) DEFAULT 0,
    TienThuoc DECIMAL(12,0) DEFAULT 0,
    TongTien DECIMAL(12,0) DEFAULT 0,
    TrangThai VARCHAR(20) DEFAULT 'ChuaThanhToan',
    NguoiLap VARCHAR(50),
    GhiChu TEXT,
    FOREIGN KEY (MaLanKham) REFERENCES LanKham(MaLanKham)
);

CREATE TABLE ThanhToan (
    MaThanhToan VARCHAR(15) PRIMARY KEY,
    MaHoaDon VARCHAR(15) NOT NULL,
    NgayThanhToan DATETIME DEFAULT CURRENT_TIMESTAMP,
    SoTien DECIMAL(12,0) NOT NULL,
    PhuongThuc VARCHAR(20) DEFAULT 'TienMat',
    NguoiThu VARCHAR(50),
    GhiChu TEXT,
    FOREIGN KEY (MaHoaDon) REFERENCES HoaDon(MaHoaDon)
);

CREATE TABLE NguoiDung (
    Username VARCHAR(50) PRIMARY KEY,
    MatKhau VARCHAR(50) NOT NULL,
    HoTen VARCHAR(100) NOT NULL,
    VaiTro VARCHAR(20) NOT NULL,
    TrangThai VARCHAR(20) DEFAULT 'Active'
);

-- ===== DATA MẪU =====
INSERT INTO BacSi VALUES
('BS001','001234567890','Nguyễn Văn An','1980-05-15','Hà Nội','Bác sĩ chính',15,'Thạc sĩ','Nội khoa'),
('BS002','001234567891','Trần Thị Bình','1985-08-20','Hà Nội','Bác sĩ',10,'Đại học','Nhi khoa'),
('BS003','001234567892','Lê Minh Cường','1978-03-12','Hà Nội','Bác sĩ chính',18,'Tiến sĩ','Ngoại khoa');

INSERT INTO YTa VALUES
('YT01','002234567890','Nguyễn Thị Mai','Cử nhân',5,'1995-05-12','Hà Nội','0912345678'),
('YT02','002234567891','Trần Thị Hoa','Cao đẳng',3,'1997-09-24','Hà Nội','0987654321'),
('YT03','002234567892','Lê Văn Nam','Trung cấp',2,'1999-01-08','Hà Nội','0901234567');

INSERT INTO BenhNhan VALUES
('BN001','001111111111','Hoàng Văn Em','1992-01-15','Cầu Giấy, Hà Nội','0901111111'),
('BN002','001111111112','Ngô Thị Hoa','1988-11-30','Đống Đa, Hà Nội','0902222222'),
('BN003','001111111113','Vũ Minh Khoa','2000-06-05','Hai Bà Trưng, Hà Nội','0903333333'),
('BN004','001111111114','Phạm Thị Lan','1995-03-20','Thanh Xuân, Hà Nội','0904444444'),
('BN005','001111111115','Đỗ Văn Hùng','1985-07-18','Ba Đình, Hà Nội','0905555555');

INSERT INTO DanhMucBenh VALUES
('B001','Cảm cúm','Cảm lạnh thông thường'),
('B002','Viêm họng','Viêm họng cấp'),
('B003','Đau dạ dày','Viêm loét dạ dày'),
('B004','Cao huyết áp','Tăng huyết áp'),
('B005','Viêm phổi','Viêm phổi nhẹ');

INSERT INTO LanKham VALUES
('LK001','BN001','BS001','B001','2026-09-05','2026-09-07',350000,'DaKhoi',NULL),
('LK002','BN001','BS001','B002','2026-09-15','2026-09-16',280000,'DaKhoi',NULL),
('LK003','BN002','BS002','B003','2026-09-10','2026-09-14',520000,'DaKhoi',NULL),
('LK004','BN003','BS001','B001','2026-09-20','2026-09-22',350000,'DaKhoi',NULL),
('LK005','BN002','BS002','B004','2026-10-01',NULL,450000,'DangDieuTri',NULL),
('LK006','BN001','BS001','B003','2026-10-03','2026-10-05',480000,'DaKhoi',NULL),
('LK007','BN004','BS003','B005','2026-10-08','2026-10-12',650000,'DaKhoi',NULL),
('LK008','BN005','BS001','B002','2026-10-10',NULL,300000,'DangDieuTri',NULL);

INSERT INTO PhanCongYT (MaLanKham,MaNV,NgayPhanCong,LoaiHoTro) VALUES
('LK001','YT01','2026-09-05','TiepDon'),
('LK001','YT01','2026-09-06','ChamSoc'),
('LK002','YT02','2026-09-15','CapThuoc'),
('LK003','YT01','2026-09-10','ChamSoc'),
('LK003','YT03','2026-09-12','TiemThuoc'),
('LK004','YT02','2026-09-20','TiepDon'),
('LK006','YT01','2026-10-03','ChamSoc'),
('LK007','YT02','2026-10-08','ChamSoc'),
('LK007','YT03','2026-10-10','TiemThuoc');

INSERT INTO Thuoc VALUES
('T001','Paracetamol 500mg','Viên',2000,5000),
('T002','Amoxicillin 500mg','Viên',3500,3000),
('T003','Omeprazole 20mg','Viên',4500,2000),
('T004','Vitamin C 1000mg','Viên',1500,4000),
('T005','Losartan 50mg','Viên',6000,1500),
('T006','Azithromycin 500mg','Viên',8000,1000);

INSERT INTO DonThuoc VALUES
('DT001','LK001','2026-09-05'),
('DT002','LK002','2026-09-15'),
('DT003','LK003','2026-09-10'),
('DT004','LK004','2026-09-20'),
('DT005','LK006','2026-10-03'),
('DT006','LK007','2026-10-08');

INSERT INTO ChiTietDonThuoc VALUES
('DT001','T001',20,2000,40000),('DT001','T004',10,1500,15000),
('DT002','T002',15,3500,52500),
('DT003','T003',30,4500,135000),('DT003','T001',10,2000,20000),
('DT004','T001',20,2000,40000),('DT004','T004',15,1500,22500),
('DT005','T003',28,4500,126000),
('DT006','T002',20,3500,70000),('DT006','T006',10,8000,80000);

INSERT INTO HoaDon VALUES
('HD001','LK001','2026-09-07 10:30:00',350000,55000,405000,'DaThanhToan','letan01',NULL),
('HD002','LK002','2026-09-16 14:00:00',280000,52500,332500,'DaThanhToan','letan01',NULL),
('HD003','LK003','2026-09-14 09:15:00',520000,155000,675000,'DaThanhToan','admin',NULL),
('HD004','LK004','2026-09-22 16:45:00',350000,62500,412500,'DaThanhToan','letan01',NULL),
('HD005','LK006','2026-10-05 11:20:00',480000,126000,606000,'ChuaThanhToan','letan01',NULL),
('HD006','LK007','2026-10-12 15:00:00',650000,150000,800000,'DaThanhToan','admin',NULL);

INSERT INTO ThanhToan VALUES
('TT001','HD001','2026-09-07 10:35:00',405000,'TienMat','letan01',NULL),
('TT002','HD002','2026-09-16 14:05:00',332500,'ChuyenKhoan','letan01',NULL),
('TT003','HD003','2026-09-14 09:20:00',675000,'The','admin',NULL),
('TT004','HD004','2026-09-22 16:50:00',412500,'TienMat','letan01',NULL),
('TT005','HD006','2026-10-12 15:10:00',800000,'ChuyenKhoan','admin',NULL);

-- Mật khẩu LƯU PLAIN TEXT cho dễ demo (123456)
INSERT INTO NguoiDung VALUES
('admin','123456','Nguyễn Quản Trị','Admin','Active'),
('letan01','123456','Trần Thị Lễ Tân','LeTan','Active'),
('bsan','123456','Nguyễn Văn An','BacSi','Active');

SELECT '=== IMPORT OK ===' AS msg;
SELECT Username, MatKhau, VaiTro FROM NguoiDung;
SHOW TABLES;