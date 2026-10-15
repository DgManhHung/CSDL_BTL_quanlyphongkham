package com.phongkham.tv1_bacsi;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * ========== TV1: BÁC SĨ & TÍNH LƯƠNG ==========
 * PLACEHOLDER – Thành viên 1 tự code phần này rồi gắn vào MainShell.
 *
 * Việc cần làm:
 * - CRUD bảng BacSi
 * - Tính lương: 7.000.000 + 1.000.000/bệnh nhân chữa khỏi 1 loại bệnh
 * - Màn hình tra cứu & tính lương Bác sĩ
 */
public class BacSiView {

    public static Node placeholder() {
        VBox box = new VBox(14);
        box.setAlignment(Pos.CENTER);
        Label t = new Label("PHÂN HỆ BÁC SĨ & TÍNH LƯƠNG");
        t.setFont(Font.font("System", FontWeight.BOLD, 22));
        t.setTextFill(Color.web("#64748b"));
        Label m = new Label(
            "Đây là phần của Thành viên 1.\n\n" +
            "Cần làm:\n" +
            "• CRUD danh sách Bác sĩ (bảng BacSi đã có sẵn trong DB)\n" +
            "• Tính lương: 7tr cơ bản + 1tr / BN chữa khỏi 1 loại bệnh\n" +
            "• Màn hình tra cứu & tính lương theo tháng\n\n" +
            "Khi xong: thay method placeholder() bằng UI thật,\n" +
            "rồi gọi từ MainShell (menu \"Quản lý Bác sĩ\")."
        );
        m.setTextFill(Color.GRAY);
        m.setStyle("-fx-text-alignment: center;");
        box.getChildren().addAll(t, m);
        return box;
    }
}