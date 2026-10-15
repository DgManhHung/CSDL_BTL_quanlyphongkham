package com.phongkham.shared;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Kết nối DB dùng chung cả nhóm.
 * SỬA PASS cho đúng MySQL máy bạn.
 */
public class DB {
    private static final String URL  = "jdbc:mysql://localhost:3306/phongkham_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8";
    private static final String USER = "root";
    private static final String PASS = "12345678as";   // <-- SỬA PASSWORD MYSQL TẠI ĐÂY

    public static Connection conn() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
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