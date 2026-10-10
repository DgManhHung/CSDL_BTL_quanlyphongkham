package com.phongkham.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HoaDon {
    private String maHoaDon;
    private String maLanKham;
    private LocalDateTime ngayLap;
    private BigDecimal tienKham;
    private BigDecimal tienThuoc;
    private BigDecimal tongTien;
    private String trangThai;
    private String nguoiLap;
    private String ghiChu;
    // join
    private String tenBenhNhan;
    private String sdt;

    public String getMaHoaDon() { return maHoaDon; }
    public void setMaHoaDon(String maHoaDon) { this.maHoaDon = maHoaDon; }

    public String getMaLanKham() { return maLanKham; }
    public void setMaLanKham(String maLanKham) { this.maLanKham = maLanKham; }

    public LocalDateTime getNgayLap() { return ngayLap; }
    public void setNgayLap(LocalDateTime ngayLap) { this.ngayLap = ngayLap; }

    public BigDecimal getTienKham() { return tienKham; }
    public void setTienKham(BigDecimal tienKham) { this.tienKham = tienKham; }

    public BigDecimal getTienThuoc() { return tienThuoc; }
    public void setTienThuoc(BigDecimal tienThuoc) { this.tienThuoc = tienThuoc; }

    public BigDecimal getTongTien() { return tongTien; }
    public void setTongTien(BigDecimal tongTien) { this.tongTien = tongTien; }

    public String getTrangThai() { return trangThai; }
    public void setTrangThai(String trangThai) { this.trangThai = trangThai; }

    public String getNguoiLap() { return nguoiLap; }
    public void setNguoiLap(String nguoiLap) { this.nguoiLap = nguoiLap; }

    public String getGhiChu() { return ghiChu; }
    public void setGhiChu(String ghiChu) { this.ghiChu = ghiChu; }

    public String getTenBenhNhan() { return tenBenhNhan; }
    public void setTenBenhNhan(String tenBenhNhan) { this.tenBenhNhan = tenBenhNhan; }

    public String getSdt() { return sdt; }
    public void setSdt(String sdt) { this.sdt = sdt; }
}