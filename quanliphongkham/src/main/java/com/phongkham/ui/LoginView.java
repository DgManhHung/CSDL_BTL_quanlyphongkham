package com.phongkham.ui;

import com.phongkham.dao.DBConnection;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.security.MessageDigest;

public class LoginView {
    private final BorderPane root = new BorderPane();
    private final Stage stage;

    public LoginView(Stage stage) {
        this.stage = stage;
        build();
    }

    public BorderPane getRoot() { return root; }

    private void build() {
        root.setStyle("-fx-background-color: #f0f2f5;");

        VBox card = new VBox(16);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 16, 0, 0, 4);");

        Label title = new Label("PHÒNG KHÁM TƯ NHÂN");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#1a365d"));

        Label sub = new Label("Đăng nhập hệ thống");
        sub.setTextFill(Color.GRAY);

        TextField userField = new TextField();
        userField.setPromptText("Tên đăng nhập");
        userField.setPrefHeight(40);
        userField.setMaxWidth(300);

        PasswordField passField = new PasswordField();
        passField.setPromptText("Mật khẩu");
        passField.setPrefHeight(40);
        passField.setMaxWidth(300);

        Label error = new Label();
        error.setTextFill(Color.web("#e53e3e"));

        Button btn = new Button("Đăng nhập");
        btn.setPrefHeight(42);
        btn.setPrefWidth(300);
        btn.setStyle("-fx-background-color: #0d9488; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            String u = userField.getText().trim();
            String p = passField.getText();
            if (u.isEmpty() || p.isEmpty()) {
                error.setText("Vui lòng nhập đầy đủ!");
                return;
            }
            try {
                if (checkLogin(u, p)) {
                    stage.setScene(new javafx.scene.Scene(new MainShell(stage, u).getRoot(), 1200, 750));
                } else {
                    error.setText("Sai tên đăng nhập hoặc mật khẩu!");
                }
            } catch (Exception ex) {
                error.setText("Lỗi kết nối DB: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        Label hint = new Label("Demo: admin / 123456  |  letan01 / 123456");
        hint.setTextFill(Color.GRAY);
        hint.setFont(Font.font(11));

        card.getChildren().addAll(title, sub, userField, passField, error, btn, hint);

        StackPane center = new StackPane(card);
        root.setCenter(center);
    }

    private boolean checkLogin(String user, String pass) throws Exception {
        String hash = md5(pass);
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                 "SELECT Username FROM NguoiDung WHERE Username=? AND PasswordHash=? AND TrangThai='Active'")) {
            ps.setString(1, user);
            ps.setString(2, hash);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }

    private static String md5(String s) throws Exception {
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] dig = md.digest(s.getBytes());
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}