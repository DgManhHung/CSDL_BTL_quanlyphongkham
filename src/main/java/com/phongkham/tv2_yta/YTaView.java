package com.phongkham.tv2_yta;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Consumer;

/**
 * ========== TV2: Y TÁ & TÍNH LƯƠNG ==========
 * - Danh sách, tìm kiếm và quản lý Y tá
 * - Lương cơ bản theo trình độ, thâm niên; phụ cấp gồm 10% lương cơ bản và 200.000/lượt hỗ trợ
 * - Phân công chăm sóc
 */
public class YTaView {

    public static Node luongYTa() {
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Runnable[] refreshPayroll = {() -> {}};
        Label staffCount = UI.badge("0 nhân sự");
        tabs.getTabs().addAll(
                new Tab("Danh sách", danhSachYTa(() -> refreshPayroll[0].run(), staffCount)),
                new Tab("Tính lương", tinhLuong(load -> refreshPayroll[0] = load)));
        VBox root = new VBox(10, UI.pageHeading("Y tá", staffCount), tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return root;
    }

    public static Node danhSachYTa() {
        Label staffCount = UI.badge("0 nhân sự");
        VBox content = danhSachYTa(() -> {}, staffCount);
        content.getChildren().add(0, UI.pageHeading("Y tá", staffCount));
        return content;
    }

    private static VBox danhSachYTa(Runnable onChanged, Label staffCount) {
        VBox root = new VBox(10);
        TextField search = new TextField();
        search.setPromptText("⌕  Tìm theo tên, mã, CMT, SĐT...");
        search.setPrefWidth(280);

        TableView<Map<String, Object>> table = UI.table(
                "MaNV:Mã YT:80", "HoTen:Họ tên:150", "CMT:CMT:120", "NgaySinh:Ngày sinh:100",
                "DiaChi:Địa chỉ:170", "SDT:SĐT:110", "NgayVaoLam:Ngày vào làm:110",
                "TrinhDo:Trình độ:100", "ThamNien:Thâm niên:90");

        String selectNurses = """
                SELECT MaNV,CMT,HoTen,TrinhDo,NgaySinh,DiaChi,SDT,NgayVaoLam,
                       TIMESTAMPDIFF(YEAR,NgayVaoLam,CURDATE()) AS ThamNien
                FROM YTa
                """;
        Runnable load = () -> {
            String kw = search.getText().trim();
            List<Map<String, Object>> rows;
            if (kw.isEmpty()) {
                rows = DB.query(selectNurses + " ORDER BY MaNV");
            } else {
                String k = "%" + kw + "%";
                rows = DB.query(
                        selectNurses + " WHERE MaNV LIKE ? OR HoTen LIKE ? OR CMT LIKE ? " +
                        "OR SDT LIKE ? OR DiaChi LIKE ? ORDER BY MaNV", k, k, k, k, k);
            }
            UI.loadTable(table, rows);
            staffCount.setText(rows.size() + " nhân sự");
        };
        search.setOnAction(e -> load.run());
        load.run();

        Button btnSearch = new Button("⚲");
        btnSearch.getStyleClass().add("filter-button");
        btnSearch.setAccessibleText("Lọc danh sách y tá");
        btnSearch.setOnAction(e -> load.run());
        Button btnAdd = UI.teal("+ Thêm Y tá");
        btnAdd.setOnAction(e -> formYTa(null, load, onChanged));
        Button btnEdit = new Button("✎  Sửa");
        btnEdit.getStyleClass().add("secondary-button");
        btnEdit.setOnAction(e -> {
            Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { UI.alert("Chọn y tá cần sửa!"); return; }
            formYTa(selected, load, onChanged);
        });
        Button btnDelete = UI.danger("▣  Xóa");
        btnDelete.setOnAction(e -> {
            Map<String, Object> selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) { UI.alert("Chọn y tá cần xóa!"); return; }
            xoaYTa(UI.str(selected.get("MaNV")), UI.str(selected.get("HoTen")), load, onChanged);
        });

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.toolbar(search, btnSearch, btnAdd, btnEdit, btnDelete),
                tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    public static Node tinhLuong() {
        return tinhLuong(ignored -> {});
    }

    private static Node tinhLuong(Consumer<Runnable> registerReload) {
        VBox root = new VBox(10);
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
                SELECT s.MaNV, s.HoTen, s.SDT, s.TrinhDo, s.ThamNien, s.LuongCB, s.SoLuotHT,
                       s.LuongCB*0.10+s.SoLuotHT*200000 AS PhuCap,
                       s.LuongCB*1.10+s.SoLuotHT*200000 AS TongLuong
                FROM (
                    SELECT t.*,
                           CASE t.TrinhDo
                               WHEN 'Trung cấp' THEN 5000000
                               WHEN 'Cao đẳng' THEN 5500000
                               WHEN 'Cử nhân' THEN 6000000
                               ELSE 5000000
                           END + t.ThamNien*200000 AS LuongCB
                    FROM (
                        SELECT y.MaNV, y.HoTen, y.SDT, y.TrinhDo,
                               TIMESTAMPDIFF(YEAR,y.NgayVaoLam,CURDATE()) AS ThamNien,
                               COUNT(pc.MaPC) AS SoLuotHT
                        FROM YTa y
                        LEFT JOIN PhanCongYT pc ON y.MaNV=pc.MaNV
                            AND MONTH(pc.NgayPhanCong)=? AND YEAR(pc.NgayPhanCong)=?
                        GROUP BY y.MaNV, y.HoTen, y.SDT, y.TrinhDo, y.NgayVaoLam
                    ) t
                ) s
                ORDER BY s.MaNV
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
        registerReload.accept(load);

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.pageHeading("Tính lương Y tá"),
                UI.toolbar(new Label("Tháng:"), cbT, new Label("/"), cbN),
                cards, tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    public static Node phanCong() {
        VBox root = new VBox(10);
        DatePicker dpTuNgay = new DatePicker();
        dpTuNgay.setPromptText("Từ ngày");
        DatePicker dpDenNgay = new DatePicker();
        dpDenNgay.setPromptText("Đến ngày");
        Button btnXoaLoc = new Button("Xóa lọc ngày");

        TableView<Map<String, Object>> table = UI.table(
                "MaPC:Mã:50", "MaLanKham:Lần khám:90", "TenBN:Bệnh nhân:130",
                "TenYT:Y tá:130", "NgayPhanCong:Ngày:100", "LoaiHoTro:Loại HT:100");

        Runnable load = () -> {
            LocalDate tu = dpTuNgay.getValue();
            LocalDate den = dpDenNgay.getValue();
            if (tu != null && den != null && tu.isAfter(den)) {
                UI.alert("Từ ngày không được lớn hơn đến ngày!");
                return;
            }
            StringBuilder sql = new StringBuilder("""
                SELECT pc.MaPC, pc.MaLanKham, bn.HoTen AS TenBN, y.HoTen AS TenYT,
                       pc.NgayPhanCong,
                       CASE pc.LoaiHoTro
                           WHEN 'TiepDon' THEN 'Tiếp đón'
                           WHEN 'CapThuoc' THEN 'Cấp phát thuốc'
                           WHEN 'TiemThuoc' THEN 'Tiêm thuốc'
                           WHEN 'ChamSoc' THEN 'Chăm sóc người bệnh'
                           ELSE pc.LoaiHoTro
                       END AS LoaiHoTro
                FROM PhanCongYT pc
                JOIN YTa y ON pc.MaNV=y.MaNV
                JOIN LanKham lk ON pc.MaLanKham=lk.MaLanKham
                JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
                WHERE 1=1
                """);
            List<Object> params = new ArrayList<>();
            if (tu != null) {
                sql.append(" AND DATE(pc.NgayPhanCong) >= ?");
                params.add(java.sql.Date.valueOf(tu));
            }
            if (den != null) {
                sql.append(" AND DATE(pc.NgayPhanCong) <= ?");
                params.add(java.sql.Date.valueOf(den));
            }
            sql.append(" ORDER BY pc.NgayPhanCong DESC");
            UI.loadTable(table, DB.query(sql.toString(), params.toArray()));
        };
        dpTuNgay.setOnAction(e -> load.run());
        dpDenNgay.setOnAction(e -> load.run());
        btnXoaLoc.setOnAction(e -> {
            dpTuNgay.setValue(null);
            dpDenNgay.setValue(null);
            load.run();
        });
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
            ComboBox<String> cbLoai = new ComboBox<>(FXCollections.observableArrayList(
                    "Tiếp đón", "Chăm sóc người bệnh", "Đo dấu hiệu sinh tồn",
                    "Tiêm thuốc", "Cấp phát thuốc", "Hỗ trợ bác sĩ"));
            cbLoai.setValue("Chăm sóc người bệnh");
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

        HBox locNgay = new HBox(10,
                new Label("Từ ngày:"), dpTuNgay,
                new Label("Đến ngày:"), dpDenNgay,
                btnXoaLoc, btnAdd);
        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.pageHeading("Theo dõi chăm sóc"),
                UI.toolbar(locNgay),
                tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    private static void formYTa(Map<String, Object> old, Runnable reload, Runnable onChanged) {
        boolean edit = old != null;
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(edit ? "Sửa Y tá" : "Thêm Y tá");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField tfMa = new TextField(edit ? UI.str(old.get("MaNV")) : maYTaTiepTheo());
        tfMa.setEditable(false);
        TextField tfCMT = new TextField(edit ? UI.str(old.get("CMT")) : "");
        TextField tfHoTen = new TextField(edit ? UI.str(old.get("HoTen")) : "");
        ComboBox<String> cbTrinhDo = new ComboBox<>(
                FXCollections.observableArrayList("Trung cấp", "Cao đẳng", "Cử nhân"));
        cbTrinhDo.setValue(edit ? UI.str(old.get("TrinhDo")) : "Trung cấp");
        DatePicker dpNgayVaoLam = new DatePicker(edit && old.get("NgayVaoLam") != null
                ? LocalDate.parse(UI.str(old.get("NgayVaoLam"))) : LocalDate.now());
        TextField tfThamNien = new TextField();
        tfThamNien.setEditable(false);
        TextField tfLuongCB = new TextField();
        tfLuongCB.setEditable(false);
        TextField tfPhuCap = new TextField();
        tfPhuCap.setEditable(false);
        TextField tfNgaySinh = new TextField(edit ? UI.str(old.get("NgaySinh")) : "");
        TextField tfDiaChi = new TextField(edit ? UI.str(old.get("DiaChi")) : "");
        TextField tfSDT = new TextField(edit ? UI.str(old.get("SDT")) : "");

        Runnable updatePayPreview = () -> {
            LocalDate ngayVaoLam = dpNgayVaoLam.getValue();
            int thamNien = ngayVaoLam == null ? 0
                    : (int) Math.max(0, ChronoUnit.YEARS.between(ngayVaoLam, LocalDate.now()));
            tfThamNien.setText(thamNien + " năm");
            long luongCB = luongCoBan(cbTrinhDo.getValue(), thamNien);
            tfLuongCB.setText(UI.MONEY.format(luongCB) + " đ");
            tfPhuCap.setText(UI.MONEY.format(luongCB / 10) + " đ");
        };
        cbTrinhDo.setOnAction(e -> updatePayPreview.run());
        dpNgayVaoLam.setOnAction(e -> updatePayPreview.run());
        updatePayPreview.run();

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        g.addRow(0, new Label("Mã YT:"), tfMa);
        g.addRow(1, new Label("CMT:"), tfCMT);
        g.addRow(2, new Label("Họ tên:"), tfHoTen);
        g.addRow(3, new Label("Trình độ:"), cbTrinhDo);
        g.addRow(4, new Label("Ngày vào làm:"), dpNgayVaoLam);
        g.addRow(5, new Label("Thâm niên hiện tại:"), tfThamNien);
        g.addRow(6, new Label("Lương cơ bản:"), tfLuongCB);
        g.addRow(7, new Label("Phụ cấp:"), tfPhuCap);
        g.addRow(8, new Label("Ngày sinh (yyyy-MM-dd):"), tfNgaySinh);
        g.addRow(9, new Label("Địa chỉ:"), tfDiaChi);
        g.addRow(10, new Label("SĐT:"), tfSDT);
        d.getDialogPane().setContent(g);

        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (tfCMT.getText().isBlank() || tfHoTen.getText().isBlank()) {
                UI.error("CMT và họ tên không được để trống!");
                event.consume();
            } else if (dpNgayVaoLam.getValue() == null
                    || dpNgayVaoLam.getValue().isAfter(LocalDate.now())) {
                UI.error("Ngày vào làm phải được chọn và không ở tương lai!");
                event.consume();
            } else if (!tfNgaySinh.getText().isBlank()) {
                try {
                    LocalDate.parse(tfNgaySinh.getText().trim());
                } catch (java.time.format.DateTimeParseException ex) {
                    UI.error("Ngày sinh phải đúng dạng yyyy-MM-dd!");
                    event.consume();
                }
            }
        });

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            Object ngaySinh = tfNgaySinh.getText().isBlank() ? null : tfNgaySinh.getText().trim();
            boolean saved = edit
                    ? DB.exec("UPDATE YTa SET CMT=?,HoTen=?,TrinhDo=?,NgayVaoLam=?,NgaySinh=?,DiaChi=?,SDT=? WHERE MaNV=?",
                            tfCMT.getText().trim(), tfHoTen.getText().trim(), cbTrinhDo.getValue(),
                            java.sql.Date.valueOf(dpNgayVaoLam.getValue()), ngaySinh,
                            tfDiaChi.getText().trim(), tfSDT.getText().trim(), tfMa.getText())
                    : DB.exec("INSERT INTO YTa (MaNV,CMT,HoTen,TrinhDo,NgayVaoLam,NgaySinh,DiaChi,SDT) VALUES (?,?,?,?,?,?,?,?)",
                            tfMa.getText(), tfCMT.getText().trim(), tfHoTen.getText().trim(),
                            cbTrinhDo.getValue(), java.sql.Date.valueOf(dpNgayVaoLam.getValue()), ngaySinh,
                            tfDiaChi.getText().trim(), tfSDT.getText().trim());
            if (saved) {
                UI.alert(edit ? "Cập nhật thành công!" : "Thêm Y tá thành công!");
                reload.run();
                onChanged.run();
            } else {
                UI.error("Lỗi lưu dữ liệu! (CMT có thể bị trùng)");
            }
        });
    }

    private static void xoaYTa(String ma, String ten, Runnable reload, Runnable onChanged) {
        long assignments = UI.toLong(DB.query(
                "SELECT COUNT(*) AS n FROM PhanCongYT WHERE MaNV=?", ma).get(0).get("n"));
        if (assignments > 0) {
            UI.error("Không thể xóa " + ten + " vì đã có lịch sử phân công.");
            return;
        }
        if (DB.exec("DELETE FROM YTa WHERE MaNV=?", ma)) {
            UI.alert("Đã xóa y tá!");
            reload.run();
            onChanged.run();
        } else {
            UI.error("Không thể xóa y tá!");
        }
    }

    private static String maYTaTiepTheo() {
        List<Map<String, Object>> rows = DB.query("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(MaNV,3) AS UNSIGNED)),0) AS maxCode
                FROM YTa WHERE MaNV REGEXP '^YT[0-9]+$'
                """);
        return String.format(Locale.ROOT, "YT%03d", UI.toLong(rows.get(0).get("maxCode")) + 1);
    }

    private static long luongCoBan(String trinhDo, int thamNien) {
        long theoTrinhDo = switch (trinhDo) {
            case "Cao đẳng" -> 5_500_000;
            case "Cử nhân" -> 6_000_000;
            default -> 5_000_000;
        };
        return theoTrinhDo + thamNien * 200_000L;
    }
}