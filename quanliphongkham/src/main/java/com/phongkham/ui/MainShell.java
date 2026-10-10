package com.phongkham.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Khung chính: sidebar tối + vùng nội dung – giống style module Y tá.
 */
public class MainShell {
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Stage stage;
    private final String username;
    private final Map<String, Button> menuBtns = new LinkedHashMap<>();

    public MainShell(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
        build();
        showDoanhThu(); // mặc định mở Báo cáo Doanh thu (phân hệ 5)
    }

    public BorderPane getRoot() { return root; }

    private void build() {
        // ===== SIDEBAR =====
        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(230);
        sidebar.setStyle("-fx-background-color: #1e293b;");
        sidebar.setPadding(new Insets(20, 12, 20, 12));

        Label brand = new Label("PHÒNG KHÁM");
        brand.setFont(Font.font("System", FontWeight.BOLD, 16));
        brand.setTextFill(Color.WHITE);
        Label brandSub = new Label("QUẢN LÝ HỆ THỐNG");
        brandSub.setTextFill(Color.web("#94a3b8"));
        brandSub.setFont(Font.font(11));
        VBox brandBox = new VBox(2, brand, brandSub);
        brandBox.setPadding(new Insets(0, 0, 24, 8));

        sidebar.getChildren().add(brandBox);

        // Menu items (placeholder cho module khác + module 5)
        addMenu(sidebar, "Dashboard", false, () -> showPlaceholder("Dashboard"));
        addMenu(sidebar, "Quản lý Bác sĩ", false, () -> showPlaceholder("Quản lý Bác sĩ (TV1)"));
        addMenu(sidebar, "Quản lý Y tá", false, () -> showPlaceholder("Quản lý Y tá (TV2)"));
        addMenu(sidebar, "Quản lý Bệnh nhân", false, () -> showPlaceholder("Quản lý Bệnh nhân (TV3)"));
        addMenu(sidebar, "Lịch sử khám bệnh", false, () -> showPlaceholder("Lịch sử khám (TV3/4)"));
        addMenu(sidebar, "Theo dõi Chăm sóc", false, () -> showPlaceholder("Chăm sóc (TV2)"));
        addMenu(sidebar, "Quản lý Hóa đơn", true, this::showHoaDon);
        addMenu(sidebar, "Báo cáo Doanh thu", true, this::showDoanhThu);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Label userLbl = new Label("👤  " + username);
        userLbl.setTextFill(Color.web("#94a3b8"));
        userLbl.setPadding(new Insets(8));
        Button logout = new Button("Đăng xuất");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setStyle("-fx-background-color: #334155; -fx-text-fill: white; -fx-background-radius: 6; -fx-cursor: hand;");
        logout.setOnAction(e -> stage.setScene(new javafx.scene.Scene(new LoginView(stage).getRoot(), 1100, 700)));
        sidebar.getChildren().addAll(userLbl, logout);

        root.setLeft(sidebar);

        // ===== CONTENT =====
        content.setStyle("-fx-background-color: #f8fafc;");
        content.setPadding(new Insets(24));
        root.setCenter(content);
    }

    private void addMenu(VBox sidebar, String text, boolean activeModule, Runnable action) {
        Button btn = new Button("  " + text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPrefHeight(40);
        styleMenuBtn(btn, false);
        btn.setOnAction(e -> {
            menuBtns.values().forEach(b -> styleMenuBtn(b, false));
            styleMenuBtn(btn, true);
            action.run();
        });
        if (!activeModule) {
            btn.setDisable(false);
            // vẫn cho click nhưng hiện placeholder
        }
        menuBtns.put(text, btn);
        sidebar.getChildren().add(btn);
    }

    private void styleMenuBtn(Button btn, boolean active) {
        if (active) {
            btn.setStyle("-fx-background-color: #14b8a6; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8; -fx-cursor: hand;");
        } else {
            btn.setStyle("-fx-background-color: transparent; -fx-text-fill: #cbd5e1; -fx-background-radius: 8; -fx-cursor: hand;");
        }
    }

    private void showDoanhThu() {
        content.getChildren().setAll(new DoanhThuView().getRoot());
        styleMenuBtn(menuBtns.get("Báo cáo Doanh thu"), true);
    }

    private void showHoaDon() {
        content.getChildren().setAll(new HoaDonView(username).getRoot());
        styleMenuBtn(menuBtns.get("Quản lý Hóa đơn"), true);
    }

    private void showPlaceholder(String name) {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        Label l = new Label(name);
        l.setFont(Font.font("System", FontWeight.BOLD, 22));
        l.setTextFill(Color.web("#64748b"));
        Label hint = new Label("Module này thuộc thành viên khác trong nhóm.\nKhi ghép app, thay placeholder bằng màn hình của họ.");
        hint.setTextFill(Color.GRAY);
        hint.setAlignment(Pos.CENTER);
        box.getChildren().addAll(l, hint);
        content.getChildren().setAll(box);
    }
}