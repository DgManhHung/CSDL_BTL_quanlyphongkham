package com.phongkham.tv5_doanhthu;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * ========== TV5: HÓA ĐƠN, DOANH THU & ĐĂNG NHẬP ==========
 * - Báo cáo doanh thu (tiền khám + tiền thuốc)
 * - Quản lý hóa đơn + thanh toán
 * - (Login nằm ở shared)
 */
public class DoanhThuView {

    public static Node baoCao(String username) {
        VBox root = new VBox(16);
        ComboBox<Integer> cbT = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbT.setValue(LocalDate.now().getMonthValue());
        ComboBox<Integer> cbN = new ComboBox<>(FXCollections.observableArrayList(2024,2025,2026,2027));
        cbN.setValue(LocalDate.now().getYear());

        VBox c1 = UI.card("Tổng doanh thu", "0", "#0d9488");
        VBox c2 = UI.card("Tiền khám", "0", "#0284c7");
        VBox c3 = UI.card("Tiền thuốc", "0", "#7c3aed");
        VBox c4 = UI.card("Số hóa đơn", "0", "#ea580c");
        HBox cards = new HBox(16, c1, c2, c3, c4);

        TableView<Map<String, Object>> table = UI.table(
                "MaBS:Mã BS:80", "HoTen:Bác sĩ:160", "SoHD:Số HĐ:80", "TongDT:Doanh thu:140");

        Runnable load = () -> {
            List<Map<String, Object>> tong = DB.query("""
                SELECT COALESCE(SUM(TienKham),0) TongKham, COALESCE(SUM(TienThuoc),0) TongThuoc,
                       COALESCE(SUM(TongTien),0) TongDT, COUNT(*) SoHD
                FROM HoaDon WHERE MONTH(NgayLap)=? AND YEAR(NgayLap)=? AND TrangThai!='Huy'
                """, cbT.getValue(), cbN.getValue());
            Map<String, Object> m = tong.get(0);
            UI.setCardValue(c1, UI.MONEY.format(m.get("TongDT")) + " đ");
            UI.setCardValue(c2, UI.MONEY.format(m.get("TongKham")) + " đ");
            UI.setCardValue(c3, UI.MONEY.format(m.get("TongThuoc")) + " đ");
            UI.setCardValue(c4, UI.str(m.get("SoHD")));
            UI.loadTable(table, DB.query("""
                SELECT bs.MaBS, bs.HoTen, COUNT(DISTINCT hd.MaHoaDon) SoHD, SUM(hd.TongTien) TongDT
                FROM HoaDon hd
                JOIN LanKham lk ON hd.MaLanKham=lk.MaLanKham
                JOIN BacSi bs ON lk.MaBS=bs.MaBS
                WHERE MONTH(hd.NgayLap)=? AND YEAR(hd.NgayLap)=? AND hd.TrangThai!='Huy'
                GROUP BY bs.MaBS, bs.HoTen ORDER BY TongDT DESC
                """, cbT.getValue(), cbN.getValue()));
        };
        cbT.setOnAction(e -> load.run());
        cbN.setOnAction(e -> load.run());
        load.run();

        root.getChildren().addAll(
                UI.header("BÁO CÁO DOANH THU (TV5)", new Label("Tháng:"), cbT, new Label("/"), cbN),
                cards,
                new Label("Doanh thu theo Bác sĩ") {{ setStyle("-fx-font-weight:bold;-fx-font-size:14;"); }},
                table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    public static Node hoaDon(String username) {
        VBox root = new VBox(16);
        ComboBox<String> cbFilter = new ComboBox<>(FXCollections.observableArrayList("Tất cả","ChuaThanhToan","DaThanhToan"));
        cbFilter.setValue("Tất cả");

        TableView<Map<String, Object>> table = UI.table(
                "MaHoaDon:Mã HĐ:80", "TenBN:Bệnh nhân:130", "SDT:SĐT:100",
                "TienKham:Tiền khám:100", "TienThuoc:Tiền thuốc:100", "TongTien:Tổng:110", "TrangThai:TT:110");

        Runnable load = () -> {
            List<Map<String, Object>> all = DB.query("""
                SELECT hd.*, bn.HoTen AS TenBN, bn.SDT
                FROM HoaDon hd
                JOIN LanKham lk ON hd.MaLanKham=lk.MaLanKham
                JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
                ORDER BY hd.NgayLap DESC
                """);
            String f = cbFilter.getValue();
            if (!"Tất cả".equals(f))
                all = all.stream().filter(h -> f.equals(UI.str(h.get("TrangThai")))).toList();
            UI.loadTable(table, all);
        };
        cbFilter.setOnAction(e -> load.run());
        load.run();

        Button btnTao = UI.teal("+ Tạo hóa đơn");
        btnTao.setOnAction(e -> {
            List<Map<String, Object>> lks = DB.query("""
                SELECT lk.MaLanKham, bn.HoTen, dm.TenBenh, lk.TongTienKham
                FROM LanKham lk
                JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
                JOIN DanhMucBenh dm ON lk.MaBenh=dm.MaBenh
                WHERE lk.MaLanKham NOT IN (SELECT MaLanKham FROM HoaDon WHERE TrangThai!='Huy')
                ORDER BY lk.NgayVao DESC
                """);
            if (lks.isEmpty()) { UI.alert("Không còn lần khám cần lập HĐ!"); return; }
            ComboBox<String> cb = new ComboBox<>();
            lks.forEach(l -> cb.getItems().add(l.get("MaLanKham") + " | " + l.get("HoTen") + " | " + l.get("TenBenh")));
            cb.getSelectionModel().selectFirst();
            Dialog<ButtonType> d = new Dialog<>();
            d.setTitle("Tạo hóa đơn");
            d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            d.getDialogPane().setContent(new VBox(8, new Label("Chọn lần khám:"), cb));
            d.showAndWait().ifPresent(bt -> {
                if (bt == ButtonType.OK) {
                    String maLK = cb.getValue().split(" \\| ")[0];
                    if (taoHD(maLK, username)) { UI.alert("Tạo HĐ thành công!"); load.run(); }
                    else UI.error("Lỗi tạo HĐ!");
                }
            });
        });

        Button btnTT = UI.teal("Thanh toán");
        btnTT.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn hóa đơn!"); return; }
            if (!"ChuaThanhToan".equals(UI.str(sel.get("TrangThai")))) { UI.alert("HĐ đã thanh toán!"); return; }
            thanhToan(UI.str(sel.get("MaHoaDon")), toBD(sel.get("TongTien")), username, load);
        });

        root.getChildren().addAll(UI.header("QUẢN LÝ HÓA ĐƠN (TV5)", new Label("Lọc:"), cbFilter, btnTao, btnTT), table);
        VBox.setVgrow(table, Priority.ALWAYS);
        return root;
    }

    private static boolean taoHD(String maLK, String user) {
        try {
            List<Map<String, Object>> lk = DB.query("SELECT TongTienKham FROM LanKham WHERE MaLanKham=?", maLK);
            BigDecimal tienKham = toBD(lk.get(0).get("TongTienKham"));
            List<Map<String, Object>> th = DB.query(
                "SELECT COALESCE(SUM(ct.ThanhTien),0) s FROM ChiTietDonThuoc ct JOIN DonThuoc dt ON ct.MaDonThuoc=dt.MaDonThuoc WHERE dt.MaLanKham=?", maLK);
            BigDecimal tienThuoc = toBD(th.get(0).get("s"));
            List<Map<String, Object>> last = DB.query("SELECT MaHoaDon FROM HoaDon WHERE MaHoaDon LIKE 'HD%' ORDER BY MaHoaDon DESC LIMIT 1");
            String maHD = "HD001";
            if (!last.isEmpty()) {
                int n = Integer.parseInt(UI.str(last.get(0).get("MaHoaDon")).substring(2)) + 1;
                maHD = String.format("HD%03d", n);
            }
            return DB.exec(
                "INSERT INTO HoaDon (MaHoaDon,MaLanKham,TienKham,TienThuoc,TongTien,TrangThai,NguoiLap) VALUES (?,?,?,?,?,'ChuaThanhToan',?)",
                maHD, maLK, tienKham, tienThuoc, tienKham.add(tienThuoc), user);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void thanhToan(String maHD, BigDecimal tong, String user, Runnable reload) {
        List<Map<String, Object>> da = DB.query("SELECT COALESCE(SUM(SoTien),0) s FROM ThanhToan WHERE MaHoaDon=?", maHD);
        BigDecimal daTT = toBD(da.get(0).get("s"));
        BigDecimal con = tong.subtract(daTT);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Thanh toán " + maHD);
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField tf = new TextField(con.toPlainString());
        ComboBox<String> cbPT = new ComboBox<>(FXCollections.observableArrayList("TienMat","ChuyenKhoan","The","ViDienTu"));
        cbPT.setValue("TienMat");
        d.getDialogPane().setContent(new VBox(8,
                new Label("Còn lại: " + UI.MONEY.format(con) + " đ"),
                new Label("Số tiền:"), tf, new Label("Phương thức:"), cbPT));
        d.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                try {
                    BigDecimal st = new BigDecimal(tf.getText().trim());
                    List<Map<String, Object>> last = DB.query("SELECT MaThanhToan FROM ThanhToan WHERE MaThanhToan LIKE 'TT%' ORDER BY MaThanhToan DESC LIMIT 1");
                    String maTT = "TT001";
                    if (!last.isEmpty()) {
                        int n = Integer.parseInt(UI.str(last.get(0).get("MaThanhToan")).substring(2)) + 1;
                        maTT = String.format("TT%03d", n);
                    }
                    DB.exec("INSERT INTO ThanhToan (MaThanhToan,MaHoaDon,SoTien,PhuongThuc,NguoiThu) VALUES (?,?,?,?,?)",
                            maTT, maHD, st, cbPT.getValue(), user);
                    if (daTT.add(st).compareTo(tong) >= 0)
                        DB.exec("UPDATE HoaDon SET TrangThai='DaThanhToan' WHERE MaHoaDon=?", maHD);
                    UI.alert("Thanh toán thành công!");
                    reload.run();
                } catch (Exception ex) {
                    UI.error("Số tiền không hợp lệ!");
                }
            }
        });
    }

    private static BigDecimal toBD(Object o) {
        if (o instanceof BigDecimal bd) return bd;
        if (o instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return BigDecimal.ZERO;
    }
}