package com.phongkham.app;

import com.phongkham.ui.LoginView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) {
        stage.setTitle("Hệ Thống Quản Lý Phòng Khám Tư Nhân");
        stage.setScene(new Scene(new LoginView(stage).getRoot(), 1100, 700));
        stage.setMinWidth(1000);
        stage.setMinHeight(600);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}