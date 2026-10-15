package com.phongkham.app;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import com.phongkham.tv1_bacsi.BacSiView;
import com.phongkham.tv2_yta.YTaView;
import com.phongkham.tv3_benhnhan.BenhNhanView;
import com.phongkham.tv4_thuoc.ThuocView;
import com.phongkham.tv5_doanhthu.DoanhThuView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.*;

/**
 * Khung app chính – menu sidebar.
 * TV5 phụ trách ghép các màn hình của TV1–TV4 vào đây.
 */
public class MainShell {
    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Stage stage;
    private final String username;
    private final Map<String, Button> btns = new LinkedHashMap<>();

    public MainShell(Stage stage, String username) {
        this.stage = stage;
        this.username = username;
        build();
        show("Dashboard");
    }

    public BorderPane getRoot() { return root; }

    private void build() {
        VBox sidebar = new VBox(4);
        sidebar.setPrefWidth(235);
        sidebar.setStyle("-fx-background-color:#1e293b;");
        sidebar.setPadding(new Insets(20, 12, 20, 12));

        Label brand = new Label("PHÒNG KHÁM");
        brand.setFont(Font.font("System", FontWeight.BOLD, 16));
        brand.setTextFill(Color.WHITE);
        Label sub = new Label("NHÓM 5 – QUẢN LÝ");
        sub.setTextFill(Color.web("#94a3b8"));
        sub.setFont(Font.font(11));
        VBox brandBox = new VBox(2, brand, sub);
        brandBox.setPadding(new Insets(0, 0, 18, 8));
        sidebar.getChildren().add(brandBox);

        // ---- Menu theo thành viên ----
        addMenu(sidebar, "Dashboard", () -> show("Dashboard"));

        Label tv1 = section("TV1 – Bác sĩ");
        sidebar.getChildren().add(tv1);
        addMenu(sidebar, "Quản lý Bác sĩ", () -> show("BacSi"));

        Label tv2 = section("TV2 – Y tá");
        sidebar.getChildren().add(tv2);
        addMenu(sidebar, "Quản lý Y tá", () -> show("YTa"));
        addMenu(sidebar, "Theo dõi Chăm sóc", () -> show("PhanCong"));

        Label tv3 = section("TV3 – Bệnh nhân");
        sidebar.getChildren().add(tv3);
        addMenu(sidebar, "Quản lý Bệnh nhân", () -> show("BenhNhan"));
        addMenu(sidebar, "Lịch sử khám bệnh", () -> show("LanKham"));

        Label tv4 = section("TV4 – Thuốc");
        sidebar.getChildren().add(tv4);
        addMenu(sidebar, "Quản lý Thuốc", () -> show("Thuoc"));
        addMenu(sidebar, "Thống kê Bệnh", () -> show("ThongKeBenh"));

        Label tv5 = section("TV5 – Doanh thu");
        sidebar.getChildren().add(tv5);
        addMenu(sidebar, "Quản lý Hóa đơn", () -> show("HoaDon"));
        addMenu(sidebar, "Báo cáo Doanh thu", () -> show("DoanhThu"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Label userLbl = new Label("👤  " + username);
        userLbl.setTextFill(Color.web("#94a3b8"));
        userLbl.setPadding(new Insets(8));
        Button logout = new Button("Đăng xuất");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setStyle("-fx-background-color:#334155;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
        logout.setOnAction(e -> stage.setScene(new javafx.scene.Scene(new LoginView(stage).getRoot(), 1100, 700)));
        sidebar.getChildren().addAll(userLbl, logout);

        root.setLeft(sidebar);
        content.setStyle("-fx-background-color:#f8fafc;");
        content.setPadding(new Insets(24));
        root.setCenter(content);
    }

    private Label section(String text) {
        Label l = new Label(text);
        l.setTextFill(Color.web("#64748b"));
        l.setFont(Font.font(10));
        l.setPadding(new Insets(12, 0, 2, 8));
        return l;
    }

    private void addMenu(VBox sidebar, String text, Runnable action) {
        Button btn = new Button("  " + text);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setPrefHeight(38);
        style(btn, false);
        btn.setOnAction(e -> {
            btns.values().forEach(b -> style(b, false));
            style(btn, true);
            action.run();
        });
        btns.put(text, btn);
        sidebar.getChildren().add(btn);
    }

    private void style(Button btn, boolean on) {
        if (on)
            btn.setStyle("-fx-background-color:#14b8a6;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:8;-fx-cursor:hand;");
        else
            btn.setStyle("-fx-background-color:transparent;-fx-text-fill:#cbd5e1;-fx-background-radius:8;-fx-cursor:hand;");
    }

    private void show(String page) {
        content.getChildren().setAll(switch (page) {
            case "BacSi" -> BacSiView.placeholder();          // TV1
            case "YTa" -> YTaView.luongYTa();                 // TV2
            case "PhanCong" -> YTaView.phanCong();            // TV2
            case "BenhNhan" -> BenhNhanView.danhSachBN();     // TV3
            case "LanKham" -> BenhNhanView.lichSuKham();      // TV3
            case "Thuoc" -> ThuocView.khoThuoc();             // TV4
            case "ThongKeBenh" -> ThuocView.thongKeBenh();    // TV4
            case "HoaDon" -> DoanhThuView.hoaDon(username);   // TV5
            case "DoanhThu" -> DoanhThuView.baoCao(username); // TV5
            default -> dashboard();
        });
    }

    private VBox dashboard() {
        VBox root = new VBox(20);
        int t = LocalDate.now().getMonthValue(), n = LocalDate.now().getYear();
        try {
            List<Map<String, Object>> dt = DB.query("""
                SELECT COALESCE(SUM(TongTien),0) TongDT, COUNT(*) SoHD,
                       SUM(CASE WHEN TrangThai='ChuaThanhToan' THEN TongTien ELSE 0 END) ChuaThu
                FROM HoaDon WHERE MONTH(NgayLap)=? AND YEAR(NgayLap)=? AND TrangThai!='Huy'
                """, t, n);
            Map<String, Object> m = dt.get(0);
            root.getChildren().add(UI.title("DASHBOARD"));
            root.getChildren().add(new HBox(16,
                    UI.card("Doanh thu tháng", UI.MONEY.format(m.get("TongDT")) + " đ", "#0d9488"),
                    UI.card("Số hóa đơn", UI.str(m.get("SoHD")), "#0284c7"),
                    UI.card("Tiền chưa thu", UI.MONEY.format(m.get("ChuaThu")) + " đ", "#ea580c")
            ));
            Label hint = new Label("Menu chia theo thành viên TV1→TV5. Chọn mục bên trái để vào từng phân hệ.");
            hint.setTextFill(Color.GRAY);
            root.getChildren().add(hint);
        } catch (Exception e) {
            root.getChildren().add(new Label("Lỗi DB: " + e.getMessage()));
        }
        return root;
    }
}