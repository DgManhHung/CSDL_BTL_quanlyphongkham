package com.phongkham.tv2_yta;

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
 * ========== TV2: Y TÁ & TÍNH LƯƠNG ==========
 * - CRUD Y tá
 * - Tính lương: 5.000.000 + 200.000/lượt hỗ trợ
 * - Phân công chăm sóc
 */
public class YTaView {

    public static Node luongYTa() {
        VBox root = new VBox(16);
        ComboBox<Integer> cbT = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbT.setValue(LocalDate.now().getMonthValue());
        ComboBox<Integer> cbN = new ComboBox<>(FXCollections.observableArrayList(2024,2025,2026,2027));
        cbN.setValue(LocalDate.now().getYear());

        VBox c1 = UI.card("Tổng số Y tá", "0", "#0d9488");
        VBox c2 = UI.card("Lượt hỗ trợ", "0", "#0284c7");
        VBox c3 = UI.card("Tổng chi lương", "0", "#7c3aed");
        HBox cards = new HBox(16, c1, c2, c3);

        TableView<Map<String, Object>> table = UI.table(
                "MaNV:Mã YT:70", "HoTen:Họ tên:140", "SDT:SĐT:100", "TrinhDo:Trình độ:90",
                "ThamNien:Thâm niên:80", "LuongCB:Lương CB:100", "SoLuotHT:Số lượt:70",
                "PhuCap:Phụ cấp:100", "TongLuong:Tổng lương:120");

        Runnable load = () -> {
            List<Map<String, Object>> data = DB.query("""
                SELECT y.MaNV, y.HoTen, y.SDT, y.TrinhDo, y.ThamNien,
                       5000000 AS LuongCB,
                       COALESCE(COUNT(pc.MaPC),0) AS SoLuotHT,
                       COALESCE(COUNT(pc.MaPC),0)*200000 AS PhuCap,
                       5000000+COALESCE(COUNT(pc.MaPC),0)*200000 AS TongLuong
                FROM YTa y
                LEFT JOIN PhanCongYT pc ON y.MaNV=pc.MaNV
                    AND MONTH(pc.NgayPhanCong)=? AND YEAR(pc.NgayPhanCong)=?
                GROUP BY y.MaNV, y.HoTen, y.SDT, y.TrinhDo, y.ThamNien
                ORDER BY y.MaNV
                """, cbT.getValue(), cbN.getValue());
            UI.loadTable(table, data);
            UI.setCardValue(c1, data.size() + " người");
            long luot = data.stream().mapToLong(m -> UI.toLong(m.get("SoLuotHT"))).sum();
            long luong = data.stream().mapToLong(m -> UI.toLong(m.get("TongLuong"))).sum();
            UI.setCardValue(c2, luot + " lượt");
            UI.setCardValue(c3, UI.MONEY.format(luong) + " đ");
        };
        cbT.setOnAction(e -> load.run());
        cbN.setOnAction(e -> load.run());
        load.run();

        Button btnAdd = UI.teal("+ Thêm Y tá");
        btnAdd.setOnAction(e -> themYTa(load));

        root.getChildren().addAll(
                UI.header("QUẢN LÝ Y TÁ & TÍNH LƯƠNG (TV2)", new Label("Tháng:"), cbT, new Label("/"), cbN, btnAdd),
                cards, table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    public static Node phanCong() {
        VBox root = new VBox(16);
        TableView<Map<String, Object>> table = UI.table(
                "MaPC:Mã:50", "MaLanKham:Lần khám:90", "TenBN:Bệnh nhân:130",
                "TenYT:Y tá:130", "NgayPhanCong:Ngày:100", "LoaiHoTro:Loại HT:100");

        Runnable load = () -> UI.loadTable(table, DB.query("""
            SELECT pc.MaPC, pc.MaLanKham, bn.HoTen AS TenBN, y.HoTen AS TenYT,
                   pc.NgayPhanCong, pc.LoaiHoTro
            FROM PhanCongYT pc
            JOIN YTa y ON pc.MaNV=y.MaNV
            JOIN LanKham lk ON pc.MaLanKham=lk.MaLanKham
            JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
            ORDER BY pc.NgayPhanCong DESC
            """));
        load.run();

        Button btnAdd = UI.teal("+ Phân công Y tá");
        btnAdd.setOnAction(e -> {
            List<Map<String, Object>> lks = DB.query(
                "SELECT lk.MaLanKham, bn.HoTen FROM LanKham lk JOIN BenhNhan bn ON lk.MaBN=bn.MaBN ORDER BY lk.NgayVao DESC");
            List<Map<String, Object>> yts = DB.query("SELECT MaNV, HoTen FROM YTa");
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Phân công Y tá");
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            ComboBox<String> cbLK = new ComboBox<>();
            lks.forEach(l -> cbLK.getItems().add(l.get("MaLanKham") + " - " + l.get("HoTen")));
            cbLK.getSelectionModel().selectFirst();
            ComboBox<String> cbYT = new ComboBox<>();
            yts.forEach(y -> cbYT.getItems().add(y.get("MaNV") + " - " + y.get("HoTen")));
            cbYT.getSelectionModel().selectFirst();
            ComboBox<String> cbLoai = new ComboBox<>(FXCollections.observableArrayList("TiepDon","CapThuoc","TiemThuoc","ChamSoc"));
            cbLoai.setValue("ChamSoc");
            TextField tfNgay = new TextField(LocalDate.now().toString());
            VBox body = new VBox(8, new Label("Lần khám:"), cbLK, new Label("Y tá:"), cbYT,
                    new Label("Loại:"), cbLoai, new Label("Ngày:"), tfNgay);
            body.setPadding(new Insets(12));
            d.getDialogPane().setContent(body);
            d.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    String maLK = cbLK.getValue().split(" - ")[0];
                    String maNV = cbYT.getValue().split(" - ")[0];
                    if (DB.exec("INSERT INTO PhanCongYT (MaLanKham,MaNV,NgayPhanCong,LoaiHoTro) VALUES (?,?,?,?)",
                            maLK, maNV, tfNgay.getText(), cbLoai.getValue())) {
                        UI.alert("Phân công thành công!"); load.run();
                    } else UI.error("Lỗi phân công!");
                }
            });
        });

        root.getChildren().addAll(UI.header("THEO DÕI CHĂM SÓC (TV2)", btnAdd), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private static void themYTa(Runnable reload) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Thêm Y tá");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField[] f = new TextField[8];
        String[] labels = {"MaNV","CMT","HoTen","TrinhDo","ThamNien","NgaySinh","DiaChi","SDT"};
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        for (int i = 0; i < labels.length; i++) {
            g.add(new Label(labels[i]+":"), 0, i);
            f[i] = new TextField();
            g.add(f[i], 1, i);
        }
        d.getDialogPane().setContent(g);
        d.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                int tn = 0;
                try { tn = Integer.parseInt(f[4].getText().trim()); } catch (Exception ignored) {}
                if (DB.exec("INSERT INTO YTa VALUES (?,?,?,?,?,?,?,?)",
                        f[0].getText(), f[1].getText(), f[2].getText(), f[3].getText(),
                        tn, f[5].getText(), f[6].getText(), f[7].getText())) {
                    UI.alert("Thêm Y tá thành công!"); reload.run();
                } else UI.error("Lỗi thêm!");
            }
        });
    }
}