package com.phongkham.tv1_bacsi;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * ========== TV1: BÁC SĨ & TÍNH LƯƠNG ==========
 * - CRUD Bác sĩ (thêm / sửa / xóa / tìm kiếm)
 * - Tính lương theo tháng:
 *     Lương = 7.000.000 + 1.000.000 × (số BN chữa khỏi 1 loại bệnh trong tháng)
 *   Đếm theo LanKham WHERE TrangThai='DaKhoi', GROUP BY MaBS, MaBN, MaBenh
 *   (1 bệnh nhân khỏi 1 loại bệnh = 1 lượt, dù có nhiều lần khám cùng bệnh).
 *   Tháng tính theo ngày ra viện (NgayRa); nếu thiếu NgayRa thì dùng NgayVao.
 *
 * Gọi từ MainShell: BacSiView.luongBacSi()
 */
public class BacSiView {

    private static final long LUONG_CB = 7_000_000L;
    private static final long THUONG_MOI_CA = 1_000_000L;

    /** Màn hình chính của TV1: 2 tab (Danh sách Bác sĩ / Tính lương). */
    public static Node luongBacSi() {
        VBox root = new VBox(10);
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Label staffCount = UI.badge("0 nhân sự");

        Tab tabDs = new Tab("Danh sách", danhSachBS(staffCount));
        Tab tabLuong = new Tab("Tính lương", tinhLuong());
        tabs.getTabs().addAll(tabDs, tabLuong);

        root.getChildren().addAll(UI.pageHeading("Bác sĩ", staffCount), tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return root;
    }

    // =====================================================================
    //  TAB 1: CRUD BÁC SĨ
    // =====================================================================
    public static Node danhSachBS() {
        Label staffCount = UI.badge("0 nhân sự");
        VBox root = danhSachBS(staffCount);
        root.getChildren().add(0, UI.pageHeading("Bác sĩ", staffCount));
        return root;
    }

    private static VBox danhSachBS(Label staffCount) {
        VBox root = new VBox(10);

        TextField search = new TextField();
        search.setPromptText("⌕  Tìm theo tên, mã, CMT, chuyên môn...");
        search.setPrefWidth(280);

        TableView<Map<String, Object>> table = UI.table(
                "MaBS:Mã BS:70", "HoTen:Họ tên:150", "CMT:CMT:110", "NgaySinh:Ngày sinh:95",
                "DiaChi:Địa chỉ:130", "NgayVaoLam:Ngày vào làm:110", "ThamNien:Thâm niên:75",
                "TrinhDo:Trình độ:90", "ChuyenMon:Chuyên môn:110");

        String selectDoctors = """
                SELECT MaBS,CMT,HoTen,NgaySinh,DiaChi,NgayVaoLam,
                       TIMESTAMPDIFF(YEAR,NgayVaoLam,CURDATE()) AS ThamNien,TrinhDo,ChuyenMon
                FROM BacSi
                """;
        Runnable load = () -> {
            String kw = search.getText().trim();
            List<Map<String, Object>> rows;
            if (kw.isEmpty()) {
                rows = DB.query(selectDoctors + " ORDER BY MaBS");
            } else {
                String k = "%" + kw + "%";
                rows = DB.query(
                        selectDoctors + " WHERE MaBS LIKE ? OR HoTen LIKE ? OR CMT LIKE ? " +
                        "OR ChuyenMon LIKE ? OR DiaChi LIKE ? ORDER BY MaBS", k, k, k, k, k);
            }
            UI.loadTable(table, rows);
            staffCount.setText(rows.size() + " nhân sự");
        };
        search.setOnAction(e -> load.run());
        load.run();

        Button btnSearch = new Button("⚲");
        btnSearch.getStyleClass().add("filter-button");
        btnSearch.setAccessibleText("Lọc danh sách bác sĩ");
        btnSearch.setOnAction(e -> load.run());
        Button btnAdd = UI.teal("+ Thêm Bác sĩ");
        btnAdd.setOnAction(e -> formBS(null, load));
        Button btnEdit = new Button("✎  Sửa");
        btnEdit.getStyleClass().add("secondary-button");
        btnEdit.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn bác sĩ cần sửa!"); return; }
            formBS(sel, load);
        });
        Button btnDel = UI.danger("Xóa");
        btnDel.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn bác sĩ cần xóa!"); return; }
            xoaBS(UI.str(sel.get("MaBS")), UI.str(sel.get("HoTen")), load);
        });

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.toolbar(search, btnSearch, btnAdd, btnEdit, btnDel),
                tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    /** Form thêm (old == null) hoặc sửa (old != null). */
    private static void formBS(Map<String, Object> old, Runnable reload) {
        boolean edit = old != null;
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(edit ? "Sửa Bác sĩ" : "Thêm Bác sĩ");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        TextField tfMa = new TextField(edit ? UI.str(old.get("MaBS")) : "");
        TextField tfCMT = new TextField(edit ? UI.str(old.get("CMT")) : "");
        TextField tfHoTen = new TextField(edit ? UI.str(old.get("HoTen")) : "");
        TextField tfNgaySinh = new TextField(edit ? UI.str(old.get("NgaySinh")) : "");
        TextField tfDiaChi = new TextField(edit ? UI.str(old.get("DiaChi")) : "");
        DatePicker dpNgayVaoLam = new DatePicker(edit
                ? LocalDate.parse(UI.str(old.get("NgayVaoLam"))) : LocalDate.now());
        TextField tfThamNien = new TextField();
        tfThamNien.setEditable(false);
        Runnable updateSeniority = () -> tfThamNien.setText(
                String.valueOf(tinhThamNien(dpNgayVaoLam.getValue())) + " năm");
        dpNgayVaoLam.setOnAction(e -> updateSeniority.run());
        updateSeniority.run();
        ComboBox<String> cbTrinhDo = new ComboBox<>(FXCollections.observableArrayList(
                "Đại học", "Thạc sĩ", "Tiến sĩ", "Chuyên khoa I", "Chuyên khoa II"));
        ComboBox<String> cbChuyenMon = new ComboBox<>(FXCollections.observableArrayList(
                "Nội khoa", "Nhi khoa", "Ngoại khoa", "Tim mạch", "Da liễu",
                "Tai Mũi Họng", "Mắt", "Răng Hàm Mặt", "Sản phụ khoa", "Thần kinh",
                "Cơ xương khớp", "Hô hấp", "Tiêu hóa", "Nội tiết", "Thận - Tiết niệu",
                "Ung bướu", "Truyền nhiễm", "Tâm thần", "Chẩn đoán hình ảnh", "Hồi sức cấp cứu"));
        if (edit) {
            addLegacyChoice(cbTrinhDo, UI.str(old.get("TrinhDo")));
            addLegacyChoice(cbChuyenMon, UI.str(old.get("ChuyenMon")));
        }
        cbTrinhDo.setValue(edit ? UI.str(old.get("TrinhDo")) : "Đại học");
        cbChuyenMon.setValue(edit ? UI.str(old.get("ChuyenMon")) : "Nội khoa");
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        g.addRow(0, new Label("Mã BS:"), tfMa);
        g.addRow(1, new Label("CMT *:"), tfCMT);
        g.addRow(2, new Label("Họ tên *:"), tfHoTen);
        g.addRow(3, new Label("Ngày sinh (yyyy-MM-dd):"), tfNgaySinh);
        g.addRow(4, new Label("Địa chỉ:"), tfDiaChi);
        g.addRow(5, new Label("Ngày vào làm:"), dpNgayVaoLam);
        g.addRow(6, new Label("Thâm niên hiện tại:"), tfThamNien);
        g.addRow(7, new Label("Trình độ:"), cbTrinhDo);
        g.addRow(8, new Label("Chuyên môn:"), cbChuyenMon);
        if (edit) tfMa.setDisable(true);
        d.getDialogPane().setContent(g);

        // Kiểm tra dữ liệu trước khi đóng dialog
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String err = validate(tfMa, tfCMT, tfHoTen, tfNgaySinh, dpNgayVaoLam);
            if (err != null) { UI.error(err); ev.consume(); }
        });

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            Object ns = tfNgaySinh.getText().isBlank() ? null : tfNgaySinh.getText().trim();
            boolean okDb;
            if (edit) {
                okDb = DB.exec("UPDATE BacSi SET CMT=?, HoTen=?, NgaySinh=?, DiaChi=?, " +
                                "NgayVaoLam=?, TrinhDo=?, ChuyenMon=? WHERE MaBS=?",
                        tfCMT.getText().trim(), tfHoTen.getText().trim(), ns, tfDiaChi.getText().trim(),
                        java.sql.Date.valueOf(dpNgayVaoLam.getValue()), cbTrinhDo.getValue(),
                        cbChuyenMon.getValue(), tfMa.getText().trim());
            } else {
                okDb = DB.exec("INSERT INTO BacSi (MaBS,CMT,HoTen,NgaySinh,DiaChi,NgayVaoLam,TrinhDo,ChuyenMon) " +
                                "VALUES (?,?,?,?,?,?,?,?)",
                        tfMa.getText().trim(), tfCMT.getText().trim(), tfHoTen.getText().trim(), ns,
                        tfDiaChi.getText().trim(), java.sql.Date.valueOf(dpNgayVaoLam.getValue()),
                        cbTrinhDo.getValue(), cbChuyenMon.getValue());
            }
            if (okDb) { UI.alert(edit ? "Cập nhật thành công!" : "Thêm Bác sĩ thành công!"); reload.run(); }
            else UI.error("Lỗi lưu dữ liệu! (Mã BS hoặc CMT có thể bị trùng)");
        });
    }

    private static String validate(TextField ma, TextField cmt, TextField hoTen,
                                   TextField ngaySinh, DatePicker ngayVaoLam) {
        if (ma.getText().isBlank()) return "Mã BS không được để trống!";
        if (cmt.getText().isBlank()) return "CMT không được để trống!";
        if (hoTen.getText().isBlank()) return "Họ tên không được để trống!";
        if (!ngaySinh.getText().isBlank()) {
            try { LocalDate.parse(ngaySinh.getText().trim()); }
            catch (DateTimeParseException e) { return "Ngày sinh phải đúng dạng yyyy-MM-dd (vd 1980-05-15)!"; }
        }
        if (ngayVaoLam.getValue() == null) return "Vui lòng chọn ngày vào làm!";
        if (ngayVaoLam.getValue().isAfter(LocalDate.now())) return "Ngày vào làm không được ở tương lai!";
        return null;
    }

    private static long tinhThamNien(LocalDate ngayVaoLam) {
        return ngayVaoLam == null ? 0 : ChronoUnit.YEARS.between(ngayVaoLam, LocalDate.now());
    }

    private static void addLegacyChoice(ComboBox<String> choices, String value) {
        if (!value.isBlank() && !choices.getItems().contains(value)) choices.getItems().add(value);
    }

    private static void xoaBS(String ma, String ten, Runnable reload) {
        long soLK = UI.toLong(DB.query("SELECT COUNT(*) AS n FROM LanKham WHERE MaBS=?", ma).get(0).get("n"));
        if (soLK > 0) {
            UI.error("Không thể xóa " + ten + " (" + ma + ") vì đã có " + soLK + " lần khám liên quan.");
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, "Xóa bác sĩ " + ten + " (" + ma + ")?",
                ButtonType.YES, ButtonType.NO);
        a.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                if (DB.exec("DELETE FROM BacSi WHERE MaBS=?", ma)) { UI.alert("Đã xóa!"); reload.run(); }
                else UI.error("Lỗi khi xóa!");
            }
        });
    }

    // =====================================================================
    //  TAB 2: TÍNH LƯƠNG THEO THÁNG
    // =====================================================================
    public static Node tinhLuong() {
        VBox root = new VBox(10);

        ComboBox<Integer> cbT = new ComboBox<>(FXCollections.observableArrayList(1,2,3,4,5,6,7,8,9,10,11,12));
        cbT.setValue(LocalDate.now().getMonthValue());
        ComboBox<Integer> cbN = new ComboBox<>(FXCollections.observableArrayList(2024, 2025, 2026, 2027));
        cbN.setValue(LocalDate.now().getYear());

        VBox c1 = UI.card("Tổng số Bác sĩ", "0", "#0d9488");
        VBox c2 = UI.card("BN chữa khỏi (lượt)", "0", "#0284c7");
        VBox c3 = UI.card("Tổng chi lương", "0", "#7c3aed");
        HBox cards = new HBox(16, c1, c2, c3);

        TableView<Map<String, Object>> table = UI.table(
                "MaBS:Mã BS:70", "HoTen:Họ tên:150", "ChuyenMon:Chuyên môn:110",
                "ThamNien:Thâm niên:75", "LuongCB:Lương CB:110", "SoCaKhoi:Số BN khỏi:90",
                "PhuCap:Thưởng:110", "TongLuong:Tổng lương:130");

        Runnable load = () -> {
            // Mỗi (BS, BN, Bệnh) đã khỏi trong tháng chỉ tính 1 lượt
            List<Map<String, Object>> data = DB.query("""
                SELECT b.MaBS, b.HoTen, b.ChuyenMon,
                       TIMESTAMPDIFF(YEAR,b.NgayVaoLam,CURDATE()) AS ThamNien,
                       7000000 AS LuongCB,
                       COALESCE(k.SoCa,0) AS SoCaKhoi,
                       COALESCE(k.SoCa,0)*1000000 AS PhuCap,
                       7000000 + COALESCE(k.SoCa,0)*1000000 AS TongLuong
                FROM BacSi b
                LEFT JOIN (
                    SELECT MaBS, COUNT(*) AS SoCa FROM (
                        SELECT MaBS, MaBN, MaBenh
                        FROM LanKham
                        WHERE TrangThai='DaKhoi'
                          AND MONTH(COALESCE(NgayRa,NgayVao))=? AND YEAR(COALESCE(NgayRa,NgayVao))=?
                        GROUP BY MaBS, MaBN, MaBenh
                    ) t GROUP BY MaBS
                ) k ON b.MaBS=k.MaBS
                ORDER BY b.MaBS
                """, cbT.getValue(), cbN.getValue());
            UI.loadTable(table, data);
            long ca = data.stream().mapToLong(m -> UI.toLong(m.get("SoCaKhoi"))).sum();
            long luong = data.stream().mapToLong(m -> UI.toLong(m.get("TongLuong"))).sum();
            UI.setCardValue(c1, data.size() + " người");
            UI.setCardValue(c2, ca + " lượt");
            UI.setCardValue(c3, UI.MONEY.format(luong) + " đ");
        };
        cbT.setOnAction(e -> load.run());
        cbN.setOnAction(e -> load.run());
        load.run();

        Button btnCT = UI.teal("Xem chi tiết ca khỏi");
        btnCT.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn bác sĩ cần xem!"); return; }
            chiTietCaKhoi(UI.str(sel.get("MaBS")), UI.str(sel.get("HoTen")), cbT.getValue(), cbN.getValue());
        });

        Label note = new Label("Lương = " + UI.MONEY.format(LUONG_CB) + " + " + UI.MONEY.format(THUONG_MOI_CA)
                + " × số bệnh nhân chữa khỏi 1 loại bệnh trong tháng");
        note.setTextFill(javafx.scene.paint.Color.GRAY);

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.pageHeading("Tính lương Bác sĩ"),
                UI.toolbar(new Label("Tháng:"), cbT, new Label("/"), cbN, btnCT),
                note, cards, tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    private static void chiTietCaKhoi(String maBS, String tenBS, int thang, int nam) {
        List<Map<String, Object>> data = DB.query("""
            SELECT lk.MaBN, bn.HoTen AS TenBN, dm.TenBenh,
                   COUNT(*) AS SoLanKham, MAX(lk.NgayRa) AS NgayRa
            FROM LanKham lk
            JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
            JOIN DanhMucBenh dm ON lk.MaBenh=dm.MaBenh
            WHERE lk.MaBS=? AND lk.TrangThai='DaKhoi'
              AND MONTH(COALESCE(lk.NgayRa,lk.NgayVao))=? AND YEAR(COALESCE(lk.NgayRa,lk.NgayVao))=?
            GROUP BY lk.MaBN, bn.HoTen, lk.MaBenh, dm.TenBenh
            ORDER BY lk.MaBN
            """, maBS, thang, nam);

        TableView<Map<String, Object>> t = UI.table(
                "MaBN:Mã BN:80", "TenBN:Bệnh nhân:150", "TenBenh:Bệnh:130",
                "SoLanKham:Số lần khám:90", "NgayRa:Ngày ra:100");
        UI.loadTable(t, data);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Chi tiết ca khỏi");
        d.setHeaderText(tenBS + " (" + maBS + ") – tháng " + thang + "/" + nam
                + " – " + data.size() + " lượt khỏi");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        t.setPrefSize(560, 280);
        d.getDialogPane().setContent(t);
        d.showAndWait();
    }
}