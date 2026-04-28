package com.photoOrganizer.controller;

import javafx.fxml.FXML;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class FullImageController {
    @FXML
    private ImageView fullImageView;

    public void setData(Image image) {
        fullImageView.setImage(image);
    }

    @FXML
    private void onImageClicked() {
        Stage stage = (Stage) fullImageView.getScene().getWindow();
        stage.close();
    }
}
