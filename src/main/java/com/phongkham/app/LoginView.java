package com.phongkham.app;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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
        root.getStyleClass().add("login-root");
        VBox card = new VBox(14);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(40));
        card.setMaxWidth(400);
        card.getStyleClass().add("login-card");

        Label title = new Label("PHÒNG KHÁM TƯ NHÂN");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        title.getStyleClass().add("login-title");
        Label sub = new Label(" Đăng nhập hệ thống");
        sub.getStyleClass().add("login-subtitle");

        TextField user = new TextField();
        user.setPromptText("Tên đăng nhập");
        user.setPrefHeight(40);
        user.setMaxWidth(300);
        PasswordField pass = new PasswordField();
        pass.setPromptText("Mật khẩu");
        pass.setPrefHeight(40);
        pass.setMaxWidth(300);

        Label err = new Label();
        err.getStyleClass().add("login-error");
        err.setWrapText(true);
        err.setMaxWidth(300);

        Button btn = UI.teal("Đăng nhập");
        btn.setPrefHeight(42);
        btn.setPrefWidth(300);
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
                    Scene mainScene = new Scene(new MainShell(stage, u).getRoot(), 1280, 780);
                    var css = getClass().getResource("/app.css");
                    if (css != null) mainScene.getStylesheets().add(css.toExternalForm());
                    stage.setScene(mainScene);
                } else {
                    err.setText("Sai tên đăng nhập hoặc mật khẩu!");
                }
            } catch (Exception ex) {
                err.setText("Lỗi kết nối DB:\n" + ex.getMessage()
                        + "\n\nSửa password trong:\nshared/DB.java");
                ex.printStackTrace();
            }
        });

        Label hint = new Label("Vui lòng nhập tên và mật khẩu được cấp");
        hint.getStyleClass().add("login-hint");
        hint.setFont(Font.font("Segoe UI", 11));

        card.getChildren().addAll(title, sub, user, pass, err, btn, hint);
        root.setCenter(new StackPane(card));
    }
}