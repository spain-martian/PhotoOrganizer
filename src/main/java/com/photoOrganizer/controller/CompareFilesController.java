package com.photoOrganizer.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.nio.file.Files;
import java.nio.file.Path;

public class CompareFilesController {
    @FXML
    private ImageView sourceImageView;
    @FXML
    private ImageView toImageView;
    @FXML
    private Label folderSourceLabel;
    @FXML
    private Label folderToLabel;
    @FXML
    private Button overwriteButton;
    @FXML
    private Button renameSourceImage;
    @FXML
    private TextField nameField;

    private String newSourceName;
    private String currentName;

    public void setData(String fileName, Path path1, Path path2) {
        currentName = fileName;
        folderSourceLabel.setText(path1.toString());
        folderToLabel.setText(path2.toString());
        nameField.setText(fileName);
        sourceImageView.setImage(new Image("file:" + path1.resolve(fileName)));
        toImageView.setImage(new Image("file:" + path2.resolve(fileName)));

        nameField.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = !Files.exists(path2.resolve(newVal));
            renameSourceImage.setDisable(!valid || newVal.isEmpty());
            overwriteButton.setDisable(!newVal.equalsIgnoreCase(fileName));
        });
    }

    public String getNewSourceName() {
        return newSourceName;
    }

    @FXML
    private void renameSourceAction() {
        newSourceName = nameField.getText();
        closeAction();
    }

    @FXML
    private void overwriteAction() {
        newSourceName = currentName;
        closeAction();
    }

    @FXML
    private void closeAction() {
        Stage stage = (Stage) overwriteButton.getScene().getWindow();
        stage.close();
    }
}

