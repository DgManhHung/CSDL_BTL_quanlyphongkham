package com.phongkham.shared;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;

/** Helper UI dùng chung */
public class UI {
    public static final NumberFormat MONEY = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final int PAGE_SIZE = 14;
    private static final String TABLE_PAGER_KEY = UI.class.getName() + ".tablePager";

    public static Label title(String t) {
        Locale locale = Locale.forLanguageTag("vi");
        String normalized = t.replaceAll("\\s*\\(TV\\d+\\)$", "").trim().toLowerCase(locale);
        String display = normalized.isEmpty() ? t
                : normalized.substring(0, 1).toUpperCase(locale) + normalized.substring(1);
        Label l = new Label(display);
        l.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        l.getStyleClass().add("page-title");
        return l;
    }

    public static Button teal(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("primary-button");
        return b;
    }

    public static Button danger(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("danger-button");
        return b;
    }

    public static VBox card(String label, String value, String color) {
        VBox c = new VBox(6);
        c.setPadding(new Insets(18));
        c.setPrefWidth(210);
        c.getStyleClass().add("stat-card");
        Label lb = new Label(label);
        lb.getStyleClass().add("stat-label");
        lb.setFont(Font.font("Segoe UI", 12));
        Label v = new Label(value);
        v.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        v.setTextFill(Color.web(color));
        v.setId("val");
        v.getStyleClass().add("stat-value");
        c.getChildren().addAll(lb, v);
        return c;
    }

    public static void setCardValue(VBox card, String value) {
        for (Node n : card.getChildren()) {
            if ("val".equals(n.getId())) ((Label) n).setText(value);
        }
    }

    public static HBox header(String titleText, Node... rights) {
        HBox h = pageHeading(titleText);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        h.getChildren().add(sp);
        h.getChildren().addAll(rights);
        return h;
    }

    public static HBox pageHeading(String titleText, Node... metadata) {
        HBox h = new HBox(10);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().addAll("page-heading", "page-header");
        h.getChildren().add(title(titleText));
        h.getChildren().addAll(metadata);
        return h;
    }

    public static Label badge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("count-badge");
        return badge;
    }

    public static HBox toolbar(Node... controls) {
        HBox toolbar = new HBox(8);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.getStyleClass().add("toolbar");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        toolbar.getChildren().add(spacer);
        toolbar.getChildren().addAll(controls);
        return toolbar;
    }

    /** colDefs: "Key:Header:Width" */
    public static TableView<Map<String, Object>> table(String... colDefs) {
        TableView<Map<String, Object>> tv = new TableView<>();
        tv.getStyleClass().add("data-table");
        for (String def : colDefs) {
            String[] p = def.split(":");
            TableColumn<Map<String, Object>, String> col = new TableColumn<>(p[1]);
            String key = p[0];
            col.setCellValueFactory(d -> {
                Object v = d.getValue().get(key);
                String s;
                if (v instanceof BigDecimal bd) s = MONEY.format(bd);
                else if (v instanceof Number && key.matches(".*(Tien|Gia|Luong|PhuCap|Tong).*"))
                    s = MONEY.format(v);
                else if (v == null) s = "";
                else if ("TrangThai".equals(key)) {
                    s = switch (v.toString()) {
                        case "DaKhoi" -> "Đã Khỏi";
                        case "DangDieuTri" -> "Đang Điều Trị";
                        default -> v.toString();
                    };
                } else s = v.toString();
                return new SimpleStringProperty(s);
            });
            col.setPrefWidth(Double.parseDouble(p[2]));
            col.setCellFactory(column -> new TableCell<>() {
                private final Label tag = new Label();

                @Override
                protected void updateItem(String value, boolean empty) {
                    super.updateItem(value, empty);
                    setGraphic(null);
                    setText(null);
                    setAlignment(Pos.CENTER_LEFT);
                    if (empty || value == null || value.isBlank()) return;

                    if (isTagColumn(key)) {
                        String style = tagStyle(key, value);
                        tag.setText(value);
                        tag.getStyleClass().setAll("table-tag", style);
                        // Khóa màu chữ trực tiếp để khi click chọn dòng không bị biến thành chữ đen
                        tag.setStyle("-fx-text-fill: " + tagTextColor(style) + ";");
                        setGraphic(tag);
                        setAlignment(Pos.CENTER);
                    } else {
                        setText(value);
                        setPadding(Insets.EMPTY); // Reset khoảng cách lề
                        if (isCenteredColumn(key)) {
                            setAlignment(Pos.CENTER);
                        } else if (isRightColumn(key)) {
                            setAlignment(Pos.CENTER_RIGHT);
                            setPadding(new Insets(0, 14, 0, 0)); // Cách lề phải 14px để số không dính sát mép cột
                        }
                    }
                }
            });
            tv.getColumns().add(col);
        }
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tv;
    }

    public static VBox tablePanel(TableView<Map<String, Object>> table) {
        VBox panel = new VBox();
        panel.getStyleClass().add("table-panel");
        HBox footer = new HBox(8);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("table-footer");
        Label range = new Label();
        range.getStyleClass().add("table-range");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button previous = new Button("‹");
        previous.getStyleClass().add("pagination-button");
        Button page = new Button("1");
        page.getStyleClass().addAll("pagination-button", "pagination-current");
        page.setMouseTransparent(true);
        page.setFocusTraversable(false);
        Button next = new Button("›");
        next.getStyleClass().add("pagination-button");
        TablePager pager = new TablePager(table, range, previous, page, next);
        previous.setOnAction(event -> pager.showPage(pager.page - 1));
        next.setOnAction(event -> pager.showPage(pager.page + 1));
        footer.getChildren().addAll(range, spacer, previous, page, next);
        panel.getChildren().addAll(table, footer);
        VBox.setVgrow(table, Priority.ALWAYS);
        table.getProperties().put(TABLE_PAGER_KEY, pager);
        pager.setData(new ArrayList<>(table.getItems()));
        return panel;
    }

    public static void loadTable(TableView<Map<String, Object>> tv, List<Map<String, Object>> data) {
        TablePager pager = (TablePager) tv.getProperties().get(TABLE_PAGER_KEY);
        if (pager == null) {
            tv.setItems(FXCollections.observableArrayList(data));
            return;
        }
        pager.setData(data);
    }

    private static boolean isTagColumn(String key) {
        return "TrinhDo".equals(key) || "TrangThai".equals(key) || "LoaiHoTro".equals(key);
    }

    private static boolean isCenteredColumn(String key) {
        return key.startsWith("Ma") || key.equals("CMT") || key.equals("GioiTinh")
                || key.startsWith("Ngay") || key.equals("ThamNien") || key.startsWith("So");
    }
    private static boolean isRightColumn(String key) {
        return "PhuCap".equals(key) || "TongLuong".equals(key);
    }

    private static String tagStyle(String key, String value) {
        String normalized = value.toLowerCase(Locale.ROOT).trim();
        if ("TrinhDo".equals(key)) {
            return switch (normalized) {
                case "bác sĩ nội trú", "bsnt" -> "table-tag-green";
                case "cử nhân", "đại học", "bác sĩ chuyên khoa i", "bscki", "thạc sĩ", "ths" -> "table-tag-blue";
                case "cao đẳng", "bác sĩ chuyên khoa ii", "bsckii", "tiến sĩ", "ts" -> "table-tag-orange";
                default -> "table-tag-gray"; // Bác sĩ đa khoa (BSĐK) & Trung cấp (Y tá)
            };
        }
        if (normalized.contains("đã thanh toán") || normalized.contains("đã khỏi")
                || normalized.contains("hoàn thành") || normalized.contains("đang làm việc")) {
            return "table-tag-green";
        }
        if (normalized.contains("chưa thanh toán") || normalized.contains("đang điều trị")) {
            return "table-tag-orange";
        }
        if (normalized.contains("hủy") || normalized.contains("đã nghỉ việc")) {
            return "table-tag-red"; // Hoặc đổi thành "table-tag-gray" nếu muốn Đã nghỉ việc màu xám
        }
        return "table-tag-blue";
    }

    /** Giữ đúng màu chữ cho 5 loại thẻ ngay cả khi dòng TableView đang được :selected */
    private static String tagTextColor(String styleClass) {
        return switch (styleClass) {
            case "table-tag-green" -> "#15803d";
            case "table-tag-orange" -> "#c2410c";
            case "table-tag-red" -> "#b91c1c";
            case "table-tag-gray" -> "#475569";
            default -> "#0284c7"; // table-tag-blue
        };
    }

    private static final class TablePager {
        private final TableView<Map<String, Object>> table;
        private final Label range;
        private final Button previous;
        private final Button pageButton;
        private final Button next;
        private List<Map<String, Object>> data = List.of();
        private int page;

        private TablePager(TableView<Map<String, Object>> table, Label range, Button previous,
                           Button pageButton, Button next) {
            this.table = table;
            this.range = range;
            this.previous = previous;
            this.pageButton = pageButton;
            this.next = next;
        }

        private void setData(List<Map<String, Object>> rows) {
            data = List.copyOf(rows);
            page = 0;
            update();
        }

        private void showPage(int requestedPage) {
            int pageCount = Math.max(1, (data.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            page = Math.max(0, Math.min(requestedPage, pageCount - 1));
            update();
        }

        private void update() {
            int start = page * PAGE_SIZE;
            int end = Math.min(start + PAGE_SIZE, data.size());
            range.setText(data.isEmpty()
                    ? "Hiển thị 0-0 trong số 0"
                    : "Hiển thị " + (start + 1) + "-" + end + " trong số " + data.size());
            int pageCount = Math.max(1, (data.size() + PAGE_SIZE - 1) / PAGE_SIZE);
            pageButton.setText(Integer.toString(page + 1));
            previous.setDisable(page == 0);
            next.setDisable(page + 1 >= pageCount);
            table.setItems(FXCollections.observableArrayList(data.subList(start, end)));
        }
    }

    public static void alert(String msg) {
        new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK).showAndWait();
    }

    public static void error(String msg) {
        new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK).showAndWait();
    }

    public static String str(Object o) { return o == null ? "" : o.toString(); }

    public static long toLong(Object o) {
        if (o instanceof Number n) return n.longValue();
        try { return Long.parseLong(str(o).replace(",", "")); } catch (Exception e) { return 0; }
    }
}