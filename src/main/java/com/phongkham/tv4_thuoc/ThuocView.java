package com.phongkham.tv4_thuoc;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.*;

/** TV4: Kho thuốc + thống kê bệnh theo tháng */
public class ThuocView {
    private static final String THUOC_MOI = "Thuốc mới...";

    public static Node khoThuoc() {
        VBox root = new VBox(10);
        Label stockCount = UI.badge("0 mặt hàng");
        TableView<Map<String, Object>> table = UI.table(
                "MaThuoc:Mã:80", "TenThuoc:Tên thuốc:180", "DonVi:Đơn vị:70",
                "GiaBan:Giá bán:100", "SoLuongTon:Tồn kho:90");
        Runnable load = () -> {
            List<Map<String, Object>> rows = DB.query("SELECT * FROM Thuoc ORDER BY MaThuoc");
            UI.loadTable(table, rows);
            stockCount.setText(rows.size() + " mặt hàng");
        };
        load.run();

        Button btnAdd = UI.teal("+ Thêm thuốc");
        btnAdd.setOnAction(e -> themThuoc(load));

        Button btnXoa = UI.danger("Xóa");
        btnXoa.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (DB.exec("DELETE FROM Thuoc WHERE MaThuoc=?", sel.get("MaThuoc"))) {
                UI.alert("Đã xóa!"); load.run();
            } else UI.error("Không xóa được (có thể đang dùng trong đơn thuốc)!");
        });

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.pageHeading("Kho thuốc", stockCount),
                UI.toolbar(btnAdd, btnXoa),
                tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    /** Chọn thuốc sẵn → cộng tồn; "Thuốc mới..." → mã Txxx tự tăng */
    private static void themThuoc(Runnable reload) {
        List<Map<String, Object>> ds = DB.query(
                "SELECT MaThuoc, TenThuoc, SoLuongTon FROM Thuoc ORDER BY MaThuoc");

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Thêm thuốc");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        d.getDialogPane().setPrefWidth(360);

        ComboBox<String> cb = new ComboBox<>();
        cb.setMaxWidth(Double.MAX_VALUE);
        ds.forEach(t -> cb.getItems().add(t.get("MaThuoc") + " - " + t.get("TenThuoc")));
        cb.getItems().add(THUOC_MOI);
        if (ds.isEmpty()) cb.getSelectionModel().select(THUOC_MOI);
        else cb.getSelectionModel().select(0);

        TextField tfMa = field(taoMaThuocMoi(), false);
        TextField tfTen = field("", true);
        TextField tfDonVi = field("Viên", true);
        TextField tfGia = field("", true);
        TextField tfSL = field("", true);
        tfTen.setPromptText("Tên thuốc");
        tfGia.setPromptText("Giá bán");
        tfSL.setPromptText("Số lượng thêm vào kho");

        Label lbMa = label("Mã thuốc:");
        Label lbTen = label("Tên thuốc:");
        Label lbDonVi = label("Đơn vị:");
        Label lbGia = label("Giá bán:");
        Label lbSL = label("Số lượng:");
        Label hint = new Label();
        hint.setStyle("-fx-text-fill:#0d9488;-fx-font-size:12;");
        hint.setWrapText(true);

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        g.add(label("Chọn thuốc:"), 0, 0); g.add(cb, 1, 0);
        g.add(lbMa, 0, 1); g.add(tfMa, 1, 1);
        g.add(lbTen, 0, 2); g.add(tfTen, 1, 2);
        g.add(lbDonVi, 0, 3); g.add(tfDonVi, 1, 3);
        g.add(lbGia, 0, 4); g.add(tfGia, 1, 4);
        g.add(lbSL, 0, 5); g.add(tfSL, 1, 5);
        g.add(hint, 0, 6, 2, 1);
        ColumnConstraints c0 = new ColumnConstraints();
        c0.setMinWidth(90);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        g.getColumnConstraints().addAll(c0, c1);
        d.getDialogPane().setContent(g);

        Node[] moiOnly = {lbMa, tfMa, lbTen, tfTen, lbDonVi, tfDonVi, lbGia, tfGia};
        Runnable sync = () -> {
            boolean moi = THUOC_MOI.equals(cb.getValue());
            for (Node n : moiOnly) { n.setVisible(moi); n.setManaged(moi); }
            if (moi) {
                tfMa.setText(taoMaThuocMoi());
                tfTen.clear(); tfDonVi.setText("Viên"); tfGia.clear(); tfSL.clear();
                hint.setText("Thuốc mới → mã tự tăng (T001, T002, ...)");
            } else if (cb.getValue() != null) {
                String ma = cb.getValue().split(" - ")[0];
                Object ton = ds.stream().filter(x -> ma.equals(String.valueOf(x.get("MaThuoc"))))
                        .map(x -> x.get("SoLuongTon")).findFirst().orElse(0);
                tfSL.clear();
                hint.setText("Tồn hiện tại: " + ton + " → số lượng sẽ CỘNG dồn");
            }
            // Resize dialog khi ẩn/hiện field
            if (d.getDialogPane().getScene() != null && d.getDialogPane().getScene().getWindow() != null)
                d.getDialogPane().getScene().getWindow().sizeToScene();
        };
        cb.setOnAction(e -> sync.run());
        sync.run();

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            int sl = parseInt(tfSL.getText());
            if (sl <= 0) { UI.error("Số lượng phải lớn hơn 0!"); return; }

            if (THUOC_MOI.equals(cb.getValue())) {
                String ten = tfTen.getText().trim();
                if (ten.isBlank()) { UI.error("Vui lòng nhập tên thuốc!"); return; }

                List<Map<String, Object>> trung = DB.query(
                        "SELECT MaThuoc FROM Thuoc WHERE LOWER(TRIM(TenThuoc))=LOWER(TRIM(?)) LIMIT 1", ten);
                if (!trung.isEmpty()) {
                    congTon(UI.str(trung.get(0).get("MaThuoc")), sl, reload);
                    return;
                }
                long gia = parseLong(tfGia.getText());
                String dv = tfDonVi.getText().trim().isEmpty() ? "Viên" : tfDonVi.getText().trim();
                String ma = taoMaThuocMoi();
                if (DB.exec("INSERT INTO Thuoc (MaThuoc,TenThuoc,DonVi,GiaBan,SoLuongTon) VALUES (?,?,?,?,?)",
                        ma, ten, dv, gia, sl)) {
                    UI.alert("Thêm thuốc mới thành công! Mã: " + ma);
                    reload.run();
                } else UI.error("Lỗi thêm thuốc!");
            } else {
                congTon(cb.getValue().split(" - ")[0], sl, reload);
            }
        });
    }

    private static void congTon(String ma, int sl, Runnable reload) {
        if (DB.exec("UPDATE Thuoc SET SoLuongTon = SoLuongTon + ? WHERE MaThuoc=?", sl, ma)) {
            UI.alert("Đã cộng " + sl + " vào tồn kho " + ma + "!");
            reload.run();
        } else UI.error("Lỗi cập nhật tồn kho!");
    }

    private static String taoMaThuocMoi() {
        List<Map<String, Object>> r = DB.query("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(MaThuoc,2) AS UNSIGNED)),0) AS maxCode
                FROM Thuoc WHERE MaThuoc REGEXP '^T[0-9]+$'
                """);
        long next = r.isEmpty() ? 1 : UI.toLong(r.get(0).get("maxCode")) + 1;
        return String.format(Locale.ROOT, "T%03d", next);
    }

    private static TextField field(String text, boolean editable) {
        TextField f = new TextField(text);
        f.setEditable(editable);
        return f;
    }

    private static Label label(String t) { return new Label(t); }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (Exception e) { return 0; }
    }

    private static long parseLong(String s) {
        try { return Long.parseLong(s.trim()); } catch (Exception e) { return 0; }
    }

    public static Node thongKeBenh() {
        VBox root = new VBox(16);
        ComboBox<Integer> cbT = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbT.setValue(LocalDate.now().getMonthValue());
        ComboBox<Integer> cbN = new ComboBox<>(FXCollections.observableArrayList(2024,2025,2026,2027));
        cbN.setValue(LocalDate.now().getYear());

        TableView<Map<String, Object>> table = UI.table(
                "MaBenh:Mã bệnh:80", "TenBenh:Tên bệnh:200", "SoBN:Số bệnh nhân:120",
                "SoDotMac:Số đợt mắc:120");
        Runnable load = () -> {
            LocalDate from = LocalDate.of(cbN.getValue(), cbT.getValue(), 1);
            LocalDate until = from.plusMonths(1);
            LocalDate today = LocalDate.now();
            if (from.getYear() == today.getYear() && from.getMonth() == today.getMonth()) {
                until = today.plusDays(1);
            }
            UI.loadTable(table, DB.query("""
                SELECT d.MaBenh, d.TenBenh, COUNT(DISTINCT d.MaBN) AS SoBN,
                       COUNT(DISTINCT d.MaDotDieuTri) AS SoDotMac
                FROM V_DotDieuTri d
                JOIN LanKham lk ON lk.MaDotDieuTri=d.MaDotDieuTri
                WHERE lk.NgayVao>=? AND lk.NgayVao<?
                GROUP BY d.MaBenh, d.TenBenh
                ORDER BY SoDotMac DESC, SoBN DESC, TenBenh
                """, java.sql.Date.valueOf(from), java.sql.Date.valueOf(until)));
        };
        cbT.setOnAction(e -> load.run());
        cbN.setOnAction(e -> load.run());
        load.run();

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.pageHeading("Thống kê bệnh theo tháng"),
                UI.toolbar(new Label("Tháng:"), cbT, new Label("/"), cbN),
                tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }
}
