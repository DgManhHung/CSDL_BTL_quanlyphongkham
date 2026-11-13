package com.phongkham.shared;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kết nối DB dùng chung cả nhóm – UTF-8 cố định để không lỗi tiếng Việt.
 * SỬA PASS cho đúng MySQL máy bạn.
 */
public class DB {
    private static final String URL =
            "jdbc:mysql://localhost:3306/phongkham_db"
                    + "?useSSL=false"
                    + "&serverTimezone=Asia/Ho_Chi_Minh"
                    + "&allowPublicKeyRetrieval=true"
                    + "&useUnicode=true"
                    + "&characterEncoding=UTF-8"
                    + "&connectionCollation=utf8mb4_unicode_ci";

    private static final String USER = "root";
    private static final String PASS = "12345678as";   // <-- SỬA PASSWORD MYSQL TẠI ĐÂY

    public static Connection conn() throws SQLException {
        Connection c = DriverManager.getConnection(URL, USER, PASS);
        // Ép charset UTF-8 cho mọi phiên kết nối
        try (Statement st = c.createStatement()) {
            st.execute("SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci");
            st.execute("SET CHARACTER SET utf8mb4");
        }
        return c;
    }

    public static void ensureSchema() throws SQLException {
        try (Connection c = conn(); Statement st = c.createStatement()) {
            ensureHireDateColumn(c, st, "BacSi");
            ensureHireDateColumn(c, st, "YTa");
            ensureTreatmentEpisodeSchema(c, st);
            ensureProject1Views(st);
        }
    }

    private static void ensureProject1Views(Statement st) throws SQLException {
        st.execute("""
                CREATE OR REPLACE VIEW V_DotDieuTri AS
                SELECT d.MaDotDieuTri, d.MaBN, bn.HoTen AS TenBN, d.MaBS, bs.HoTen AS TenBS,
                       d.MaBenh, dm.TenBenh, d.NgayBatDau, d.NgayKetThuc, d.TrangThai,
                       COUNT(lk.MaLanKham) AS SoLanKham
                FROM DotDieuTri d
                JOIN BenhNhan bn ON bn.MaBN=d.MaBN
                JOIN BacSi bs ON bs.MaBS=d.MaBS
                JOIN DanhMucBenh dm ON dm.MaBenh=d.MaBenh
                LEFT JOIN LanKham lk ON lk.MaDotDieuTri=d.MaDotDieuTri
                GROUP BY d.MaDotDieuTri, d.MaBN, bn.HoTen, d.MaBS, bs.HoTen,
                         d.MaBenh, dm.TenBenh, d.NgayBatDau, d.NgayKetThuc, d.TrangThai
                """);
        st.execute("""
                CREATE OR REPLACE VIEW V_HoSoBenhNhan AS
                SELECT lk.MaBN, bn.HoTen AS TenBN, lk.MaLanKham, lk.MaDotDieuTri,
                       lk.MaBenh, dm.TenBenh, lk.MaBS, bs.HoTen AS TenBS,
                       lk.NgayVao, lk.NgayRa, lk.TongTienKham, lk.TrangThai,
                       d.TrangThai AS TrangThaiDot,
                       (SELECT COUNT(*) + 1
                        FROM LanKham truoc
                        WHERE truoc.MaDotDieuTri=lk.MaDotDieuTri
                          AND (truoc.NgayVao < lk.NgayVao
                               OR (truoc.NgayVao=lk.NgayVao AND truoc.MaLanKham < lk.MaLanKham))
                       ) AS LanThuTrongDot,
                       (SELECT COUNT(*)
                        FROM LanKham cungBenh
                        WHERE cungBenh.MaBN=lk.MaBN AND cungBenh.MaBenh=lk.MaBenh
                       ) AS TongLanKhamBenh,
                       (SELECT GROUP_CONCAT(CONCAT(t.TenThuoc, ' x', ct.SoLuong)
                                            ORDER BY t.TenThuoc SEPARATOR ', ')
                        FROM DonThuoc dt
                        JOIN ChiTietDonThuoc ct ON ct.MaDonThuoc=dt.MaDonThuoc
                        JOIN Thuoc t ON t.MaThuoc=ct.MaThuoc
                        WHERE dt.MaLanKham=lk.MaLanKham
                       ) AS ThuocKe,
                       COALESCE((SELECT SUM(ct.ThanhTien)
                                 FROM DonThuoc dt
                                 JOIN ChiTietDonThuoc ct ON ct.MaDonThuoc=dt.MaDonThuoc
                                 WHERE dt.MaLanKham=lk.MaLanKham),0) AS TongTienThuoc,
                       (SELECT GROUP_CONCAT(CONCAT(y.HoTen, ': ', pc.LoaiHoTro)
                                            ORDER BY y.HoTen SEPARATOR '; ')
                        FROM PhanCongYT pc
                        JOIN YTa y ON y.MaNV=pc.MaNV
                        WHERE pc.MaLanKham=lk.MaLanKham
                       ) AS HoTroYTa
                FROM LanKham lk
                JOIN BenhNhan bn ON bn.MaBN=lk.MaBN
                JOIN DanhMucBenh dm ON dm.MaBenh=lk.MaBenh
                JOIN BacSi bs ON bs.MaBS=lk.MaBS
                JOIN DotDieuTri d ON d.MaDotDieuTri=lk.MaDotDieuTri
                """);
        st.execute("""
                CREATE OR REPLACE VIEW V_LuotHoTroYTa AS
                SELECT pc.MaPC, pc.MaNV, y.HoTen AS TenYT, pc.MaLanKham, lk.MaBN,
                       pc.NgayPhanCong, pc.LoaiHoTro
                FROM PhanCongYT pc
                JOIN YTa y ON y.MaNV=pc.MaNV
                JOIN LanKham lk ON lk.MaLanKham=pc.MaLanKham
                """);
        st.execute("""
                CREATE OR REPLACE VIEW V_DoanhThuHoaDon AS
                SELECT hd.MaHoaDon, hd.MaLanKham, lk.MaBN, bn.HoTen AS TenBN,
                       lk.MaBS, bs.HoTen AS TenBS, hd.NgayLap, hd.TienKham, hd.TienThuoc,
                       hd.TongTien, hd.TrangThai
                FROM HoaDon hd
                JOIN LanKham lk ON lk.MaLanKham=hd.MaLanKham
                JOIN BenhNhan bn ON bn.MaBN=lk.MaBN
                JOIN BacSi bs ON bs.MaBS=lk.MaBS
                WHERE hd.TrangThai NOT IN ('Huy', 'Đã Hủy')
                """);
    }

    private static void ensureTreatmentEpisodeSchema(Connection c, Statement st) throws SQLException {
        st.execute("""
                CREATE TABLE IF NOT EXISTS DotDieuTri (
                    MaDotDieuTri VARCHAR(20) PRIMARY KEY,
                    MaBN VARCHAR(10) NOT NULL,
                    MaBS VARCHAR(5) NOT NULL,
                    MaBenh VARCHAR(10) NOT NULL,
                    NgayBatDau DATE NOT NULL,
                    NgayKetThuc DATE,
                    TrangThai VARCHAR(20) NOT NULL DEFAULT 'DangDieuTri',
                    FOREIGN KEY (MaBN) REFERENCES BenhNhan(MaBN),
                    FOREIGN KEY (MaBS) REFERENCES BacSi(MaBS),
                    FOREIGN KEY (MaBenh) REFERENCES DanhMucBenh(MaBenh)
                )
                """);
        if (!hasColumn(c, "LanKham", "MaDotDieuTri")) {
            st.execute("ALTER TABLE LanKham ADD COLUMN MaDotDieuTri VARCHAR(20) NULL");
        }

        st.execute("""
                INSERT IGNORE INTO DotDieuTri
                    (MaDotDieuTri, MaBN, MaBS, MaBenh, NgayBatDau, NgayKetThuc, TrangThai)
                SELECT CONCAT('DT', lk.MaLanKham), lk.MaBN, lk.MaBS, lk.MaBenh, lk.NgayVao,
                       CASE WHEN lk.TrangThai='DaKhoi' THEN COALESCE(lk.NgayRa, lk.NgayVao) ELSE NULL END,
                       lk.TrangThai
                FROM LanKham lk
                WHERE lk.MaDotDieuTri IS NULL
                """);
        st.executeUpdate("""
                UPDATE LanKham
                SET MaDotDieuTri=CONCAT('DT', MaLanKham)
                WHERE MaLanKham > '' AND MaDotDieuTri IS NULL
                """);
        st.execute("ALTER TABLE LanKham MODIFY MaDotDieuTri VARCHAR(20) NOT NULL");
        if (!hasForeignKey(c, "LanKham", "fk_LanKham_DotDieuTri")) {
            st.execute("""
                    ALTER TABLE LanKham
                    ADD CONSTRAINT fk_LanKham_DotDieuTri
                    FOREIGN KEY (MaDotDieuTri) REFERENCES DotDieuTri(MaDotDieuTri)
                    """);
        }
    }

    private static void ensureHireDateColumn(Connection c, Statement st, String table) throws SQLException {
        boolean hasHireDate = hasColumn(c, table, "NgayVaoLam");
        boolean nullable = !hasHireDate || isNullable(c, table, "NgayVaoLam");
        if (!hasHireDate) {
            st.execute("ALTER TABLE " + table + " ADD COLUMN NgayVaoLam DATE NULL");
        }

        if (hasColumn(c, table, "ThamNien")) {
            st.executeUpdate("UPDATE " + table + " SET NgayVaoLam = CASE " +
                    "WHEN COALESCE(ThamNien,0) <= 0 THEN CURDATE() " +
                    "ELSE DATE_SUB(CURDATE(), INTERVAL ThamNien YEAR) END " +
                    "WHERE NgayVaoLam IS NULL");
        } else {
            st.executeUpdate("UPDATE " + table + " SET NgayVaoLam = CURDATE() WHERE NgayVaoLam IS NULL");
        }
        if (nullable) st.execute("ALTER TABLE " + table + " MODIFY COLUMN NgayVaoLam DATE NOT NULL");
    }

    private static boolean hasColumn(Connection c, String table, String column) throws SQLException {
        try (var columns = c.getMetaData().getColumns(c.getCatalog(), null, table, column)) {
            return columns.next();
        }
    }

    private static boolean isNullable(Connection c, String table, String column) throws SQLException {
        try (var columns = c.getMetaData().getColumns(c.getCatalog(), null, table, column)) {
            return columns.next() && "YES".equalsIgnoreCase(columns.getString("IS_NULLABLE"));
        }
    }

    private static boolean hasForeignKey(Connection c, String table, String foreignKey) throws SQLException {
        try (var keys = c.getMetaData().getImportedKeys(c.getCatalog(), null, table)) {
            while (keys.next()) {
                if (foreignKey.equalsIgnoreCase(keys.getString("FK_NAME"))) return true;
            }
        }
        return false;
    }

    public static List<Map<String, Object>> query(String sql, Object... params) {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ResultSet rs = ps.executeQuery();
            ResultSetMetaData md = rs.getMetaData();
            int n = md.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= n; i++) row.put(md.getColumnLabel(i), rs.getObject(i));
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Lỗi SQL: " + e.getMessage(), e);
        }
        return list;
    }

    public static boolean exec(String sql, Object... params) {
        try (Connection c = conn(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean transaction(TransactionWork work) {
        try (Connection c = conn()) {
            c.setAutoCommit(false);
            try {
                if (!work.execute(c)) {
                    c.rollback();
                    return false;
                }
                c.commit();
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int exec(Connection c, String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            return ps.executeUpdate();
        }
    }

    @FunctionalInterface
    public interface TransactionWork {
        boolean execute(Connection c) throws SQLException;
    }

    /** Đăng nhập: so khớp username + mật khẩu plain text */
    public static boolean login(String user, String pass) {
        List<Map<String, Object>> rows = query(
            "SELECT Username FROM NguoiDung WHERE Username=? AND MatKhau=? AND TrangThai='Active'",
            user, pass);
        return !rows.isEmpty();
    }
}