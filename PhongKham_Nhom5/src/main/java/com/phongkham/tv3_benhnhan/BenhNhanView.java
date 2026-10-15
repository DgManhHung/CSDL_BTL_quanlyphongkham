package com.phongkham.tv3_benhnhan;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.util.*;

/**
 * ========== TV3: BỆNH NHÂN & HỒ SƠ BỆNH ÁN ==========
 * - CRUD bệnh nhân, tìm kiếm
 * - Hồ sơ lịch sử khám
 * - Tạo lần khám / tiếp đón
 */
public class BenhNhanView {

    public static Node danhSachBN() {
        VBox root = new VBox(16);
        TextField search = new TextField();
        search.setPromptText("Tìm tên / CMT / SĐT / mã BN...");
        search.setPrefWidth(260);

        TableView<Map<String, Object>> table = UI.table(
                "MaBN:Mã BN:80", "HoTen:Họ tên:150", "CMT:CMT:120",
                "NgaySinh:Ngày sinh:100", "DiaChi:Địa chỉ:200", "SDT:SĐT:110");

        Runnable load = () -> {
            String kw = search.getText().trim();
            if (kw.isEmpty())
                UI.loadTable(table, DB.query("SELECT * FROM BenhNhan ORDER BY MaBN"));
            else {
                String k = "%" + kw + "%";
                UI.loadTable(table, DB.query(
                    "SELECT * FROM BenhNhan WHERE HoTen LIKE ? OR CMT LIKE ? OR SDT LIKE ? OR MaBN LIKE ?",
                    k, k, k, k));
            }
        };
        search.setOnAction(e -> load.run());
        load.run();

        Button btnSearch = UI.teal("Tìm");
        btnSearch.setOnAction(e -> load.run());
        Button btnAdd = UI.teal("+ Đăng ký BN mới");
        btnAdd.setOnAction(e -> themBN(load));
        Button btnHS = UI.teal("Xem hồ sơ");
        btnHS.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn bệnh nhân!"); return; }
            showHoSo(UI.str(sel.get("MaBN")), UI.str(sel.get("HoTen")));
        });

        root.getChildren().addAll(UI.header("QUẢN LÝ BỆNH NHÂN (TV3)", search, btnSearch, btnAdd, btnHS), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    public static Node lichSuKham() {
        VBox root = new VBox(16);
        TableView<Map<String, Object>> table = UI.table(
                "MaLanKham:Mã LK:90", "TenBN:Bệnh nhân:140", "TenBenh:Bệnh:120", "TenBS:Bác sĩ:130",
                "NgayVao:Ngày vào:100", "NgayRa:Ngày ra:100", "TongTienKham:Tiền khám:100", "TrangThai:TT:100");

        Runnable load = () -> UI.loadTable(table, DB.query("""
            SELECT lk.*, bn.HoTen AS TenBN, bs.HoTen AS TenBS, dm.TenBenh
            FROM LanKham lk
            JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
            JOIN BacSi bs ON lk.MaBS=bs.MaBS
            JOIN DanhMucBenh dm ON lk.MaBenh=dm.MaBenh
            ORDER BY lk.NgayVao DESC
            """));
        load.run();

        Button btnAdd = UI.teal("+ Tiếp đón / Tạo lần khám");
        btnAdd.setOnAction(e -> taoLanKham(load));

        root.getChildren().addAll(UI.header("LỊCH SỬ KHÁM BỆNH (TV3)", btnAdd), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private static void showHoSo(String maBN, String ten) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Hồ sơ: " + ten);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setPrefSize(720, 400);
        TableView<Map<String, Object>> t = UI.table(
                "MaLanKham:Mã LK:90", "TenBenh:Bệnh:120", "TenBS:Bác sĩ:130",
                "NgayVao:Vào:100", "NgayRa:Ra:100", "TongTienKham:Tiền:100", "TrangThai:TT:100");
        UI.loadTable(t, DB.query("""
            SELECT lk.*, dm.TenBenh, bs.HoTen AS TenBS
            FROM LanKham lk
            JOIN DanhMucBenh dm ON lk.MaBenh=dm.MaBenh
            JOIN BacSi bs ON lk.MaBS=bs.MaBS
            WHERE lk.MaBN=? ORDER BY lk.NgayVao DESC
            """, maBN));
        VBox box = new VBox(10, new Label("Lịch sử khám của " + ten + " (" + maBN + ")"), t);
        box.setPadding(new Insets(12));
        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
    }

    private static void themBN(Runnable reload) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Đăng ký bệnh nhân");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        String[] labels = {"MaBN","CMT","HoTen","NgaySinh","DiaChi","SDT"};
        TextField[] f = new TextField[6];
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        for (int i = 0; i < 6; i++) {
            g.add(new Label(labels[i]+":"), 0, i);
            f[i] = new TextField();
            g.add(f[i], 1, i);
        }
        d.getDialogPane().setContent(g);
        d.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (DB.exec("INSERT INTO BenhNhan VALUES (?,?,?,?,?,?)",
                        f[0].getText(), f[1].getText(), f[2].getText(),
                        f[3].getText(), f[4].getText(), f[5].getText())) {
                    UI.alert("Đăng ký thành công!"); reload.run();
                } else UI.error("Lỗi đăng ký!");
            }
        });
    }

    private static void taoLanKham(Runnable reload) {
        List<Map<String, Object>> bns = DB.query("SELECT MaBN, HoTen FROM BenhNhan");
        List<Map<String, Object>> bss = DB.query("SELECT MaBS, HoTen FROM BacSi");
        List<Map<String, Object>> benhs = DB.query("SELECT MaBenh, TenBenh FROM DanhMucBenh");
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Tạo lần khám");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField tfMa = new TextField("LK" + String.format("%03d", DB.query("SELECT MaLanKham FROM LanKham").size() + 1));
        ComboBox<String> cbBN = new ComboBox<>();
        bns.forEach(b -> cbBN.getItems().add(b.get("MaBN") + " - " + b.get("HoTen")));
        cbBN.getSelectionModel().selectFirst();
        ComboBox<String> cbBS = new ComboBox<>();
        bss.forEach(b -> cbBS.getItems().add(b.get("MaBS") + " - " + b.get("HoTen")));
        cbBS.getSelectionModel().selectFirst();
        ComboBox<String> cbBenh = new ComboBox<>();
        benhs.forEach(b -> cbBenh.getItems().add(b.get("MaBenh") + " - " + b.get("TenBenh")));
        cbBenh.getSelectionModel().selectFirst();
        TextField tfTien = new TextField("300000");
        TextField tfNgay = new TextField(LocalDate.now().toString());
        VBox body = new VBox(8,
                new Label("Mã LK:"), tfMa, new Label("BN:"), cbBN, new Label("Bác sĩ:"), cbBS,
                new Label("Bệnh:"), cbBenh, new Label("Ngày vào:"), tfNgay, new Label("Tiền khám:"), tfTien);
        body.setPadding(new Insets(12));
        d.getDialogPane().setContent(body);
        d.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                long tien = 0;
                try { tien = Long.parseLong(tfTien.getText().trim()); } catch (Exception ignored) {}
                if (DB.exec("INSERT INTO LanKham (MaLanKham,MaBN,MaBS,MaBenh,NgayVao,TongTienKham,TrangThai) VALUES (?,?,?,?,?,?,'DangDieuTri')",
                        tfMa.getText(), cbBN.getValue().split(" - ")[0], cbBS.getValue().split(" - ")[0],
                        cbBenh.getValue().split(" - ")[0], tfNgay.getText(), tien)) {
                    UI.alert("Tạo lần khám thành công!"); reload.run();
                } else UI.error("Lỗi!");
            }
        });
    }
}