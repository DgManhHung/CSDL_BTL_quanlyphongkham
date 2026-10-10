package com.phongkham.ui;

import com.phongkham.dao.HoaDonDAO;
import com.phongkham.model.HoaDon;
import com.phongkham.util.ExcelExporter;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class HoaDonView {
    private final VBox root = new VBox(16);
    private final HoaDonDAO dao = new HoaDonDAO();
    private final String username;
    private final NumberFormat money = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final TableView<HoaDon> table = new TableView<>();
    private final ComboBox<String> cbFilter = new ComboBox<>();

    public HoaDonView(String username) {
        this.username = username;
        build();
        loadData();
    }

    public VBox getRoot() { return root; }

    private void build() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("QUẢN LÝ HÓA ĐƠN");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#1e293b"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        cbFilter.getItems().addAll("Tất cả", "ChuaThanhToan", "DaThanhToan");
        cbFilter.setValue("Tất cả");
        cbFilter.setOnAction(e -> loadData());

        Button btnTao = new Button("+ Tạo hóa đơn");
        btnTao.setStyle("-fx-background-color: #0d9488; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        btnTao.setOnAction(e -> showTaoDialog());

        Button btnXuatExcel = new Button("Xuất Excel");
        btnXuatExcel.setStyle("-fx-background-color: #15803d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        btnXuatExcel.setOnAction(e -> xuatExcel());

        header.getChildren().addAll(title, spacer, new Label("Lọc:"), cbFilter, btnXuatExcel, btnTao);

        // Table
        TableColumn<HoaDon, String> c1 = col("Mã HĐ", h -> h.getMaHoaDon(), 90);
        TableColumn<HoaDon, String> c2 = col("Bệnh nhân", h -> h.getTenBenhNhan(), 150);
        TableColumn<HoaDon, String> c3 = col("SĐT", h -> h.getSdt(), 110);
        TableColumn<HoaDon, String> c4 = col("Ngày lập", h -> h.getNgayLap() != null ? h.getNgayLap().format(dtf) : "", 130);
        TableColumn<HoaDon, String> c5 = col("Tiền khám", h -> money.format(h.getTienKham()), 100);
        TableColumn<HoaDon, String> c6 = col("Tiền thuốc", h -> money.format(h.getTienThuoc()), 100);
        TableColumn<HoaDon, String> c7 = col("Tổng tiền", h -> money.format(h.getTongTien()) + " đ", 120);
        TableColumn<HoaDon, String> c8 = col("Trạng thái", h -> {
            if ("DaThanhToan".equals(h.getTrangThai())) return "Đã TT";
            if ("ChuaThanhToan".equals(h.getTrangThai())) return "Chưa TT";
            return h.getTrangThai();
        }, 90);

        TableColumn<HoaDon, Void> cAct = new TableColumn<>("Thao tác");
        cAct.setPrefWidth(160);
        cAct.setCellFactory(col -> new TableCell<>() {
            private final Button btnTT = new Button("Thanh toán");
            private final Button btnXoa = new Button("Xóa");
            private final HBox box = new HBox(6, btnTT, btnXoa);
            {
                btnTT.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 11;");
                btnTT.setOnAction(e -> {
                    HoaDon hd = getTableView().getItems().get(getIndex());
                    showThanhToanDialog(hd);
                });
                btnXoa.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 4; -fx-cursor: hand; -fx-font-size: 11;");
                btnXoa.setOnAction(e -> {
                    HoaDon hd = getTableView().getItems().get(getIndex());
                    xoaHoaDon(hd);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HoaDon hd = getTableView().getItems().get(getIndex());
                    btnTT.setDisable(!"ChuaThanhToan".equals(hd.getTrangThai()));
                    setGraphic(box);
                }
            }
        });

        table.getColumns().addAll(c1, c2, c3, c4, c5, c6, c7, c8, cAct);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        VBox.setVgrow(table, Priority.ALWAYS);

        root.getChildren().addAll(header, table);
    }

    private TableColumn<HoaDon, String> col(String title, java.util.function.Function<HoaDon, String> fn, double w) {
        TableColumn<HoaDon, String> c = new TableColumn<>(title);
        c.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(fn.apply(d.getValue())));
        c.setPrefWidth(w);
        return c;
    }

    private void loadData() {
        String f = cbFilter.getValue();
        List<HoaDon> list = "Tất cả".equals(f) ? dao.getAll() : dao.getByTrangThai(f);
        table.setItems(FXCollections.observableArrayList(list));
    }

    private void showTaoDialog() {
        List<Map<String, Object>> lks = dao.getLanKhamChuaHD();
        if (lks.isEmpty()) {
            alert(Alert.AlertType.INFORMATION, "Không còn lần khám nào cần lập hóa đơn.");
            return;
        }
        Dialog<String> dlg = new Dialog<>();
        dlg.setTitle("Tạo hóa đơn");
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        ComboBox<String> cb = new ComboBox<>();
        for (Map<String, Object> m : lks) {
            cb.getItems().add(m.get("MaLanKham") + " | " + m.get("HoTen") + " | " + m.get("TenBenh")
                    + " | " + money.format(m.get("TongTienKham")) + "đ");
        }
        cb.getSelectionModel().selectFirst();
        VBox body = new VBox(10, new Label("Chọn lần khám:"), cb);
        body.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(body);

        dlg.setResultConverter(bt -> {
            if (bt == ButtonType.OK && cb.getValue() != null) {
                return cb.getValue().split(" \\| ")[0];
            }
            return null;
        });

        Optional<String> res = dlg.showAndWait();
        res.ifPresent(maLK -> {
            if (dao.taoHoaDon(maLK, username)) {
                alert(Alert.AlertType.INFORMATION, "Tạo hóa đơn thành công!");
                loadData();
            } else {
                alert(Alert.AlertType.ERROR, "Tạo hóa đơn thất bại!");
            }
        });
    }

    private void showThanhToanDialog(HoaDon hd) {
        BigDecimal daTT = dao.daThanhToan(hd.getMaHoaDon());
        BigDecimal conLai = hd.getTongTien().subtract(daTT);

        Dialog<ButtonType> dlg = new Dialog<>();
        dlg.setTitle("Thanh toán " + hd.getMaHoaDon());
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Label info = new Label("Tổng: " + money.format(hd.getTongTien()) + " đ\n"
                + "Đã TT: " + money.format(daTT) + " đ\n"
                + "Còn lại: " + money.format(conLai) + " đ");

        TextField tfTien = new TextField(conLai.toPlainString());
        ComboBox<String> cbPT = new ComboBox<>(FXCollections.observableArrayList(
                "TienMat", "ChuyenKhoan", "The", "ViDienTu"));
        cbPT.setValue("TienMat");

        VBox body = new VBox(10,
                info,
                new Label("Số tiền:"), tfTien,
                new Label("Phương thức:"), cbPT);
        body.setPadding(new Insets(16));
        dlg.getDialogPane().setContent(body);

        Optional<ButtonType> res = dlg.showAndWait();
        if (res.isPresent() && res.get() == ButtonType.OK) {
            try {
                BigDecimal st = new BigDecimal(tfTien.getText().trim());
                if (st.compareTo(BigDecimal.ZERO) <= 0 || st.compareTo(conLai) > 0) {
                    alert(Alert.AlertType.WARNING, "Số tiền không hợp lệ!");
                    return;
                }
                if (dao.thanhToan(hd.getMaHoaDon(), st, cbPT.getValue(), username)) {
                    alert(Alert.AlertType.INFORMATION, "Thanh toán thành công!");
                    loadData();
                } else {
                    alert(Alert.AlertType.ERROR, "Thanh toán thất bại!");
                }
            } catch (NumberFormatException ex) {
                alert(Alert.AlertType.WARNING, "Số tiền không hợp lệ!");
            }
        }
    }

    private void xoaHoaDon(HoaDon hd) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa hóa đơn");
        confirm.setHeaderText("Xóa hóa đơn " + hd.getMaHoaDon() + "?");
        confirm.setContentText("Hóa đơn và toàn bộ giao dịch thanh toán liên quan sẽ bị xóa vĩnh viễn.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (dao.xoaHoaDon(hd.getMaHoaDon())) {
                alert(Alert.AlertType.INFORMATION, "Đã xóa hóa đơn " + hd.getMaHoaDon() + ".");
                loadData();
            } else {
                alert(Alert.AlertType.ERROR, "Xóa hóa đơn thất bại. Vui lòng kiểm tra lại dữ liệu.");
            }
        }
    }

    private void xuatExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Xuất danh sách hóa đơn");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName("Danh_sach_hoa_don.xlsx");
        File file = chooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (file == null) return;

        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }

        List<List<Object>> rows = new ArrayList<>();
        rows.add(List.of("Mã hóa đơn", "Bệnh nhân", "Số điện thoại", "Ngày lập",
                "Tiền khám (đ)", "Tiền thuốc (đ)", "Tổng tiền (đ)", "Trạng thái"));
        for (HoaDon hd : table.getItems()) {
            rows.add(List.of(
                    text(hd.getMaHoaDon()),
                    text(hd.getTenBenhNhan()),
                    text(hd.getSdt()),
                    hd.getNgayLap() == null ? "" : hd.getNgayLap().format(dtf),
                    amount(hd.getTienKham()),
                    amount(hd.getTienThuoc()),
                    amount(hd.getTongTien()),
                    trangThai(hd.getTrangThai())
            ));
        }

        Map<String, List<List<Object>>> sheets = new LinkedHashMap<>();
        sheets.put("Danh sách hóa đơn", rows);
        try {
            ExcelExporter.export(file, sheets);
            alert(Alert.AlertType.INFORMATION, "Đã xuất Excel thành công:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
            alert(Alert.AlertType.ERROR, "Không thể xuất file Excel: " + ex.getMessage());
        }
    }

    private String trangThai(String status) {
        if ("DaThanhToan".equals(status)) return "Đã thanh toán";
        if ("ChuaThanhToan".equals(status)) return "Chưa thanh toán";
        return text(status);
    }

    private String text(String value) {
        return value == null ? "" : value;
    }

    private BigDecimal amount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.showAndWait();
    }
}