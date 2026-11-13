package com.phongkham.tv3_benhnhan;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * ========== TV3: BỆNH NHÂN & HỒ SƠ BỆNH ÁN ==========
 * - CRUD bệnh nhân, tìm kiếm
 * - Hồ sơ lịch sử khám
 * - Tạo lần khám / tiếp đón
 */
public class BenhNhanView {
    private static final String BENH_KHAC = "Khác...";
    private static final List<String> DANH_SACH_BENH = List.of(
            "Cảm cúm", "Viêm họng", "Đau dạ dày", "Cao huyết áp", "Viêm phổi",
            "Đái tháo đường type 2", "Rối loạn mỡ máu", "Viêm xoang", "Dị ứng",
            "Đau nửa đầu", "Viêm da", "Viêm đường tiết niệu", "Sốt xuất huyết",
            "Viêm phế quản", "Hen phế quản", "Thoái hóa khớp", "Đau lưng",
            "Trào ngược dạ dày-thực quản", "Thiếu máu", "Mất ngủ");

    public static Node danhSachBN() {
        VBox root = new VBox(10);
        Label patientCount = UI.badge("0 bệnh nhân");
        TextField search = new TextField();
        search.setPromptText("⌕  Tìm theo tên, mã, CMT, SĐT...");
        search.setPrefWidth(280);

        TableView<Map<String, Object>> table = UI.table(
                "MaBN:Mã BN:80", "HoTen:Họ tên:150", "GioiTinh:Giới tính:80",
                "CMT:CMT:120", "NgaySinh:Ngày sinh:100", "NgayDangKy:Ngày đăng ký:110",
                "DiaChi:Địa chỉ:200", "SDT:SĐT:110", "Email:Email:180");

        Runnable load = () -> {
            String kw = search.getText().trim();
            List<Map<String, Object>> rows;
            if (kw.isEmpty())
                rows = DB.query("SELECT * FROM BenhNhan ORDER BY MaBN");
            else {
                String k = "%" + kw + "%";
                rows = DB.query(
                    "SELECT * FROM BenhNhan WHERE HoTen LIKE ? OR CMT LIKE ? OR SDT LIKE ? OR Email LIKE ? OR MaBN LIKE ?",
                    k, k, k, k, k);
            }
            UI.loadTable(table, rows);
            patientCount.setText(rows.size() + " bệnh nhân");
        };
        search.setOnAction(e -> load.run());
        load.run();

        Button btnSearch = new Button("⚲");
        btnSearch.getStyleClass().add("filter-button");
        btnSearch.setAccessibleText("Lọc danh sách bệnh nhân");
        btnSearch.setOnAction(e -> load.run());
        Button btnAdd = UI.teal("+ Đăng ký BN mới");
        btnAdd.setOnAction(e -> themBN(load));
        Button btnHS = new Button("▤  Xem hồ sơ");
        btnHS.getStyleClass().add("secondary-button");
        btnHS.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn bệnh nhân!"); return; }
            showHoSo(UI.str(sel.get("MaBN")), UI.str(sel.get("HoTen")));
        });

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.pageHeading("Bệnh nhân", patientCount),
                UI.toolbar(search, btnSearch, btnAdd, btnHS),
                tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    public static Node lichSuKham() {
        VBox root = new VBox(12);
        DatePicker dpTuNgay = new DatePicker();
        dpTuNgay.setPromptText("Từ ngày");
        DatePicker dpDenNgay = new DatePicker();
        dpDenNgay.setPromptText("Đến ngày");
        Button btnXoaLoc = new Button("Xóa lọc ngày");

        TableView<Map<String, Object>> table = UI.table(
                "MaLanKham:Mã LK:90", "TenBN:Bệnh nhân:140", "TenBenh:Bệnh:120", "TenBS:Bác sĩ:130",
                "NgayVao:Ngày vào:100", "NgayRa:Ngày ra:100", "TongTienKham:Tiền khám:100", "TrangThai:TT:100");

        Runnable load = () -> {
            LocalDate tu = dpTuNgay.getValue();
            LocalDate den = dpDenNgay.getValue();
            if (tu != null && den != null && tu.isAfter(den)) {
                UI.alert("Từ ngày không được lớn hơn đến ngày!");
                return;
            }
            StringBuilder sql = new StringBuilder("""
                SELECT lk.*, bn.HoTen AS TenBN, bs.HoTen AS TenBS, dm.TenBenh
                FROM LanKham lk
                JOIN BenhNhan bn ON lk.MaBN=bn.MaBN
                JOIN BacSi bs ON lk.MaBS=bs.MaBS
                JOIN DanhMucBenh dm ON lk.MaBenh=dm.MaBenh
                WHERE 1=1
                """);
            List<Object> params = new ArrayList<>();
            if (tu != null) {
                sql.append(" AND DATE(lk.NgayVao) >= ?");
                params.add(java.sql.Date.valueOf(tu));
            }
            if (den != null) {
                sql.append(" AND DATE(lk.NgayVao) <= ?");
                params.add(java.sql.Date.valueOf(den));
            }
            sql.append(" ORDER BY lk.NgayVao DESC");
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

        Button btnAdd = UI.teal("+ Tiếp đón / Tạo lần khám");
        btnAdd.setOnAction(e -> taoLanKham(load));

        HBox locNgay = new HBox(10,
                new Label("Từ ngày:"), dpTuNgay,
                new Label("Đến ngày:"), dpDenNgay,
                btnXoaLoc);
        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.pageHeading("Lịch sử khám bệnh"),
                UI.toolbar(locNgay, btnAdd),
                tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    private static void showHoSo(String maBN, String ten) {
        Dialog<Void> dlg = new Dialog<>();
        dlg.setTitle("Hồ sơ: " + ten);
        dlg.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dlg.getDialogPane().setPrefSize(720, 400);
        TableView<Map<String, Object>> t = UI.table(
                "MaLanKham:Mã LK:90", "MaDotDieuTri:Mã đợt:110", "TenBenh:Bệnh:120",
                "LanThuTrongDot:Lần trong đợt:100", "TongLanKhamBenh:Tổng lượt bệnh:110",
                "TenBS:Bác sĩ:130", "NgayVao:Vào:100", "NgayRa:Ra:100", "ThuocKe:Thuốc kê:180",
                "HoTroYTa:Y tá hỗ trợ:180", "TongTienKham:Tiền khám:100",
                "TongTienThuoc:Tiền thuốc:100", "TrangThai:TT:100", "TrangThaiDot:Tình trạng đợt:120");
        UI.loadTable(t, DB.query("""
            SELECT MaBN, TenBN, MaLanKham, MaDotDieuTri, TenBenh, LanThuTrongDot,
                   TongLanKhamBenh, TenBS, NgayVao, NgayRa, ThuocKe, HoTroYTa,
                   TongTienKham, TongTienThuoc, TrangThai, TrangThaiDot
            FROM V_HoSoBenhNhan
            WHERE MaBN=? ORDER BY NgayVao DESC, MaLanKham DESC
            """, maBN));

        TableView<Map<String, Object>> current = UI.table(
                "MaDotDieuTri:Mã đợt:110", "TenBenh:Bệnh hiện tại:160",
                "NgayBatDau:Bắt đầu:100", "SoLanKham:Số lần khám:100");
        UI.loadTable(current, DB.query("""
            SELECT MaDotDieuTri, TenBenh, NgayBatDau, SoLanKham
            FROM V_DotDieuTri
            WHERE MaBN=? AND TrangThai='DangDieuTri'
            ORDER BY NgayBatDau
            """, maBN));
        VBox box = new VBox(10,
                new Label("Hồ sơ bệnh nhân: " + ten + " (" + maBN + ")"),
                new Label("Bệnh/đợt đang điều trị"),
                UI.tablePanel(current),
                new Label("Lịch sử khám từ trước đến nay"),
                UI.tablePanel(t));
        box.setPadding(new Insets(12));
        dlg.getDialogPane().setContent(box);
        dlg.showAndWait();
    }

    private static void themBN(Runnable reload) {
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Đăng ký bệnh nhân");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        TextField tfMa = new TextField();
        TextField tfCMT = new TextField();
        TextField tfHoTen = new TextField();
        ComboBox<String> cbGioiTinh = new ComboBox<>(
                FXCollections.observableArrayList("Nam", "Nữ"));
        TextField tfNgaySinh = new TextField();
        TextField tfDiaChi = new TextField();
        TextField tfSDT = new TextField();
        TextField tfEmail = new TextField();
        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        g.addRow(0, new Label("Mã bệnh nhân *:"), tfMa);
        g.addRow(1, new Label("CMT *:"), tfCMT);
        g.addRow(2, new Label("Họ tên *:"), tfHoTen);
        g.addRow(3, new Label("Giới tính *:"), cbGioiTinh);
        g.addRow(4, new Label("Ngày sinh (yyyy-MM-dd) *:"), tfNgaySinh);
        g.addRow(5, new Label("Địa chỉ *:"), tfDiaChi);
        g.addRow(6, new Label("SĐT *:"), tfSDT);
        g.addRow(7, new Label("Email *:"), tfEmail);
        d.getDialogPane().setContent(g);

        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (tfMa.getText().isBlank() || tfCMT.getText().isBlank()
                    || tfHoTen.getText().isBlank() || tfNgaySinh.getText().isBlank()
                    || tfDiaChi.getText().isBlank() || tfSDT.getText().isBlank()
                    || tfEmail.getText().isBlank()) {
                UI.error("Vui lòng nhập đầy đủ thông tin bắt buộc!");
                event.consume();
            } else if (cbGioiTinh.getValue() == null) {
                UI.error("Vui lòng chọn giới tính!");
                event.consume();
            } else {
                try {
                    LocalDate.parse(tfNgaySinh.getText().trim());
                } catch (DateTimeParseException ex) {
                    UI.error("Ngày sinh phải đúng dạng yyyy-MM-dd!");
                    event.consume();
                }
            }
        });

        d.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (DB.exec("INSERT INTO BenhNhan " +
                                "(MaBN,CMT,HoTen,GioiTinh,NgaySinh,NgayDangKy,DiaChi,SDT,Email) " +
                                "VALUES (?,?,?,?,?,?,?,?,?)",
                        tfMa.getText().trim(), tfCMT.getText().trim(), tfHoTen.getText().trim(),
                        cbGioiTinh.getValue(), java.sql.Date.valueOf(LocalDate.parse(tfNgaySinh.getText().trim())),
                        java.sql.Date.valueOf(LocalDate.now()), tfDiaChi.getText().trim(),
                        tfSDT.getText().trim(), tfEmail.getText().trim())) {
                    UI.alert("Đăng ký thành công!"); reload.run();
                } else UI.error("Lỗi đăng ký!");
            }
        });
    }

    private static void taoLanKham(Runnable reload) {
        List<Map<String, Object>> bns = DB.query("SELECT MaBN, HoTen FROM BenhNhan");
        List<Map<String, Object>> bss = DB.query("SELECT MaBS, HoTen FROM BacSi");
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
        ComboBox<String> cbBenh = new ComboBox<>(FXCollections.observableArrayList(DANH_SACH_BENH));
        cbBenh.getItems().add(BENH_KHAC);
        cbBenh.getSelectionModel().selectFirst();
        ComboBox<String> cbDot = new ComboBox<>();
        ComboBox<String> cbTrangThai = new ComboBox<>(FXCollections.observableArrayList(
                "Đang điều trị", "Đã khỏi"));
        cbTrangThai.setValue("Đang điều trị");
        TextField tfBenhKhac = new TextField();
        tfBenhKhac.setPromptText("Nhập tên bệnh");
        tfBenhKhac.setVisible(false);
        tfBenhKhac.setManaged(false);
        cbBenh.setOnAction(e -> {
            boolean nhapBenhKhac = BENH_KHAC.equals(cbBenh.getValue());
            tfBenhKhac.setVisible(nhapBenhKhac);
            tfBenhKhac.setManaged(nhapBenhKhac);
        });
        TextField tfTien = new TextField("300000");
        TextField tfNgay = new TextField(LocalDate.now().toString());

        Runnable refreshEpisodes = () -> {
            cbDot.getItems().clear();
            String tenBenh = BENH_KHAC.equals(cbBenh.getValue())
                    ? tfBenhKhac.getText().trim() : cbBenh.getValue();
            if (tenBenh.isBlank() || cbBN.getValue() == null) {
                cbDot.getItems().add("Đợt điều trị mới");
                cbDot.getSelectionModel().selectFirst();
                return;
            }
            List<Map<String, Object>> disease = DB.query(
                    "SELECT MaBenh FROM DanhMucBenh WHERE LOWER(TRIM(TenBenh))=LOWER(TRIM(?)) LIMIT 1",
                    tenBenh);
            if (!disease.isEmpty()) {
                List<Map<String, Object>> episodes = DB.query("""
                    SELECT d.MaDotDieuTri, d.MaBS, bs.HoTen AS TenBS, d.NgayBatDau, d.MaBenh
                    FROM DotDieuTri d JOIN BacSi bs ON bs.MaBS=d.MaBS
                    WHERE d.MaBN=? AND d.MaBenh=? AND d.TrangThai='DangDieuTri'
                    ORDER BY d.NgayBatDau DESC
                    """, cbBN.getValue().split(" - ")[0], disease.get(0).get("MaBenh"));
                for (Map<String, Object> episode : episodes) {
                    cbDot.getItems().add(UI.str(episode.get("MaDotDieuTri")) + " | "
                            + UI.str(episode.get("TenBS")) + " | " + UI.str(episode.get("NgayBatDau")));
                }
            }
            if (cbDot.getItems().isEmpty()) cbDot.getItems().add("Đợt điều trị mới");
            cbDot.getSelectionModel().selectFirst();
        };
        cbBN.setOnAction(e -> refreshEpisodes.run());
        cbBenh.setOnAction(e -> {
            boolean nhapBenhKhac = BENH_KHAC.equals(cbBenh.getValue());
            tfBenhKhac.setVisible(nhapBenhKhac);
            tfBenhKhac.setManaged(nhapBenhKhac);
            refreshEpisodes.run();
        });
        tfBenhKhac.textProperty().addListener((obs, oldValue, newValue) -> refreshEpisodes.run());
        cbDot.valueProperty().addListener((obs, oldValue, selected) -> {
            boolean existingEpisode = selected != null && !selected.equals("Đợt điều trị mới");
            cbBS.setDisable(existingEpisode);
            if (existingEpisode) {
                String maDot = selected.split(" \\| ")[0];
                List<Map<String, Object>> episodeDoctor =
                        DB.query("SELECT MaBS FROM DotDieuTri WHERE MaDotDieuTri=?", maDot);
                if (!episodeDoctor.isEmpty()) {
                    String maBS = UI.str(episodeDoctor.get(0).get("MaBS"));
                    cbBS.getItems().stream().filter(item -> item.startsWith(maBS + " - "))
                            .findFirst().ifPresent(cbBS::setValue);
                }
            }
        });
        refreshEpisodes.run();
        VBox body = new VBox(8,
                new Label("Mã LK:"), tfMa, new Label("BN:"), cbBN, new Label("Bác sĩ:"), cbBS,
                new Label("Bệnh:"), cbBenh, tfBenhKhac,
                new Label("Đợt điều trị (tái khám tiếp tục đợt đang mở):"), cbDot,
                new Label("Tình trạng sau lần khám:"), cbTrangThai,
                new Label("Ngày vào (yyyy-MM-dd):"), tfNgay, new Label("Tiền khám:"), tfTien);
        body.setPadding(new Insets(12));
        d.getDialogPane().setContent(body);
        d.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.OK) {
                if (cbBN.getValue() == null || cbBS.getValue() == null
                        || cbBenh.getValue() == null || cbDot.getValue() == null) {
                    UI.error("Vui lòng chọn đầy đủ bệnh nhân, bác sĩ, bệnh và đợt điều trị!");
                    return;
                }
                String tenBenh = BENH_KHAC.equals(cbBenh.getValue())
                        ? tfBenhKhac.getText().trim() : cbBenh.getValue();
                if (tenBenh.isBlank()) {
                    UI.error("Vui lòng chọn bệnh hoặc nhập tên bệnh!");
                    return;
                }
                String maBenh = layHoacTaoMaBenh(tenBenh);
                if (maBenh == null) {
                    UI.error("Không thể lưu bệnh vào danh mục!");
                    return;
                }
                long tien;
                LocalDate ngayVao;
                try {
                    tien = Long.parseLong(tfTien.getText().trim());
                    ngayVao = LocalDate.parse(tfNgay.getText().trim());
                    if (tien < 0) throw new IllegalArgumentException();
                } catch (RuntimeException ex) {
                    UI.error("Ngày vào hoặc tiền khám không hợp lệ!");
                    return;
                }

                String maBN = cbBN.getValue().split(" - ")[0];
                String maBS = cbBS.getValue().split(" - ")[0];
                String maLK = tfMa.getText().trim();
                String trangThai = "Đã khỏi".equals(cbTrangThai.getValue()) ? "DaKhoi" : "DangDieuTri";
                String selectedEpisode = cbDot.getValue();
                boolean newEpisode = selectedEpisode == null || selectedEpisode.equals("Đợt điều trị mới");
                String maDot = newEpisode ? "DT" + maLK : selectedEpisode.split(" \\| ")[0];
                java.sql.Date ngayKetThuc = "DaKhoi".equals(trangThai)
                        ? java.sql.Date.valueOf(ngayVao) : null;

                boolean saved = DB.transaction(c -> {
                    if (newEpisode && DB.exec(c, """
                            INSERT INTO DotDieuTri
                                (MaDotDieuTri,MaBN,MaBS,MaBenh,NgayBatDau,NgayKetThuc,TrangThai)
                            VALUES (?,?,?,?,?,?,?)
                            """, maDot, maBN, maBS, maBenh, java.sql.Date.valueOf(ngayVao),
                            ngayKetThuc, trangThai) != 1) return false;
                    if (DB.exec(c, """
                            INSERT INTO LanKham
                                (MaLanKham,MaDotDieuTri,MaBN,MaBS,MaBenh,NgayVao,NgayRa,TongTienKham,TrangThai)
                            VALUES (?,?,?,?,?,?,?,?,?)
                            """, maLK, maDot, maBN, maBS, maBenh, java.sql.Date.valueOf(ngayVao),
                            ngayKetThuc, tien, trangThai) != 1) return false;
                    return newEpisode || DB.exec(c, """
                            UPDATE DotDieuTri
                            SET TrangThai=?, NgayKetThuc=?
                            WHERE MaDotDieuTri=? AND TrangThai='DangDieuTri'
                            """, trangThai, ngayKetThuc, maDot) == 1;
                });
                if (!saved) {
                    UI.error("Lỗi lưu lần khám; vui lòng kiểm tra mã lần khám và đợt điều trị!");
                    return;
                }
                UI.alert("Tạo lần khám thành công!");
                reload.run();
            }
        });
    }

    private static String layHoacTaoMaBenh(String tenBenh) {
        List<Map<String, Object>> existing = DB.query(
                "SELECT MaBenh FROM DanhMucBenh WHERE LOWER(TRIM(TenBenh))=LOWER(TRIM(?)) LIMIT 1",
                tenBenh);
        if (!existing.isEmpty()) return UI.str(existing.get(0).get("MaBenh"));

        List<Map<String, Object>> last = DB.query("""
                SELECT COALESCE(MAX(CAST(SUBSTRING(MaBenh,2) AS UNSIGNED)),0) AS maxCode
                FROM DanhMucBenh WHERE MaBenh REGEXP '^B[0-9]+$'
                """);
        String maBenh = String.format(Locale.ROOT, "B%03d",
                UI.toLong(last.get(0).get("maxCode")) + 1);
        return DB.exec("INSERT INTO DanhMucBenh (MaBenh,TenBenh) VALUES (?,?)", maBenh, tenBenh)
                ? maBenh : null;
    }
}