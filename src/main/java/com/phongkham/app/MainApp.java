package com.phongkham.app;

import com.phongkham.shared.DB;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;

import java.sql.SQLException;

public class MainApp extends Application {
    @Override
    public void start(Stage stage) {
        try {
            DB.ensureSchema();
        } catch (SQLException e) {
            e.printStackTrace();
            new Alert(Alert.AlertType.ERROR,
                    "Không thể cập nhật cấu trúc cơ sở dữ liệu: " + e.getMessage(),
                    ButtonType.OK).showAndWait();
            return;
        }

        stage.setTitle("Phòng Khám Tư Nhân ");
        Scene scene = new Scene(new LoginView(stage).getRoot(), 1100, 700);
        // CSS font tiếng Việt dùng chung cả app
        var css = getClass().getResource("/app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}