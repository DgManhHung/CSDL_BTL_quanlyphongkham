package com.phongkham.dao;

import com.phongkham.model.HoaDon;

import java.math.BigDecimal;
import java.sql.*;
import java.util.*;

public class HoaDonDAO {

    public List<HoaDon> getAll() {
        List<HoaDon> list = new ArrayList<>();
        String sql = """
            SELECT hd.*, bn.HoTen AS TenBN, bn.SDT
            FROM HoaDon hd
            JOIN LanKham lk ON hd.MaLanKham = lk.MaLanKham
            JOIN BenhNhan bn ON lk.MaBN = bn.MaBN
            ORDER BY hd.NgayLap DESC
            """;
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<HoaDon> getByTrangThai(String tt) {
        List<HoaDon> list = new ArrayList<>();
        String sql = """
            SELECT hd.*, bn.HoTen AS TenBN, bn.SDT
            FROM HoaDon hd
            JOIN LanKham lk ON hd.MaLanKham = lk.MaLanKham
            JOIN BenhNhan bn ON lk.MaBN = bn.MaBN
            WHERE hd.TrangThai = ?
            ORDER BY hd.NgayLap DESC
            """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tt);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<HoaDon> getDoanhThuTheoThang(int thang, int nam) {
        List<HoaDon> list = new ArrayList<>();
        String sql = """
            SELECT hd.*, bn.HoTen AS TenBN, bn.SDT
            FROM HoaDon hd
            JOIN LanKham lk ON hd.MaLanKham = lk.MaLanKham
            JOIN BenhNhan bn ON lk.MaBN = bn.MaBN
            WHERE MONTH(hd.NgayLap)=? AND YEAR(hd.NgayLap)=? AND hd.TrangThai!='Huy'
            ORDER BY hd.NgayLap DESC
            """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, thang);
            ps.setInt(2, nam);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean xoaHoaDon(String maHD) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM ThanhToan WHERE MaHoaDon=?")) {
                ps.setString(1, maHD);
                ps.executeUpdate();
            }
            int deleted;
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM HoaDon WHERE MaHoaDon=?")) {
                ps.setString(1, maHD);
                deleted = ps.executeUpdate();
            }
            if (deleted != 1) {
                c.rollback();
                return false;
            }
            c.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Map<String, Object> doanhThuThang(int thang, int nam) {
        Map<String, Object> m = new HashMap<>();
        String sql = """
            SELECT COALESCE(SUM(TienKham),0) AS TongKham,
                   COALESCE(SUM(TienThuoc),0) AS TongThuoc,
                   COALESCE(SUM(TongTien),0) AS TongDT,
                   COUNT(*) AS SoHD,
                   SUM(CASE WHEN TrangThai='ChuaThanhToan' THEN TongTien ELSE 0 END) AS ChuaThu
            FROM HoaDon
            WHERE MONTH(NgayLap)=? AND YEAR(NgayLap)=? AND TrangThai!='Huy'
            """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, thang);
            ps.setInt(2, nam);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                m.put("TongKham", rs.getBigDecimal("TongKham"));
                m.put("TongThuoc", rs.getBigDecimal("TongThuoc"));
                m.put("TongDT", rs.getBigDecimal("TongDT"));
                m.put("SoHD", rs.getInt("SoHD"));
                m.put("ChuaThu", rs.getBigDecimal("ChuaThu"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return m;
    }

    public List<Map<String, Object>> doanhThuTheoBacSi(int thang, int nam) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT bs.MaBS, bs.HoTen,
                   COUNT(DISTINCT hd.MaHoaDon) AS SoHD,
                   SUM(hd.TongTien) AS TongDT
            FROM HoaDon hd
            JOIN LanKham lk ON hd.MaLanKham = lk.MaLanKham
            JOIN BacSi bs ON lk.MaBS = bs.MaBS
            WHERE MONTH(hd.NgayLap)=? AND YEAR(hd.NgayLap)=? AND hd.TrangThai!='Huy'
            GROUP BY bs.MaBS, bs.HoTen
            ORDER BY TongDT DESC
            """;
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, thang);
            ps.setInt(2, nam);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("MaBS", rs.getString("MaBS"));
                m.put("HoTen", rs.getString("HoTen"));
                m.put("SoHD", rs.getInt("SoHD"));
                m.put("TongDT", rs.getBigDecimal("TongDT"));
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public BigDecimal daThanhToan(String maHD) {
        String sql = "SELECT COALESCE(SUM(SoTien),0) FROM ThanhToan WHERE MaHoaDon=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, maHD);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getBigDecimal(1);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return BigDecimal.ZERO;
    }

    public boolean thanhToan(String maHD, BigDecimal soTien, String phuongThuc, String nguoiThu) {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            // sinh mã TT
            String maTT = "TT001";
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT MaThanhToan FROM ThanhToan WHERE MaThanhToan LIKE 'TT%' ORDER BY MaThanhToan DESC LIMIT 1")) {
                if (rs.next()) {
                    int n = Integer.parseInt(rs.getString(1).substring(2)) + 1;
                    maTT = String.format("TT%03d", n);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO ThanhToan (MaThanhToan, MaHoaDon, SoTien, PhuongThuc, NguoiThu) VALUES (?,?,?,?,?)")) {
                ps.setString(1, maTT);
                ps.setString(2, maHD);
                ps.setBigDecimal(3, soTien);
                ps.setString(4, phuongThuc);
                ps.setString(5, nguoiThu);
                ps.executeUpdate();
            }
            // cập nhật trạng thái nếu đủ
            BigDecimal daTT = daThanhToan(maHD);
            BigDecimal tong;
            try (PreparedStatement ps = c.prepareStatement("SELECT TongTien FROM HoaDon WHERE MaHoaDon=?")) {
                ps.setString(1, maHD);
                ResultSet rs = ps.executeQuery();
                rs.next();
                tong = rs.getBigDecimal(1);
            }
            if (daTT.add(soTien).compareTo(tong) >= 0) {
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE HoaDon SET TrangThai='DaThanhToan' WHERE MaHoaDon=?")) {
                    ps.setString(1, maHD);
                    ps.executeUpdate();
                }
            }
            c.commit();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> getLanKhamChuaHD() {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
            SELECT lk.MaLanKham, bn.HoTen, lk.NgayVao, lk.TongTienKham, dm.TenBenh
            FROM LanKham lk
            JOIN BenhNhan bn ON lk.MaBN = bn.MaBN
            JOIN DanhMucBenh dm ON lk.MaBenh = dm.MaBenh
            WHERE lk.MaLanKham NOT IN (SELECT MaLanKham FROM HoaDon WHERE TrangThai!='Huy')
            ORDER BY lk.NgayVao DESC
            """;
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> m = new HashMap<>();
                m.put("MaLanKham", rs.getString("MaLanKham"));
                m.put("HoTen", rs.getString("HoTen"));
                m.put("NgayVao", rs.getDate("NgayVao"));
                m.put("TongTienKham", rs.getBigDecimal("TongTienKham"));
                m.put("TenBenh", rs.getString("TenBenh"));
                list.add(m);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean taoHoaDon(String maLK, String nguoiLap) {
        try (Connection c = DBConnection.getConnection()) {
            // tiền khám
            BigDecimal tienKham = BigDecimal.ZERO;
            try (PreparedStatement ps = c.prepareStatement("SELECT TongTienKham FROM LanKham WHERE MaLanKham=?")) {
                ps.setString(1, maLK);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) tienKham = rs.getBigDecimal(1);
            }
            // tiền thuốc
            BigDecimal tienThuoc = BigDecimal.ZERO;
            try (PreparedStatement ps = c.prepareStatement("""
                    SELECT COALESCE(SUM(ct.ThanhTien),0) FROM ChiTietDonThuoc ct
                    JOIN DonThuoc dt ON ct.MaDonThuoc=dt.MaDonThuoc WHERE dt.MaLanKham=?
                    """)) {
                ps.setString(1, maLK);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) tienThuoc = rs.getBigDecimal(1);
            }
            String maHD = "HD001";
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery(
                     "SELECT MaHoaDon FROM HoaDon WHERE MaHoaDon LIKE 'HD%' ORDER BY MaHoaDon DESC LIMIT 1")) {
                if (rs.next()) {
                    int n = Integer.parseInt(rs.getString(1).substring(2)) + 1;
                    maHD = String.format("HD%03d", n);
                }
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO HoaDon (MaHoaDon, MaLanKham, TienKham, TienThuoc, TongTien, TrangThai, NguoiLap) VALUES (?,?,?,?,?,'ChuaThanhToan',?)")) {
                ps.setString(1, maHD);
                ps.setString(2, maLK);
                ps.setBigDecimal(3, tienKham);
                ps.setBigDecimal(4, tienThuoc);
                ps.setBigDecimal(5, tienKham.add(tienThuoc));
                ps.setString(6, nguoiLap);
                ps.executeUpdate();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    private HoaDon map(ResultSet rs) throws SQLException {
        HoaDon h = new HoaDon();
        h.setMaHoaDon(rs.getString("MaHoaDon"));
        h.setMaLanKham(rs.getString("MaLanKham"));
        Timestamp ts = rs.getTimestamp("NgayLap");
        if (ts != null) h.setNgayLap(ts.toLocalDateTime());
        h.setTienKham(rs.getBigDecimal("TienKham"));
        h.setTienThuoc(rs.getBigDecimal("TienThuoc"));
        h.setTongTien(rs.getBigDecimal("TongTien"));
        h.setTrangThai(rs.getString("TrangThai"));
        h.setNguoiLap(rs.getString("NguoiLap"));
        try { h.setTenBenhNhan(rs.getString("TenBN")); } catch (SQLException ignored) {}
        try { h.setSdt(rs.getString("SDT")); } catch (SQLException ignored) {}
        return h;
    }
}