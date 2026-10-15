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

/**
 * ========== TV4: THUỐC, ĐƠN THUỐC & THỐNG KÊ BỆNH ==========
 * - CRUD kho thuốc
 * - Thống kê bệnh theo tháng
 */
public class ThuocView {

    public static Node khoThuoc() {
        VBox root = new VBox(16);
        TableView<Map<String, Object>> table = UI.table(
                "MaThuoc:Mã:80", "TenThuoc:Tên thuốc:180", "DonVi:Đơn vị:70",
                "GiaBan:Giá bán:100", "SoLuongTon:Tồn kho:90");

        Runnable load = () -> UI.loadTable(table, DB.query("SELECT * FROM Thuoc ORDER BY MaThuoc"));
        load.run();

        Button btnAdd = UI.teal("+ Thêm thuốc");
        btnAdd.setOnAction(e -> {
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Thêm thuốc");
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            String[] lbs = {"MaThuoc","TenThuoc","DonVi","GiaBan","SoLuongTon"};
            TextField[] f = new TextField[5];
            GridPane g = new GridPane();
            g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
            for (int i = 0; i < 5; i++) {
                g.add(new Label(lbs[i]+":"), 0, i);
                f[i] = new TextField();
                if (i == 2) f[i].setText("Viên");
                g.add(f[i], 1, i);
            }
            d.getDialogPane().setContent(g);
            d.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    long gia = 0; int ton = 0;
                    try { gia = Long.parseLong(f[3].getText().trim()); } catch (Exception ignored) {}
                    try { ton = Integer.parseInt(f[4].getText().trim()); } catch (Exception ignored) {}
                    if (DB.exec("INSERT INTO Thuoc VALUES (?,?,?,?,?)",
                            f[0].getText(), f[1].getText(), f[2].getText(), gia, ton)) {
                        UI.alert("Thêm thành công!"); load.run();
                    } else UI.error("Lỗi!");
                }
            });
        });

        Button btnXoa = new Button("Xóa");
        btnXoa.setStyle("-fx-background-color:#ef4444;-fx-text-fill:white;-fx-background-radius:6;-fx-cursor:hand;");
        btnXoa.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            if (DB.exec("DELETE FROM Thuoc WHERE MaThuoc=?", sel.get("MaThuoc"))) {
                UI.alert("Đã xóa!"); load.run();
            } else UI.error("Không xóa được (có thể đang dùng trong đơn thuốc)!");
        });

        root.getChildren().addAll(UI.header("QUẢN LÝ KHO THUỐC (TV4)", btnAdd, btnXoa), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    public static Node thongKeBenh() {
        VBox root = new VBox(16);
        ComboBox<Integer> cbT = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbT.setValue(LocalDate.now().getMonthValue());
        ComboBox<Integer> cbN = new ComboBox<>(FXCollections.observableArrayList(2024,2025,2026,2027));
        cbN.setValue(LocalDate.now().getYear());

        TableView<Map<String, Object>> table = UI.table(
                "MaBenh:Mã bệnh:80", "TenBenh:Tên bệnh:200", "SoBN:Số bệnh nhân:120", "SoLanKham:Số lần khám:120");

        Runnable load = () -> UI.loadTable(table, DB.query("""
            SELECT dm.MaBenh, dm.TenBenh,
                   COUNT(DISTINCT lk.MaBN) AS SoBN,
                   COUNT(*) AS SoLanKham
            FROM LanKham lk
            JOIN DanhMucBenh dm ON lk.MaBenh=dm.MaBenh
            WHERE MONTH(lk.NgayVao)=? AND YEAR(lk.NgayVao)=?
            GROUP BY dm.MaBenh, dm.TenBenh
            ORDER BY SoBN DESC
            """, cbT.getValue(), cbN.getValue()));
        cbT.setOnAction(e -> load.run());
        cbN.setOnAction(e -> load.run());
        load.run();

        root.getChildren().addAll(
                UI.header("THỐNG KÊ BỆNH THEO THÁNG (TV4)", new Label("Tháng:"), cbT, new Label("/"), cbN),
                table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }
}