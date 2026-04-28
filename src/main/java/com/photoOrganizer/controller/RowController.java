package com.photoOrganizer.controller;

import com.photoOrganizer.Main;
import com.photoOrganizer.Utils.Tools;
import com.photoOrganizer.model.DirEntry;
import com.photoOrganizer.model.ImageData;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.photoOrganizer.controller.MainPageController.log;

public class RowController {
    @FXML
    private Label nLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label fileLabel;
    @FXML
    private Label folderLabel;
    @FXML
    private Label duplicatesLabel;
    @FXML
    private TextArea gpsText;
    @FXML
    private Button mapsButton;
    @FXML
    private ImageView thumbnail;
    @FXML
    private ImageView playIcon;
    @FXML
    private Button deleteButton;
    @FXML
    private Button moveButton;
    @FXML
    private ProgressIndicator progress;
    @FXML
    private Tooltip folderTooltip;
    @FXML
    private Tooltip duplicateTooltip;

    @FXML
    private VBox folderOptions;
    List<MoveOption> moveOptions = new ArrayList<>();

    private MainPageController mainPageController;

    private ImageData data;

    private final BooleanProperty emptyRow = new SimpleBooleanProperty(true);
    private ObjectProperty<RowController> activeRow;

    private volatile Process currentProcess;
    private final AtomicInteger requestId = new AtomicInteger(0);

    public RowController() {

    }

    @FXML
    public void initialize() {
        mapsButton.visibleProperty().bind(gpsText.textProperty().isNotEmpty());

        //align the play icon to center of the thumbnail
        playIcon.layoutXProperty().bind(
                Bindings.createDoubleBinding(
                        () -> thumbnail.getBoundsInParent().getWidth() / 2 - playIcon.getFitWidth() / 2,
                        thumbnail.boundsInParentProperty(), playIcon.fitWidthProperty()
                )
        );

        playIcon.layoutYProperty().bind(
                Bindings.createDoubleBinding(
                        () -> thumbnail.getBoundsInParent().getHeight() / 2 - playIcon.getFitHeight() / 2,
                        thumbnail.boundsInParentProperty(), playIcon.fitHeightProperty()
                )
        );
    }

    public void setupObservables(BooleanProperty initialized, ObjectProperty<RowController> activeRow) {
        this.activeRow = activeRow;

        BooleanBinding otherRowRunning =
                activeRow.isNotNull().and(activeRow.isNotEqualTo(this));

        moveButton.disableProperty().bind(
                initialized.not().or(otherRowRunning).or(emptyRow)
        );
        deleteButton.disableProperty().bind(
                initialized.not().or(otherRowRunning).or(emptyRow)
        );
    }

    public ImageData getData() {
        return data;
    }

    public void setData(ImageData data, int n, Set<DirEntry> sameDateDirs, MainPageController mainPageController) {
        this.mainPageController = mainPageController;
        this.data = data;
        resetDuplicates();
        if (currentProcess != null) {
            currentProcess.destroy();
            currentProcess = null;
        }
        thumbnail.setImage(null);
        playIcon.setVisible(false);
        progress.setVisible(false);

        if (data == null) {
            emptyRow.setValue(true);
            nLabel.setText("");
            dateLabel.setText("");
            fileLabel.setText("");
            folderLabel.setText("");
            folderLabel.getStyleClass().remove("label-unsorted");
            gpsText.setText("");
            folderOptions.getChildren().clear();
            moveOptions.clear();

            deleteButton.setOnAction((e) -> {});
            mapsButton.setOnAction(e -> {});
            thumbnail.setOnMouseClicked(e -> {});
            playIcon.setOnMouseClicked(e -> {});
        } else {
            emptyRow.setValue(false);
            nLabel.setText("" + (n + 1));
            dateLabel.setText(data.dateTime);
            fileLabel.setText(data.getFileName());
            String folder = data.getDirEntry().getPath().toString();
            folderLabel.setText(folder);
            folderTooltip.setText(folder);
            if (mainPageController.getConfig().isFolderUnsorted(folder)) {
                if (!folderLabel.getStyleClass().contains("label-unsorted")) {
                    folderLabel.getStyleClass().add("label-unsorted");
                }
            } else {
                folderLabel.getStyleClass().remove("label-unsorted");
            }
            gpsText.setText(data.gpsLat.isEmpty() ? "" : data.gpsLat + ", " + data.gpsLon);

            Path filePath = data.getDirEntry().getPath().resolve(data.getFileName());
            boolean isVideo = mainPageController.getConfig().isVideo(filePath.toString());

            try {
                if (isVideo) {
                    thumbnail.setOnMouseClicked(ev -> {});
                    playIcon.setOnMouseClicked(ev -> {});
                    createThumbnail(filePath);
                } else {
                    thumbnail.setImage(new Image("file:" + filePath));
                    thumbnail.setOnMouseClicked(e -> showImage(filePath));
                    playIcon.setOnMouseClicked(e -> {});
                }
            } catch (Exception ex) {
                log.error("Failed to create thumbnail for {}", filePath, ex);
            }
            thumbnail.setPreserveRatio(true);

            folderOptions.getChildren().clear();
            moveOptions.clear();
            for (DirEntry newDir : sameDateDirs) {
                if (!newDir.getPath().toString().equalsIgnoreCase(data.getDirEntry().getPath().toString())) {
                    Button button = new Button(newDir.getPath().toString());
                    button.setOnAction(v -> {
                        moveTo(data, newDir);
                        mainPageController.update();
                    });
                    button.setMnemonicParsing(false);
                    Tooltip tooltip = new Tooltip(button.getText());
                    tooltip.setShowDelay(Duration.millis(300));
                    button.setTooltip(tooltip);

                    button.getStyleClass().add("move-option-button");
                    folderOptions.getChildren().add(button);
                    moveOptions.add(new MoveOption(newDir, button));
                }
            }

            deleteButton.setOnAction((e) -> {
                if (activeRow.getValue() != null) {
                    activeRow.set(null);  //stop moving
                }
                mainPageController.delete(data);
            });

            if (!data.getMapsLink().isEmpty()) {
                mapsButton.setOnAction(e -> {
                    Main.getHostServicesInstance().showDocument(data.getMapsLink());
                });
            }
            setDuplicates();
        }
    }

    private void resetDuplicates() {
        duplicatesLabel.textProperty().unbind();
        duplicatesLabel.managedProperty().unbind();
        duplicateTooltip.textProperty().unbind();

        duplicatesLabel.setText("");
        duplicatesLabel.setManaged(false);
    }

    private void setDuplicates() {
        duplicatesLabel.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    if (data.loadingProperty().get()) {
                        return "Duplicates";
                    }

                    List<ImageData> dups = data.getDuplicates();
                    if (dups == null) {
                        return "Duplicates"; // not started yet
                    }

                    return dups.isEmpty() ? ""
                            :  dups.size() == 1
                            ? "1 duplicate"
                            : dups.size() + " duplicates";

                }, data.loadingProperty(), data.duplicatesProperty())
        );

        BooleanBinding showDuplicatesLabel = Bindings.createBooleanBinding(() -> {
            return data.loadingProperty().get() || data.getDuplicates() == null || !data.getDuplicates().isEmpty();
        }, data.loadingProperty(), data.duplicatesProperty());

        duplicatesLabel.visibleProperty().bind(showDuplicatesLabel);
        duplicatesLabel.managedProperty().bind(showDuplicatesLabel);

        duplicateTooltip.textProperty().bind(
                Bindings.createStringBinding(() -> {
                    List<ImageData> dups = data.getDuplicates();
                    if (dups == null || dups.isEmpty()) return "";

                    return dups.stream()
                            .map(d -> d.getDirEntry().getPath().toString())
                            .collect(Collectors.joining("\n"));

                }, data.duplicatesProperty())
        );
    }

    private String getFfmpegPath() {
        String base = System.getProperty("user.dir");

        Path[] candidates = new Path[] {
                Path.of(base, "app", "ffmpeg", "ffmpeg.exe"),      //for installed
                Path.of(base, "packaging", "ffmpeg", "ffmpeg.exe") //for Intellij
        };

        for (Path p : candidates) {
            if (Files.exists(p)) {
                return p.toString();
            }
        }

        throw new RuntimeException("FFmpeg not found");
    }

    private void createThumbnail(Path path) {
        Platform.runLater(() -> progress.setVisible(true));

        int myId = requestId.incrementAndGet();

        //cancel previous process
        Process old = currentProcess;
        if (old != null) {
            old.destroyForcibly();
        }

        new Thread(() -> {
            try {
                Path tempFile = Files.createTempFile("thumb_", ".png");

                ProcessBuilder pb = new ProcessBuilder(
                        getFfmpegPath(),
                        "-y",
                        "-ss", "00:00:01",
                        "-i", path.toString(),
                        "-frames:v", "1",
                        "-vf", "scale=320:-1",
                        "-update", "1",
                        tempFile.toString()
                );
                pb.redirectErrorStream(true);

                Process process = pb.start();
                currentProcess = process;
                consumeStream(process.getInputStream());//consume output (avoid blocking)

                boolean finished = process.waitFor(5, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return;
                }

                if (process.exitValue() == 0 && Files.exists(tempFile)) {
                    //ignore outdated requests
                    if (myId != requestId.get()) return;

                    Platform.runLater(() -> {
                        Image image = new Image(tempFile.toUri().toString());
                        thumbnail.setImage(image);
                        progress.setVisible(false);
                        playIcon.setVisible(true);

                        thumbnail.setOnMouseClicked(e -> showVideo(path));
                        playIcon.setOnMouseClicked(e -> showVideo(path));
                        try {
                            Files.deleteIfExists(tempFile);
                        } catch (IOException ignored) {}
                    });
                } else {
                    fail(myId);
                }
            } catch (Exception e) {
                log.error("Error while creating video thumbnail", e);
                fail(myId);
            }
        }).start();
    }

    private void consumeStream(InputStream is) {
        new Thread(() -> {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                while (br.readLine() != null) {}
            } catch (IOException ignored) {}
        }).start();
    }

    private void fail(int myId) {
        if (myId != requestId.get()) return;
        Platform.runLater(() -> {
            progress.setVisible(false);
            thumbnail.setImage(new Image(
                    getClass().getResource("/icons/unsupported.png").toExternalForm()
            ));
        });
    }

    public void moveTo(ImageData data, DirEntry newDir) {
        try {
            String oldName = data.getFileName();
            if (Files.exists(newDir.getPath().resolve(data.getFileName()))) {
                CompareFilesController controller = showCompare(data.getFileName(), data.getPath(), newDir.getPath());
                if (controller == null) { //cancel move
                    return;
                }
                String newNameFrom = controller.getNewSourceName();
                if (newNameFrom == null) {
                    return; //cancel
                }
                if (!newNameFrom.equals(data.getFileName())) { //rename source and move
                    data.setFileName(newNameFrom);
                } else { //overwrite, remove target from list
                    ImageData toDelete = newDir.files.get(oldName);
                    if (toDelete != null) {
                        mainPageController.delete(toDelete);
                    }
                }
            }

            Files.move(data.getDirEntry().getPath().resolve(oldName),
                    newDir.getPath().resolve(data.getFileName()),
                    StandardCopyOption.REPLACE_EXISTING
            );
            data.getDirEntry().files.remove(oldName);
            newDir.files.put(data.getFileName(), data);

            DirEntry oldDir = data.getDirEntry();
            data.setDirEntry(newDir);
            mainPageController.updateFolderFiles(oldDir);
            mainPageController.updateFolderFiles(newDir);
        } catch (Exception ex) {
            log.error("Error while moving folder {}", newDir.getPath(), ex);
            Tools.showError("Error while moving folder");
        }
    }

    private CompareFilesController showCompare(String fileName, Path path1, Path path2) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/compare.fxml"));
            Parent root = loader.load();
            CompareFilesController controller = loader.getController();

            controller.setData(fileName, path1, path2);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    stage.close();
                }
            });
            stage.showAndWait();
            return controller;
        } catch (Exception e) {
            log.error("Error while displaying 2 images: ", e);
            return null;
        }
    }

    @FXML
    private void moveAction() {
        if (activeRow.get() == this) {
            activeRow.setValue(null); //stop moving mode
            mainPageController.changeMoveMode(true);
        } else {
            activeRow.set(this);   //start moving mode
            mainPageController.changeMoveMode(false);
        }
    }

    private void showImage(Path filePath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/image.fxml"));
            Parent root = loader.load();
            FullImageController controller = loader.getController();

            Image image = new Image("file:" + data.getDirEntry().getPath().resolve(data.getFileName()));
            controller.setData(image);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    stage.close();
                }
            });
            stage.showAndWait();

        } catch (Exception e) {
            log.error("Error while displaying image {} ", filePath, e);
        }
    }

    private void showVideo(Path filePath) {
        try {
            Desktop.getDesktop().open(filePath.toFile());
//            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/video.fxml"));
//            Parent root = loader.load();
//
//            FullVideoController controller = loader.getController();
//
//            Stage stage = new Stage();
//            stage.initModality(Modality.APPLICATION_MODAL);
//            Scene scene = new Scene(root);
//            stage.setScene(scene);
//            scene.setOnKeyPressed(e -> {
//                if (e.getCode() == KeyCode.ESCAPE) {
//                    stage.close();
//                }
//            });
//
//            controller.setVideo(filePath);
//            stage.showAndWait();

        } catch (Exception e) {
            log.error("Error while displaying video: {} ", filePath, e);
        }
    }

    public void updateFolderName() {
        if (data != null) {
            String folder = data.getDirEntry().getPath().toString();
            folderLabel.setText(folder);

            for (MoveOption moveOption: moveOptions) {
                moveOption.button().setText(moveOption.dir().getPath().toString());
            }
        }
    }
}

record MoveOption(
        DirEntry dir,
        Button button
) {
}