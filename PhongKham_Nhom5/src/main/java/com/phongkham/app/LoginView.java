package com.phongkham.app;

import com.phongkham.shared.DB;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

public class LoginView {
    private final BorderPane root = new BorderPane();
    private final Stage stage;

    public LoginView(Stage stage) {
        this.stage = stage;
        build();
    }

    public BorderPane getRoot() { return root; }

    private void build() {
        root.setStyle("-fx-background-color:#f0f2f5;");
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(400);
        card.setStyle("-fx-background-color:white;-fx-background-radius:12;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.12),16,0,0,4);");

        Label title = new Label("PHÒNG KHÁM TƯ NHÂN");
        title.setFont(Font.font("System", FontWeight.BOLD, 20));
        title.setTextFill(Color.web("#1a365d"));
        Label sub = new Label("Nhóm 5 – Đăng nhập hệ thống");
        sub.setTextFill(Color.GRAY);

        TextField user = new TextField();
        user.setPromptText("Tên đăng nhập");
        user.setPrefHeight(40);
        user.setMaxWidth(300);
        PasswordField pass = new PasswordField();
        pass.setPromptText("Mật khẩu");
        pass.setPrefHeight(40);
        pass.setMaxWidth(300);

        Label err = new Label();
        err.setTextFill(Color.web("#e53e3e"));
        err.setWrapText(true);
        err.setMaxWidth(300);

        Button btn = new Button("Đăng nhập");
        btn.setPrefHeight(42);
        btn.setPrefWidth(300);
        btn.setStyle("-fx-background-color:#0d9488;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-cursor:hand;");
        btn.setDefaultButton(true);
        btn.setOnAction(e -> {
            String u = user.getText().trim();
            String p = pass.getText();
            if (u.isEmpty() || p.isEmpty()) {
                err.setText("Vui lòng nhập đầy đủ!");
                return;
            }
            try {
                if (DB.login(u, p)) {
                    stage.setScene(new Scene(new MainShell(stage, u).getRoot(), 1280, 780));
                } else {
                    err.setText("Sai tên đăng nhập hoặc mật khẩu!\nThử: admin / 123456");
                }
            } catch (Exception ex) {
                err.setText("Lỗi kết nối DB:\n" + ex.getMessage()
                        + "\n\nSửa password trong:\nshared/DB.java");
                ex.printStackTrace();
            }
        });

        Label hint = new Label("admin / 123456   |   letan01 / 123456");
        hint.setTextFill(Color.GRAY);
        hint.setFont(Font.font(11));

        card.getChildren().addAll(title, sub, user, pass, err, btn, hint);
        root.setCenter(new StackPane(card));
    }
}