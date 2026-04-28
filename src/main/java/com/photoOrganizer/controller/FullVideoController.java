package com.photoOrganizer.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.shape.Polygon;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.nio.file.Path;

public class FullVideoController {
    @FXML
    private VBox videoBox;
    @FXML
    private MediaView mediaView;
    @FXML
    private Button playPauseButton;
    @FXML
    private Button stopButton;
    @FXML
    private Slider progressSlider;
    @FXML
    private Polygon playIcon;
    @FXML
    private HBox pauseIcon;
    @FXML
    private HBox videoContainer;

    private MediaPlayer player;

    public void setVideo(Path path) {

        Stage stage = (Stage) videoBox.getScene().getWindow();
        mediaView.setOnMouseClicked(e -> switchState());
        stage.setOnCloseRequest(e -> stopPlayer());

        Media media = new Media(new File(path.toString()).toURI().toString());
        player = new MediaPlayer(media);
        mediaView.setMediaPlayer(player);

        mediaView.setPreserveRatio(true);
        mediaView.fitWidthProperty().bind(videoContainer.widthProperty());
        mediaView.fitHeightProperty().bind(videoContainer.heightProperty());
        progressSlider.prefWidthProperty().bind(mediaView.fitWidthProperty());

        setupControls();
        player.play();
    }

    private void switchState() {
        if (player.getStatus() == MediaPlayer.Status.PLAYING) {
            player.pause();
            playIcon.setVisible(true);
            pauseIcon.setVisible(false);
        } else {
            player.play();
            playIcon.setVisible(false);
            pauseIcon.setVisible(true);
        }
    }

    private void setupControls() {

        playPauseButton.setOnAction(e -> switchState());

        stopButton.setOnAction(e -> {
            player.stop();
            playIcon.setVisible(true);
            pauseIcon.setVisible(false);
        });

        //Update slider while playing
        player.currentTimeProperty().addListener((obs, oldTime, newTime) -> {
            if (!progressSlider.isValueChanging()) {
                progressSlider.setValue(newTime.toSeconds());
            }
        });

        //Set slider max when ready
        player.setOnReady(() -> {
            progressSlider.setMax(player.getTotalDuration().toSeconds());
        });

        //Seek when user moves slider
        progressSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (progressSlider.isValueChanging()) {
                player.seek(Duration.seconds(newVal.doubleValue()));
            }
        });
    }

    private void stopPlayer() {
        if (player != null) {
            player.stop();
            player.dispose();
        }
    }
}
