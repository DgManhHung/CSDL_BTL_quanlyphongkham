package com.phongkham.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Kết nối MySQL – sửa USER / PASS cho đúng máy bạn.
 */
public class DBConnection {
    private static final String URL  = "jdbc:mysql://localhost:3306/phongkham_db?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&characterEncoding=UTF-8&allowPublicKeyRetrieval=true";
    private static final String USER = "root";
    private static final String PASS = "12345678as";   // <-- điền password MySQL của bạn

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}