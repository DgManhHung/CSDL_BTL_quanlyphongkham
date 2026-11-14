package com.phongkham.tv1_bacsi;

import com.phongkham.shared.DB;
import com.phongkham.shared.UI;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * ========== TV1: BÁC SĨ & TÍNH LƯƠNG ==========
 * Bảng BacSi: ID (auto), MaBS (BSxxx, sinh từ ID), CMT, HoTen, GioiTinh, NgaySinh, SDT, Email,
 *             DiaChi, NgayVaoLam, NgayNghiViec, TrinhDo, ChuyenMon (NOT NULL), TrangThai.
 *
 * Danh sách: Mã, Họ tên, Giới tính, Thâm niên, Trình độ, Chuyên môn, Trạng thái.
 *   - Tìm kiếm: chỉ theo 3 số cuối của mã BS (vd 001 -> BS001).
 *   - Lọc: trình độ / chuyên môn / trạng thái (kết hợp được).
 *   - Sắp xếp: Đang làm việc trước, Đã nghỉ việc sau; cùng trạng thái thì theo mã tăng dần.
 *   - Tổng nhân sự: ghi rõ số người đang làm việc.
 *   - Không có nút Xóa; nút "Xem chi tiết / Sửa hồ sơ" mở hồ sơ đầy đủ để xem và sửa.
 * Thêm: không nhập ngày nghỉ việc, TrangThai = 'Đang làm việc'.
 * Sửa : điền Ngày nghỉ việc -> TrangThai tự thành 'Đã nghỉ việc' (xóa ngày -> 'Đang làm việc').
 * Lương (theo tháng, khớp truy vấn 2a trong database.sql):
 *   7.000.000 + 1.000.000 × số đợt điều trị đã khỏi (V_DotDieuTri, TrangThai='DaKhoi',
 *   NgayKetThuc trong tháng). Chỉ tính bác sĩ có làm việc trong tháng đó.
 *
 * Gọi từ MainShell: BacSiView.luongBacSi()
 */
public class BacSiView {

    private static final long LUONG_CB = 7_000_000L;
    private static final long THUONG_MOI_CA = 1_000_000L;

    private static final String DANG_LAM = "Đang làm việc";
    private static final String DA_NGHI = "Đã nghỉ việc";

    private static final List<String> TRINH_DO = List.of(
            "Bác sĩ đa khoa", "Bác sĩ nội trú", "Bác sĩ Chuyên khoa I",
            "Bác sĩ Chuyên Khoa II", "Thạc sĩ", "Tiến sĩ");

    private static final List<String> CHUYEN_MON = List.of(
            "Nội khoa", "Nhi khoa", "Ngoại khoa", "Tim mạch", "Da liễu",
            "Tai Mũi Họng", "Mắt", "Răng Hàm Mặt", "Sản phụ khoa", "Thần kinh",
            "Cơ xương khớp", "Hô hấp", "Tiêu hóa", "Nội tiết", "Thận - Tiết niệu",
            "Ung bướu", "Truyền nhiễm", "Tâm thần", "Chẩn đoán hình ảnh", "Hồi sức cấp cứu");

    /** Dữ liệu hồ sơ bác sĩ dùng cho thêm / sửa. */
    private record HoSo(String cmt, String hoTen, String gioiTinh, Object ngaySinh, String sdt,
                        String email, String diaChi, Object ngayVao, String trinhDo, String chuyenMon) {}

    /** Màn hình chính của TV1: 2 tab (Danh sách / Tính lương). */
    public static Node luongBacSi() {
        VBox root = new VBox(10);
        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Label staffCount = UI.badge("0 nhân sự");

        Tab tabDs = new Tab("Danh sách", danhSachBS(staffCount));
        Tab tabLuong = new Tab("Tính lương", tinhLuong());
        tabs.getTabs().addAll(tabDs, tabLuong);

        // Mỗi lần chuyển sang tab Tính lương thì nạp lại (trạng thái bác sĩ có thể vừa được sửa ở tab Danh sách)
        tabLuong.setOnSelectionChanged(e -> {
            if (tabLuong.isSelected() && tabLuong.getContent() != null
                    && tabLuong.getContent().getUserData() instanceof Runnable reload) reload.run();
        });

        root.getChildren().addAll(UI.pageHeading("Bác sĩ", staffCount), tabs);
        VBox.setVgrow(tabs, Priority.ALWAYS);
        return root;
    }

    // =====================================================================
    //  TAB 1: DANH SÁCH BÁC SĨ
    // =====================================================================
    public static Node danhSachBS() {
        Label staffCount = UI.badge("Tổng: 0 nhân sự (đang làm việc)");
        VBox root = danhSachBS(staffCount);
        root.getChildren().add(0, UI.pageHeading("Bác sĩ", staffCount));
        return root;
    }

    private static VBox danhSachBS(Label staffCount) {
        VBox root = new VBox(10);

        // --- Tìm kiếm: chỉ theo 3 số cuối của mã ---
        TextField search = new TextField();
        search.setPromptText("⌕  3 số cuối của mã (VD: 001)");
        search.setPrefWidth(220);
        search.setTextFormatter(new TextFormatter<String>(ch ->
                ch.getControlNewText().matches("\\d{0,3}") ? ch : null));

        // --- Lọc: trình độ / chuyên môn / trạng thái ---
        final String ALL_TD = "-- Trình độ --", ALL_CM = "-- Chuyên môn --", ALL_TT = "-- Trạng thái --";
        ComboBox<String> cbTrinhDo = new ComboBox<>(FXCollections.observableArrayList(TRINH_DO));
        cbTrinhDo.getItems().add(0, ALL_TD);
        cbTrinhDo.setValue(ALL_TD);

        Set<String> cmSet = new LinkedHashSet<>(CHUYEN_MON);
        for (Map<String, Object> m : DB.query(
                "SELECT DISTINCT ChuyenMon FROM BacSi ORDER BY ChuyenMon"))
            cmSet.add(UI.str(m.get("ChuyenMon")));
        ComboBox<String> cbChuyenMon = new ComboBox<>(FXCollections.observableArrayList(cmSet));
        cbChuyenMon.getItems().add(0, ALL_CM);
        cbChuyenMon.setValue(ALL_CM);

        ComboBox<String> cbTrangThai = new ComboBox<>(
                FXCollections.observableArrayList(ALL_TT, DANG_LAM, DA_NGHI));
        cbTrangThai.setValue(ALL_TT);

        TableView<Map<String, Object>> table = UI.table(
                "MaBS:Mã BS:80", "HoTen:Họ tên:150", "GioiTinh:Giới tính:80",
                "ThamNien:Thâm niên (năm):110", "TrinhDo:Trình độ:160",
                "ChuyenMon:Chuyên môn:120", "TrangThai:Trạng thái:110");

        Runnable load = () -> {
            StringBuilder sql = new StringBuilder("""
                    SELECT MaBS, HoTen, GioiTinh,
                           TIMESTAMPDIFF(YEAR,NgayVaoLam,COALESCE(NgayNghiViec,CURDATE())) AS ThamNien,
                           TrinhDo, ChuyenMon, TrangThai
                    FROM BacSi WHERE 1=1
                    """);
            List<Object> p = new ArrayList<>();
            String kw = search.getText().trim();
            if (!kw.isEmpty()) {
                sql.append(" AND RIGHT(MaBS,3)=?");
                p.add(String.format("%3s", kw).replace(' ', '0'));   // "7" -> "007"
            }
            if (!ALL_TD.equals(cbTrinhDo.getValue())) { sql.append(" AND TrinhDo=?"); p.add(cbTrinhDo.getValue()); }
            if (!ALL_CM.equals(cbChuyenMon.getValue())) { sql.append(" AND ChuyenMon=?"); p.add(cbChuyenMon.getValue()); }
            if (!ALL_TT.equals(cbTrangThai.getValue())) { sql.append(" AND TrangThai=?"); p.add(cbTrangThai.getValue()); }
            // Đang làm việc trước, đã nghỉ việc sau; cùng trạng thái thì theo mã tăng dần
            sql.append(" ORDER BY CASE WHEN TrangThai='").append(DANG_LAM).append("' THEN 0 ELSE 1 END, MaBS");
            UI.loadTable(table, DB.query(sql.toString(), p.toArray()));

            long dangLam = UI.toLong(DB.query(
                    "SELECT COUNT(*) AS n FROM BacSi WHERE TrangThai=?", DANG_LAM).get(0).get("n"));
            staffCount.setText(dangLam + " nhân sự");
        };
        search.setOnAction(e -> load.run());
        search.textProperty().addListener((o, a, b) -> { if (b.isEmpty() || b.length() == 3) load.run(); });
        cbTrinhDo.setOnAction(e -> load.run());
        cbChuyenMon.setOnAction(e -> load.run());
        cbTrangThai.setOnAction(e -> load.run());
        load.run();

        Button btnSearch = new Button("⚲");
        btnSearch.getStyleClass().add("filter-button");
        btnSearch.setAccessibleText("Tìm bác sĩ theo 3 số cuối của mã");
        btnSearch.setOnAction(e -> load.run());
        Button btnAdd = UI.teal("+ Thêm Bác sĩ");
        btnAdd.setOnAction(e -> formBS(null, load));
        Button btnDetail = new Button("✎  Xem chi tiết / Sửa hồ sơ");
        btnDetail.getStyleClass().add("secondary-button");
        btnDetail.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn bác sĩ cần xem!"); return; }
            List<Map<String, Object>> full = DB.query("SELECT * FROM BacSi WHERE MaBS=?", UI.str(sel.get("MaBS")));
            if (full.isEmpty()) { UI.error("Không tìm thấy hồ sơ bác sĩ!"); return; }
            formBS(full.get(0), load);
        });

        // 1. Khóa kích thước tối thiểu để các nút không bao giờ bị co lại thành "..."
        btnAdd.setMinWidth(Region.USE_PREF_SIZE);
        btnDetail.setMinWidth(Region.USE_PREF_SIZE);
        btnSearch.setMinWidth(Region.USE_PREF_SIZE);

        // 2. Hàng trên: Tận dụng luôn UI.toolbar() để tự động đẩy 2 nút sang sát lề phải
        HBox actionRow = UI.toolbar(btnAdd, btnDetail);

        // 3. Hàng dưới: Cho ô tìm kiếm và 3 ComboBox tự động giãn đều hết chiều ngang
        search.setMaxWidth(Double.MAX_VALUE);
        cbTrinhDo.setMaxWidth(Double.MAX_VALUE);
        cbChuyenMon.setMaxWidth(Double.MAX_VALUE);
        cbTrangThai.setMaxWidth(Double.MAX_VALUE);

        HBox.setHgrow(search, Priority.ALWAYS);
        HBox.setHgrow(cbTrinhDo, Priority.ALWAYS);
        HBox.setHgrow(cbChuyenMon, Priority.ALWAYS);
        HBox.setHgrow(cbTrangThai, Priority.ALWAYS);

        HBox filterRow = new HBox(8, search, btnSearch, cbTrinhDo, cbChuyenMon, cbTrangThai);
        filterRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        filterRow.getStyleClass().add("toolbar");

        // 4. Đưa 2 hàng công cụ và bảng vào giao diện
        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(actionRow, filterRow, tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        return root;
    }

    /** Form thêm (old == null) hoặc xem chi tiết / sửa hồ sơ (old = 1 dòng đầy đủ của BacSi). */
    private static void formBS(Map<String, Object> old, Runnable reload) {
        boolean edit = old != null;
        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle(edit ? "Hồ sơ bác sĩ" : "Thêm Bác sĩ");
        d.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        ((Button) d.getDialogPane().lookupButton(ButtonType.OK)).setText(edit ? "Lưu thay đổi" : "Thêm");
        ((Button) d.getDialogPane().lookupButton(ButtonType.CANCEL)).setText(edit ? "Đóng" : "Hủy");

        String maBSCu = edit ? UI.str(old.get("MaBS")) : "";

        TextField tfMa = new TextField(edit ? maBSCu : "(Tự sinh khi lưu)");
        tfMa.setDisable(true);
        TextField tfCMT = new TextField(edit ? UI.str(old.get("CMT")) : "");
        TextField tfHoTen = new TextField(edit ? UI.str(old.get("HoTen")) : "");
        ComboBox<String> cbGioiTinh = new ComboBox<>(FXCollections.observableArrayList("Nam", "Nữ"));
        cbGioiTinh.setValue(edit ? UI.str(old.get("GioiTinh")) : "Nam");
        DatePicker dpNgaySinh = new DatePicker(edit ? parseDate(old.get("NgaySinh")) : null);
        TextField tfSDT = new TextField(edit ? UI.str(old.get("SDT")) : "");
        TextField tfEmail = new TextField(edit ? UI.str(old.get("Email")) : "");
        TextField tfDiaChi = new TextField(edit ? UI.str(old.get("DiaChi")) : "");
        DatePicker dpNgayVaoLam = new DatePicker(edit ? parseDate(old.get("NgayVaoLam")) : LocalDate.now());
        DatePicker dpNgayNghi = new DatePicker(edit ? parseDate(old.get("NgayNghiViec")) : null);

        TextField tfThamNien = new TextField();
        tfThamNien.setEditable(false);
        TextField tfTrangThai = new TextField();
        tfTrangThai.setEditable(false);
        Runnable refreshAuto = () -> {
            LocalDate vao = dpNgayVaoLam.getValue();
            LocalDate nghi = edit ? dpNgayNghi.getValue() : null;
            tfThamNien.setText(tinhThamNien(vao, nghi) + " năm");
            tfTrangThai.setText(nghi != null ? DA_NGHI : DANG_LAM);
        };
        dpNgayVaoLam.setOnAction(e -> refreshAuto.run());
        dpNgayNghi.setOnAction(e -> refreshAuto.run());
        refreshAuto.run();

        ComboBox<String> cbTrinhDo = new ComboBox<>(FXCollections.observableArrayList(TRINH_DO));
        ComboBox<String> cbChuyenMon = new ComboBox<>(FXCollections.observableArrayList(CHUYEN_MON));
        if (edit) addLegacyChoice(cbChuyenMon, UI.str(old.get("ChuyenMon")));
        cbTrinhDo.setValue(edit ? UI.str(old.get("TrinhDo")) : TRINH_DO.get(0));
        cbChuyenMon.setValue(edit ? UI.str(old.get("ChuyenMon")) : CHUYEN_MON.get(0));

        GridPane g = new GridPane();
        g.setHgap(10); g.setVgap(8); g.setPadding(new Insets(12));
        int r = 0;
        g.addRow(r++, new Label("Mã BS:"), tfMa);
        g.addRow(r++, new Label("CMT *:"), tfCMT);
        g.addRow(r++, new Label("Họ tên *:"), tfHoTen);
        g.addRow(r++, new Label("Giới tính *:"), cbGioiTinh);
        g.addRow(r++, new Label("Ngày sinh *:"), dpNgaySinh);
        g.addRow(r++, new Label("SĐT *:"), tfSDT);
        g.addRow(r++, new Label("Email *:"), tfEmail);
        g.addRow(r++, new Label("Địa chỉ *:"), tfDiaChi);
        g.addRow(r++, new Label("Ngày vào làm *:"), dpNgayVaoLam);
        if (edit) g.addRow(r++, new Label("Ngày nghỉ việc:"), dpNgayNghi);
        g.addRow(r++, new Label("Thâm niên:"), tfThamNien);
        g.addRow(r++, new Label("Trạng thái:"), tfTrangThai);
        g.addRow(r++, new Label("Trình độ *:"), cbTrinhDo);
        g.addRow(r++, new Label("Chuyên môn *:"), cbChuyenMon);
        d.getDialogPane().setContent(g);

        // Kiểm tra dữ liệu trước khi đóng dialog
        Button ok = (Button) d.getDialogPane().lookupButton(ButtonType.OK);
        ok.addEventFilter(javafx.event.ActionEvent.ACTION, ev -> {
            String err = validate(tfCMT, tfHoTen, dpNgaySinh, tfSDT, tfEmail, tfDiaChi,
                    dpNgayVaoLam, edit ? dpNgayNghi : null, cbChuyenMon);
            if (err == null) err = kiemTraTrung(tfCMT.getText().trim(), tfSDT.getText().trim(),
                    tfEmail.getText().trim(), maBSCu);
            if (err != null) { UI.error(err); ev.consume(); }
        });

        d.showAndWait().ifPresent(bt -> {
            if (bt != ButtonType.OK) return;
            HoSo h = new HoSo(tfCMT.getText().trim(), tfHoTen.getText().trim(), cbGioiTinh.getValue(),
                    toSqlDate(dpNgaySinh.getValue()), tfSDT.getText().trim(), tfEmail.getText().trim(),
                    tfDiaChi.getText().trim(), toSqlDate(dpNgayVaoLam.getValue()),
                    cbTrinhDo.getValue(), cbChuyenMon.getValue());
            if (edit) {
                LocalDate nghi = dpNgayNghi.getValue();
                boolean okDb = DB.exec("""
                        UPDATE BacSi SET CMT=?, HoTen=?, GioiTinh=?, NgaySinh=?, SDT=?, Email=?, DiaChi=?,
                               NgayVaoLam=?, NgayNghiViec=?, TrinhDo=?, ChuyenMon=?, TrangThai=?
                        WHERE MaBS=?""",
                        h.cmt(), h.hoTen(), h.gioiTinh(), h.ngaySinh(), h.sdt(), h.email(), h.diaChi(),
                        h.ngayVao(), toSqlDate(nghi), h.trinhDo(), h.chuyenMon(),
                        nghi != null ? DA_NGHI : DANG_LAM, maBSCu);
                if (okDb) { UI.alert("Cập nhật hồ sơ thành công!"); reload.run(); }
                else UI.error("Lỗi lưu dữ liệu!");
            } else {
                String maMoi = themBS(h);
                if (maMoi != null) { UI.alert("Thêm Bác sĩ thành công! Mã: " + maMoi); reload.run(); }
                else UI.error("Lỗi thêm bác sĩ! (có thể đã vượt quá BS999, xem log để biết chi tiết)");
            }
        });
    }

    /**
     * Thêm bác sĩ: INSERT (MaBS để NULL) -> lấy ID tự tăng -> UPDATE MaBS = "BS" + ID (3 chữ số).
     * Làm trong 1 transaction. Trả về MaBS mới, hoặc null nếu lỗi.
     */
    private static String themBS(HoSo h) {
        try (Connection c = DB.conn()) {
            c.setAutoCommit(false);
            try {
                int id;
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO BacSi (CMT,HoTen,GioiTinh,NgaySinh,SDT,Email,DiaChi,NgayVaoLam,TrinhDo,ChuyenMon)
                        VALUES (?,?,?,?,?,?,?,?,?,?)""", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, h.cmt());
                    ps.setString(2, h.hoTen());
                    ps.setString(3, h.gioiTinh());
                    ps.setObject(4, h.ngaySinh());
                    ps.setString(5, h.sdt());
                    ps.setString(6, h.email());
                    ps.setString(7, h.diaChi());
                    ps.setObject(8, h.ngayVao());
                    ps.setString(9, h.trinhDo());
                    ps.setString(10, h.chuyenMon());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        if (!rs.next()) throw new SQLException("Không lấy được ID");
                        id = rs.getInt(1);
                    }
                }
                if (id > 999) throw new SQLException("MaBS VARCHAR(5) chỉ chứa tới BS999");
                String ma = String.format("BS%03d", id);
                try (PreparedStatement up = c.prepareStatement("UPDATE BacSi SET MaBS=? WHERE ID=?")) {
                    up.setString(1, ma);
                    up.setInt(2, id);
                    up.executeUpdate();
                }
                c.commit();
                return ma;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String validate(TextField cmt, TextField hoTen, DatePicker ngaySinh, TextField sdt,
                                   TextField email, TextField diaChi, DatePicker ngayVao, DatePicker ngayNghi,
                                   ComboBox<String> chuyenMon) {
        String c = cmt.getText().trim();
        if (c.isEmpty()) return "CMT không được để trống!";
        if (!c.matches("\\d{9}|\\d{12}")) return "CMT phải gồm 9 hoặc 12 chữ số!";
        if (hoTen.getText().isBlank()) return "Họ tên không được để trống!";
        LocalDate today = LocalDate.now();
        LocalDate ns = ngaySinh.getValue(), vao = ngayVao.getValue();
        if (ns == null) return "Vui lòng chọn ngày sinh!";
        if (!ns.isBefore(today)) return "Ngày sinh phải trước hôm nay!";
        String s = sdt.getText().trim();
        if (s.isEmpty()) return "SĐT không được để trống!";
        if (!s.matches("0\\d{9}")) return "SĐT phải gồm 10 chữ số và bắt đầu bằng 0!";
        String em = email.getText().trim();
        if (em.isEmpty()) return "Email không được để trống!";
        if (em.length() > 100 || !em.matches("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+")) return "Email không hợp lệ!";
        if (diaChi.getText().isBlank()) return "Địa chỉ không được để trống!";
        if (chuyenMon.getValue() == null || chuyenMon.getValue().isBlank()) return "Vui lòng chọn chuyên môn!";
        if (vao == null) return "Vui lòng chọn ngày vào làm!";
        if (vao.isAfter(today)) return "Ngày vào làm không được ở tương lai!";
        if (!ns.isBefore(vao)) return "Ngày vào làm phải sau ngày sinh!";
        if (ngayNghi != null && ngayNghi.getValue() != null) {
            LocalDate nghi = ngayNghi.getValue();
            if (nghi.isBefore(vao)) return "Ngày nghỉ việc không được trước ngày vào làm!";
            if (nghi.isAfter(today)) return "Ngày nghỉ việc không được ở tương lai!";
        }
        return null;
    }

    /** CMT, SĐT, Email đều UNIQUE: báo rõ trường nào bị trùng (loại trừ chính bác sĩ đang sửa). */
    private static String kiemTraTrung(String cmt, String sdt, String email, String maBSCu) {
        List<Map<String, Object>> rows = DB.query(
                "SELECT CMT, SDT, Email FROM BacSi WHERE (CMT=? OR SDT=? OR Email=?) AND (MaBS IS NULL OR MaBS<>?)",
                cmt, sdt, email, maBSCu);
        for (Map<String, Object> r : rows) {
            if (cmt.equals(UI.str(r.get("CMT")))) return "CMT này đã tồn tại!";
            if (sdt.equals(UI.str(r.get("SDT")))) return "SĐT này đã tồn tại!";
            if (email.equalsIgnoreCase(UI.str(r.get("Email")))) return "Email này đã tồn tại!";
        }
        return null;
    }

    private static LocalDate parseDate(Object o) {
        String s = UI.str(o).trim();
        return s.length() >= 10 ? LocalDate.parse(s.substring(0, 10)) : null;
    }

    private static Object toSqlDate(LocalDate d) {
        return d == null ? null : java.sql.Date.valueOf(d);
    }

    private static long tinhThamNien(LocalDate vao, LocalDate nghi) {
        if (vao == null) return 0;
        LocalDate den = nghi != null ? nghi : LocalDate.now();
        return Math.max(0, ChronoUnit.YEARS.between(vao, den));
    }

    private static void addLegacyChoice(ComboBox<String> choices, String value) {
        if (!value.isBlank() && !choices.getItems().contains(value)) choices.getItems().add(value);
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

        VBox c1 = UI.card("Bác sĩ làm việc trong tháng", "0", "#0d9488");
        VBox c2 = UI.card("Đợt điều trị đã khỏi", "0", "#0284c7");
        VBox c3 = UI.card("Tổng chi lương", "0", "#7c3aed");
        HBox cards = new HBox(16, c1, c2, c3);

        TableView<Map<String, Object>> table = UI.table(
                "MaBS:Mã BS:70", "HoTen:Họ tên:150", "ChuyenMon:Chuyên môn:110",
                "TrangThai:Trạng thái:110", "SoCaKhoi:Số đợt khỏi:90", "PhuCap:Thưởng:110",
                "TongLuong:Tổng lương:130");

        Runnable load = () -> {
            LocalDate dauThang = LocalDate.of(cbN.getValue(), cbT.getValue(), 1);
            LocalDate dauThangSau = dauThang.plusMonths(1);
            LocalDate cuoiThang = dauThangSau.minusDays(1);
            // Giống truy vấn 2a trong database.sql: mỗi đợt điều trị đã khỏi kết thúc trong tháng = 1 lượt
            List<Map<String, Object>> data = DB.query("""
                SELECT b.MaBS, b.HoTen, b.ChuyenMon, b.TrangThai,
                       TIMESTAMPDIFF(YEAR,b.NgayVaoLam,COALESCE(b.NgayNghiViec,CURDATE())) AS ThamNien,
                       7000000 AS LuongCB,
                       COUNT(d.MaDotDieuTri) AS SoCaKhoi,
                       COUNT(d.MaDotDieuTri)*1000000 AS PhuCap,
                       7000000 + COUNT(d.MaDotDieuTri)*1000000 AS TongLuong
                FROM BacSi b
                LEFT JOIN V_DotDieuTri d
                       ON d.MaBS=b.MaBS AND d.TrangThai='DaKhoi'
                      AND d.NgayKetThuc >= ? AND d.NgayKetThuc < ?
                WHERE b.NgayVaoLam <= ? AND (b.NgayNghiViec IS NULL OR b.NgayNghiViec >= ?)
                GROUP BY b.ID, b.MaBS, b.HoTen, b.ChuyenMon, b.TrangThai, b.NgayVaoLam, b.NgayNghiViec
                ORDER BY b.MaBS
                """, java.sql.Date.valueOf(dauThang), java.sql.Date.valueOf(dauThangSau),
                    java.sql.Date.valueOf(cuoiThang), java.sql.Date.valueOf(dauThang));
            UI.loadTable(table, data);
            long ca = data.stream().mapToLong(m -> UI.toLong(m.get("SoCaKhoi"))).sum();
            long luong = data.stream().mapToLong(m -> UI.toLong(m.get("TongLuong"))).sum();
            UI.setCardValue(c1, data.size() + " người");
            UI.setCardValue(c2, ca + " đợt");
            UI.setCardValue(c3, UI.MONEY.format(luong) + " đ");
        };
        cbT.setOnAction(e -> load.run());
        cbN.setOnAction(e -> load.run());
        load.run();

        Button btnCT = UI.teal("Xem chi tiết đợt khỏi");
        btnCT.setOnAction(e -> {
            Map<String, Object> sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) { UI.alert("Chọn bác sĩ cần xem!"); return; }
            chiTietDotKhoi(UI.str(sel.get("MaBS")), UI.str(sel.get("HoTen")), cbT.getValue(), cbN.getValue());
        });

        Label note = new Label("Lương = " + UI.MONEY.format(LUONG_CB) + " (lương cơ bản) + " + UI.MONEY.format(THUONG_MOI_CA)
                + " × số đợt điều trị đã khỏi trong tháng (chỉ tính bác sĩ có làm việc trong tháng)");
        note.setTextFill(javafx.scene.paint.Color.GRAY);

        VBox tablePanel = UI.tablePanel(table);
        root.getChildren().addAll(
                UI.toolbar(new Label("Tháng:"), cbT, new Label("/"), cbN, btnCT),
                note, cards, tablePanel);
        VBox.setVgrow(tablePanel, Priority.ALWAYS);
        root.setUserData(load);   // để tab gọi nạp lại khi được chọn
        return root;
    }

    private static void chiTietDotKhoi(String maBS, String tenBS, int thang, int nam) {
        LocalDate dau = LocalDate.of(nam, thang, 1);
        List<Map<String, Object>> data = DB.query("""
            SELECT MaDotDieuTri, MaBN, TenBN, TenBenh, NgayBatDau, NgayKetThuc, SoLanKham
            FROM V_DotDieuTri
            WHERE MaBS=? AND TrangThai='DaKhoi' AND NgayKetThuc >= ? AND NgayKetThuc < ?
            ORDER BY NgayKetThuc, MaDotDieuTri
            """, maBS, java.sql.Date.valueOf(dau), java.sql.Date.valueOf(dau.plusMonths(1)));

        TableView<Map<String, Object>> t = UI.table(
                "MaDotDieuTri:Mã đợt:90", "TenBN:Bệnh nhân:140", "TenBenh:Bệnh:120",
                "NgayBatDau:Bắt đầu:95", "NgayKetThuc:Kết thúc:95", "SoLanKham:Số lần khám:90");
        UI.loadTable(t, data);

        Dialog<ButtonType> d = new Dialog<>();
        d.setTitle("Chi tiết đợt điều trị đã khỏi");
        d.setHeaderText(tenBS + " (" + maBS + ") – tháng " + thang + "/" + nam
                + " – " + data.size() + " đợt khỏi");
        d.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        t.setPrefSize(640, 300);
        d.getDialogPane().setContent(t);
        d.showAndWait();
    }
}