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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DoanhThuView {
    private final VBox root = new VBox(20);
    private final HoaDonDAO dao = new HoaDonDAO();
    private final NumberFormat money = NumberFormat.getInstance(new Locale("vi", "VN"));
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ComboBox<Integer> cbThang = new ComboBox<>();
    private final ComboBox<Integer> cbNam = new ComboBox<>();
    private final Label lblTongDT = new Label("0");
    private final Label lblTongKham = new Label("0");
    private final Label lblTongThuoc = new Label("0");
    private final Label lblSoHD = new Label("0");
    private final TableView<Map<String, Object>> tableBS = new TableView<>();
    private final TableView<HoaDon> tableHD = new TableView<>();

    public DoanhThuView() {
        build();
        loadData();
    }

    public VBox getRoot() { return root; }

    private void build() {
        root.setPadding(new Insets(0));

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("BÁO CÁO DOANH THU");
        title.setFont(Font.font("System", FontWeight.BOLD, 22));
        title.setTextFill(Color.web("#1e293b"));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        for (int m = 1; m <= 12; m++) cbThang.getItems().add(m);
        cbThang.setValue(LocalDate.now().getMonthValue());
        for (int y = 2024; y <= 2027; y++) cbNam.getItems().add(y);
        cbNam.setValue(LocalDate.now().getYear());
        cbThang.setOnAction(e -> loadData());
        cbNam.setOnAction(e -> loadData());

        Label lblThang = new Label("Chọn tháng:");
        Button btnXuatExcel = new Button("Xuất Excel");
        btnXuatExcel.setStyle("-fx-background-color: #15803d; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        btnXuatExcel.setOnAction(e -> xuatExcel());
        header.getChildren().addAll(title, spacer, lblThang, cbThang, new Label("/"), cbNam, btnXuatExcel);
        header.setSpacing(8);

        // Stat cards
        HBox cards = new HBox(16);
        cards.getChildren().addAll(
            statCard("Tổng doanh thu", lblTongDT, "#0d9488"),
            statCard("Tiền khám", lblTongKham, "#0284c7"),
            statCard("Tiền thuốc", lblTongThuoc, "#7c3aed"),
            statCard("Số hóa đơn", lblSoHD, "#ea580c")
        );

        // Table bác sĩ
        Label tblTitle = new Label("Doanh thu theo Bác sĩ");
        tblTitle.setFont(Font.font("System", FontWeight.BOLD, 15));

        TableColumn<Map<String, Object>, String> colMa = new TableColumn<>("Mã BS");
        colMa.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(str(d.getValue().get("MaBS"))));
        colMa.setPrefWidth(100);

        TableColumn<Map<String, Object>, String> colTen = new TableColumn<>("Họ tên");
        colTen.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(str(d.getValue().get("HoTen"))));
        colTen.setPrefWidth(200);

        TableColumn<Map<String, Object>, String> colSo = new TableColumn<>("Số HĐ");
        colSo.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(str(d.getValue().get("SoHD"))));
        colSo.setPrefWidth(80);

        TableColumn<Map<String, Object>, String> colDT = new TableColumn<>("Tổng doanh thu");
        colDT.setCellValueFactory(d -> {
            Object v = d.getValue().get("TongDT");
            String s = v instanceof BigDecimal bd ? money.format(bd) + " đ" : str(v);
            return new javafx.beans.property.SimpleStringProperty(s);
        });
        colDT.setPrefWidth(180);

        tableBS.getColumns().addAll(colMa, colTen, colSo, colDT);
        tableBS.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableBS.setPrefHeight(180);
        tableBS.setMinHeight(120);

        Label hdTitle = new Label("Hóa đơn trong tháng");
        hdTitle.setFont(Font.font("System", FontWeight.BOLD, 15));
        Button btnXoaDoanhThu = new Button("Xóa doanh thu đã chọn");
        btnXoaDoanhThu.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand; -fx-padding: 8 16;");
        btnXoaDoanhThu.setOnAction(e -> xoaDoanhThu());
        HBox hdToolbar = new HBox(10, hdTitle, btnXoaDoanhThu);
        hdToolbar.setAlignment(Pos.CENTER_LEFT);

        TableColumn<HoaDon, String> colMaHD = hoaDonCol("Mã HĐ", HoaDon::getMaHoaDon, 90);
        TableColumn<HoaDon, String> colTenBN = hoaDonCol("Bệnh nhân", HoaDon::getTenBenhNhan, 160);
        TableColumn<HoaDon, String> colNgayLap = hoaDonCol("Ngày lập",
                h -> h.getNgayLap() == null ? "" : h.getNgayLap().format(dtf), 140);
        TableColumn<HoaDon, String> colTienKham = hoaDonCol("Tiền khám",
                h -> money.format(h.getTienKham()) + " đ", 130);
        TableColumn<HoaDon, String> colTienThuoc = hoaDonCol("Tiền thuốc",
                h -> money.format(h.getTienThuoc()) + " đ", 130);
        TableColumn<HoaDon, String> colTongTien = hoaDonCol("Tổng tiền",
                h -> money.format(h.getTongTien()) + " đ", 140);
        tableHD.getColumns().addAll(colMaHD, colTenBN, colNgayLap,
                colTienKham, colTienThuoc, colTongTien);
        tableHD.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableHD.setPrefHeight(260);
        VBox.setVgrow(tableHD, Priority.ALWAYS);

        root.getChildren().addAll(header, cards, tblTitle, tableBS, hdToolbar, tableHD);
    }

    private VBox statCard(String label, Label valueLbl, String color) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(18));
        card.setPrefWidth(220);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
        Label lb = new Label(label);
        lb.setTextFill(Color.GRAY);
        lb.setFont(Font.font(12));
        valueLbl.setFont(Font.font("System", FontWeight.BOLD, 22));
        valueLbl.setTextFill(Color.web(color));
        card.getChildren().addAll(lb, valueLbl);
        return card;
    }

    private void loadData() {
        int thang = cbThang.getValue();
        int nam = cbNam.getValue();
        Map<String, Object> tong = dao.doanhThuThang(thang, nam);
        lblTongDT.setText(fmt(tong.get("TongDT")) + " đ");
        lblTongKham.setText(fmt(tong.get("TongKham")) + " đ");
        lblTongThuoc.setText(fmt(tong.get("TongThuoc")) + " đ");
        lblSoHD.setText(str(tong.get("SoHD")));

        List<Map<String, Object>> bs = dao.doanhThuTheoBacSi(thang, nam);
        tableBS.setItems(FXCollections.observableArrayList(bs));
        List<HoaDon> hoaDon = dao.getDoanhThuTheoThang(thang, nam);
        tableHD.setItems(FXCollections.observableArrayList(hoaDon));
    }

    private TableColumn<HoaDon, String> hoaDonCol(String title,
            java.util.function.Function<HoaDon, String> fn, double width) {
        TableColumn<HoaDon, String> col = new TableColumn<>(title);
        col.setCellValueFactory(d -> new javafx.beans.property.SimpleStringProperty(fn.apply(d.getValue())));
        col.setPrefWidth(width);
        return col;
    }

    private void xoaDoanhThu() {
        HoaDon hd = tableHD.getSelectionModel().getSelectedItem();
        if (hd == null) {
            alert(Alert.AlertType.WARNING, "Vui lòng chọn hóa đơn cần xóa khỏi doanh thu.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xóa doanh thu");
        confirm.setHeaderText("Xóa doanh thu từ hóa đơn " + hd.getMaHoaDon() + "?");
        confirm.setContentText("Hóa đơn và toàn bộ giao dịch thanh toán liên quan sẽ bị xóa vĩnh viễn.");
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (dao.xoaHoaDon(hd.getMaHoaDon())) {
                alert(Alert.AlertType.INFORMATION, "Đã xóa hóa đơn khỏi doanh thu.");
                loadData();
            } else {
                alert(Alert.AlertType.ERROR, "Xóa doanh thu thất bại. Vui lòng kiểm tra lại dữ liệu.");
            }
        }
    }

    private void alert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.showAndWait();
    }

    private void xuatExcel() {
        int thang = cbThang.getValue();
        int nam = cbNam.getValue();
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Xuất báo cáo doanh thu");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Workbook (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(String.format("Bao_cao_doanh_thu_%02d_%d.xlsx", thang, nam));
        File file = chooser.showSaveDialog(root.getScene() == null ? null : root.getScene().getWindow());
        if (file == null) return;

        if (!file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            file = new File(file.getParentFile(), file.getName() + ".xlsx");
        }

        Map<String, Object> tong = dao.doanhThuThang(thang, nam);
        List<List<Object>> tongQuan = new ArrayList<>();
        tongQuan.add(List.of("Chỉ tiêu", "Giá trị"));
        tongQuan.add(List.of("Tháng báo cáo", String.format("%02d/%d", thang, nam)));
        tongQuan.add(List.of("Tổng doanh thu (đ)", amount(tong.get("TongDT"))));
        tongQuan.add(List.of("Tiền khám (đ)", amount(tong.get("TongKham"))));
        tongQuan.add(List.of("Tiền thuốc (đ)", amount(tong.get("TongThuoc"))));
        tongQuan.add(List.of("Số hóa đơn", tong.get("SoHD") instanceof Number count ? count.intValue() : 0));

        List<List<Object>> doanhThuBacSi = new ArrayList<>();
        doanhThuBacSi.add(List.of("Mã bác sĩ", "Họ tên", "Số hóa đơn", "Tổng doanh thu (đ)"));
        for (Map<String, Object> bacSi : tableBS.getItems()) {
            doanhThuBacSi.add(List.of(
                    str(bacSi.get("MaBS")),
                    str(bacSi.get("HoTen")),
                    bacSi.get("SoHD") instanceof Number count ? count.intValue() : 0,
                    amount(bacSi.get("TongDT"))
            ));
        }

        List<List<Object>> hoaDon = new ArrayList<>();
        hoaDon.add(List.of("Mã hóa đơn", "Mã lần khám", "Bệnh nhân", "Ngày lập",
                "Tiền khám (đ)", "Tiền thuốc (đ)", "Tổng tiền (đ)", "Trạng thái"));
        for (HoaDon hd : tableHD.getItems()) {
            hoaDon.add(List.of(
                    str(hd.getMaHoaDon()),
                    str(hd.getMaLanKham()),
                    str(hd.getTenBenhNhan()),
                    hd.getNgayLap() == null ? "" : hd.getNgayLap().format(dtf),
                    amount(hd.getTienKham()),
                    amount(hd.getTienThuoc()),
                    amount(hd.getTongTien()),
                    trangThai(hd.getTrangThai())
            ));
        }

        Map<String, List<List<Object>>> sheets = new LinkedHashMap<>();
        sheets.put("Tổng quan", tongQuan);
        sheets.put("Theo bác sĩ", doanhThuBacSi);
        sheets.put("Chi tiết hóa đơn", hoaDon);
        try {
            ExcelExporter.export(file, sheets);
            alert(Alert.AlertType.INFORMATION, "Đã xuất báo cáo Excel thành công:\n" + file.getAbsolutePath());
        } catch (IOException ex) {
            ex.printStackTrace();
            alert(Alert.AlertType.ERROR, "Không thể xuất file Excel: " + ex.getMessage());
        }
    }

    private BigDecimal amount(Object value) {
        if (value instanceof BigDecimal decimal) return decimal;
        if (value instanceof Number number) return BigDecimal.valueOf(number.doubleValue());
        return BigDecimal.ZERO;
    }

    private String trangThai(String status) {
        if ("DaThanhToan".equals(status)) return "Đã thanh toán";
        if ("ChuaThanhToan".equals(status)) return "Chưa thanh toán";
        return str(status);
    }

    private String fmt(Object o) {
        if (o instanceof BigDecimal bd) return money.format(bd);
        return o == null ? "0" : o.toString();
    }

    private String str(Object o) {
        return o == null ? "" : o.toString();
    }
}