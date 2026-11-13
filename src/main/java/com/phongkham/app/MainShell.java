package com.phongkham.app;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import javafx.scene.control.Separator;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

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
        sidebar.setPrefWidth(220);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 12, 20, 12));

        Label logo = new Label("✚");
        logo.getStyleClass().add("clinic-logo");
        Label brand = new Label("QUẢN LÝ\nPHÒNG KHÁM");
        brand.getStyleClass().add("clinic-brand");
        VBox brandText = new VBox(brand);
        HBox brandBox = new HBox(10, logo, brandText);
        brandBox.setAlignment(Pos.CENTER_LEFT);
        brandBox.setPadding(new Insets(0, 0, 18, 4));
        sidebar.getChildren().add(brandBox);

        // ---- Menu theo thành viên ----
        addMenu(sidebar, "Dashboard", () -> show("Dashboard"));
        style(btns.get("Dashboard"), true);

        Label tv1 = section("Bác sĩ");
        sidebar.getChildren().add(tv1);
        addMenu(sidebar, "Quản lý Bác sĩ", () -> show("BacSi"));

        Label tv2 = section("Y tá");
        sidebar.getChildren().add(tv2);
        addMenu(sidebar, "Quản lý Y tá", () -> show("YTa"));
        addMenu(sidebar, "Theo dõi Chăm sóc", () -> show("PhanCong"));

        Label tv3 = section("Bệnh nhân");
        sidebar.getChildren().add(tv3);
        addMenu(sidebar, "Quản lý Bệnh nhân", () -> show("BenhNhan"));
        addMenu(sidebar, "Lịch sử khám bệnh", () -> show("LanKham"));

        Label tv4 = section("Thuốc");
        sidebar.getChildren().add(tv4);
        addMenu(sidebar, "Quản lý Thuốc", () -> show("Thuoc"));
        addMenu(sidebar, "Thống kê Bệnh", () -> show("ThongKeBenh"));

        Label tv5 = section("Doanh thu");
        sidebar.getChildren().add(tv5);
        addMenu(sidebar, "Quản lý Hóa đơn", () -> show("HoaDon"));
        addMenu(sidebar, "Báo cáo Doanh thu", () -> show("DoanhThu"));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        sidebar.getChildren().add(spacer);

        Label userLbl = new Label("👤  " + username);
        userLbl.getStyleClass().add("sidebar-user");
        userLbl.setTextFill(Color.web("#94a3b8"));
        userLbl.setPadding(new Insets(8));
        Button logout = new Button("Đăng xuất");
        logout.getStyleClass().add("logout-button");
        logout.setMaxWidth(Double.MAX_VALUE);
        logout.setOnAction(e -> {
            javafx.scene.Scene s = new javafx.scene.Scene(new LoginView(stage).getRoot(), 1100, 700);
            var css = getClass().getResource("/app.css");
            if (css != null) s.getStylesheets().add(css.toExternalForm());
            stage.setScene(s);
        });
        Separator separator = new Separator();
        separator.getStyleClass().add("sidebar-separator");
        sidebar.getChildren().addAll(separator, userLbl, logout);

        root.setLeft(sidebar);
        content.getStyleClass().add("main-content");
        content.setPadding(new Insets(24));
        root.setCenter(content);
    }

    private Label section(String text) {
        Label l = new Label(text.toUpperCase(Locale.ROOT));
        l.getStyleClass().add("sidebar-section");
        l.setPadding(new Insets(12, 0, 2, 8));
        return l;
    }

    private void addMenu(VBox sidebar, String text, Runnable action) {
        Label icon = new Label(menuIcon(text));
        icon.getStyleClass().add("menu-icon");
        Button btn = new Button(text, icon);
        btn.getStyleClass().add("menu-item");
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
        btn.getStyleClass().remove("menu-item-active");
        if (on) btn.getStyleClass().add("menu-item-active");
    }

    private String menuIcon(String text) {
        return switch (text) {
            case "Dashboard" -> "⌂";
            case "Quản lý Bác sĩ" -> "⚕";
            case "Quản lý Y tá" -> "✚";
            case "Theo dõi Chăm sóc" -> "♡";
            case "Quản lý Bệnh nhân" -> "♙";
            case "Lịch sử khám bệnh" -> "◷";
            case "Quản lý Thuốc" -> "⚕";
            case "Thống kê Bệnh" -> "▤";
            case "Quản lý Hóa đơn" -> "▧";
            case "Báo cáo Doanh thu" -> "▥";
            default -> "•";
        };
    }

    private void show(String page) {
        content.getChildren().setAll(switch (page) {
            case "BacSi" -> BacSiView.luongBacSi();           // TV1
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
        VBox root = new VBox(16);
        int t = LocalDate.now().getMonthValue(), n = LocalDate.now().getYear();
        try {
            List<Map<String, Object>> dt = DB.query("""
                SELECT COALESCE(SUM(TongTien),0) TongDT, COUNT(*) SoHD,
                       COALESCE(SUM(CASE WHEN TrangThai IN ('ChuaThanhToan','Chưa Thanh Toán')
                                         THEN TongTien ELSE 0 END),0) ChuaThu
                FROM HoaDon WHERE MONTH(NgayLap)=? AND YEAR(NgayLap)=? AND TrangThai!='Huy'
                """, t, n);
            Map<String, Object> m = dt.get(0);

            List<Map<String, Object>> operational = DB.query("""
                SELECT COUNT(*) AS SoLanKham,
                       (SELECT COUNT(*) FROM BenhNhan
                        WHERE MONTH(NgayDangKy)=? AND YEAR(NgayDangKy)=?) AS BenhNhanMoi
                FROM LanKham WHERE MONTH(NgayVao)=? AND YEAR(NgayVao)=?
                """, t, n, t, n);
            Map<String, Object> counts = operational.get(0);

            Label period = UI.badge("Tháng " + t + "/" + n);
            root.getChildren().add(UI.pageHeading("Tổng quan", period));
            root.getChildren().add(new HBox(12,
                    UI.card("Doanh thu tháng", UI.MONEY.format(m.get("TongDT")) + " đ", "#0d9488"),
                    UI.card("Số hóa đơn", UI.str(m.get("SoHD")), "#0284c7"),
                    UI.card("Tiền chưa thu", UI.MONEY.format(m.get("ChuaThu")) + " đ", "#ea580c"),
                    UI.card("Lượt khám tháng", UI.str(counts.get("SoLanKham")), "#7c3aed"),
                    UI.card("Bệnh nhân mới", UI.str(counts.get("BenhNhanMoi")), "#0369a1")
            ));

            TableView<Map<String, Object>> recentVisits = UI.table(
                    "MaLanKham:Mã lần khám:100", "TenBN:Bệnh nhân:150", "TenBS:Bác sĩ:140",
                    "TenBenh:Chẩn đoán:160", "NgayVao:Ngày khám:110", "TrangThai:Trạng thái:130");
            UI.loadTable(recentVisits, DB.query("""
                SELECT lk.MaLanKham, bn.HoTen AS TenBN, bs.HoTen AS TenBS,
                       dm.TenBenh, lk.NgayVao, lk.TrangThai
                FROM LanKham lk
                JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
                JOIN BacSi bs ON lk.MaBS=bs.MaBS
                JOIN DanhMucBenh dm ON lk.MaBenh=dm.MaBenh
                ORDER BY lk.NgayVao DESC, lk.MaLanKham DESC
                LIMIT 14
                """));

            TableView<Map<String, Object>> commonConditions = UI.table(
                    "TenBenh:Bệnh thường gặp:200", "SoDotMac:Số đợt mắc:90");
            UI.loadTable(commonConditions, DB.query("""
                SELECT d.TenBenh, COUNT(DISTINCT d.MaDotDieuTri) AS SoDotMac
                FROM V_DotDieuTri d
                JOIN LanKham lk ON lk.MaDotDieuTri=d.MaDotDieuTri
                WHERE MONTH(lk.NgayVao)=? AND YEAR(lk.NgayVao)=?
                GROUP BY d.MaBenh, d.TenBenh
                ORDER BY SoDotMac DESC, d.TenBenh
                LIMIT 5
                """, t, n));

            VBox recentSection = new VBox(8,
                    sectionTitle("Lượt khám gần đây", "14 lượt mới nhất"),
                    UI.tablePanel(recentVisits));
            recentSection.getStyleClass().add("dashboard-section");
            VBox conditionSection = new VBox(8,
                    sectionTitle("Bệnh thường gặp", "Trong tháng này"),
                    UI.tablePanel(commonConditions));
            conditionSection.getStyleClass().add("dashboard-section");
            HBox details = new HBox(14, recentSection, conditionSection);
            details.getStyleClass().add("dashboard-details");
            HBox.setHgrow(recentSection, Priority.ALWAYS);
            HBox.setHgrow(conditionSection, Priority.ALWAYS);
            recentSection.setMaxWidth(Double.MAX_VALUE);
            conditionSection.setMaxWidth(360);
            VBox.setVgrow(details, Priority.ALWAYS);
            root.getChildren().add(details);
        } catch (Exception e) {
            root.getChildren().add(new Label("Lỗi DB: " + e.getMessage()));
        }
        return root;
    }

    private HBox sectionTitle(String title, String subtitle) {
        Label heading = new Label(title);
        heading.getStyleClass().add("dashboard-section-title");
        Label detail = new Label(subtitle);
        detail.getStyleClass().add("dashboard-section-subtitle");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(8, heading, spacer, detail);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}