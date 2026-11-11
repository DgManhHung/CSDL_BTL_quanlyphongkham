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

    public static void ensureSenioritySchema() throws SQLException {
        try (Connection c = conn(); Statement st = c.createStatement()) {
            ensureHireDateColumn(c, st, "BacSi");
            ensureHireDateColumn(c, st, "YTa");
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

    /** Đăng nhập: so khớp username + mật khẩu plain text */
    public static boolean login(String user, String pass) {
        List<Map<String, Object>> rows = query(
            "SELECT Username FROM NguoiDung WHERE Username=? AND MatKhau=? AND TrangThai='Active'",
            user, pass);
        return !rows.isEmpty();
    }
}