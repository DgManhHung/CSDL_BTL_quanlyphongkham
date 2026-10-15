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

    public static Label title(String t) {
        Label l = new Label(t);
        l.setFont(Font.font("System", FontWeight.BOLD, 22));
        l.setTextFill(Color.web("#1e293b"));
        return l;
    }

    public static Button teal(String text) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color:#0d9488;-fx-text-fill:white;-fx-font-weight:bold;-fx-background-radius:6;-fx-cursor:hand;-fx-padding:8 14;");
        return b;
    }

    public static VBox card(String label, String value, String color) {
        VBox c = new VBox(6);
        c.setPadding(new Insets(18));
        c.setPrefWidth(210);
        c.setStyle("-fx-background-color:white;-fx-background-radius:10;-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.06),8,0,0,2);");
        Label lb = new Label(label);
        lb.setTextFill(Color.GRAY);
        lb.setFont(Font.font(12));
        Label v = new Label(value);
        v.setFont(Font.font("System", FontWeight.BOLD, 20));
        v.setTextFill(Color.web(color));
        v.setId("val");
        c.getChildren().addAll(lb, v);
        return c;
    }

    public static void setCardValue(VBox card, String value) {
        for (Node n : card.getChildren()) {
            if ("val".equals(n.getId())) ((Label) n).setText(value);
        }
    }

    public static HBox header(String titleText, Node... rights) {
        HBox h = new HBox(10);
        h.setAlignment(Pos.CENTER_LEFT);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        h.getChildren().addAll(title(titleText), sp);
        h.getChildren().addAll(rights);
        return h;
    }

    /** colDefs: "Key:Header:Width" */
    @SuppressWarnings("unchecked")
    public static TableView<Map<String, Object>> table(String... colDefs) {
        TableView<Map<String, Object>> tv = new TableView<>();
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
                else s = v == null ? "" : v.toString();
                return new SimpleStringProperty(s);
            });
            col.setPrefWidth(Double.parseDouble(p[2]));
            tv.getColumns().add(col);
        }
        tv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        return tv;
    }

    public static void loadTable(TableView<Map<String, Object>> tv, List<Map<String, Object>> data) {
        tv.setItems(FXCollections.observableArrayList(data));
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