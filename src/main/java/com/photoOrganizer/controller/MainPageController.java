package com.photoOrganizer.controller;

import com.photoOrganizer.Utils.Tools;
import com.photoOrganizer.model.*;

import com.photoOrganizer.service.DuplicateService;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.awt.Desktop;
import java.io.File;
import java.util.*;

public class MainPageController {
    private static final int NR = 5;

    private ImagesMonitor imagesMonitor;
    private int pos = 0; //current position in the list for displaying
    private int lastCommonPos = 0; //last position in the full list before selecting folder

    private DuplicateService duplicateService;

    private Config config;

    private boolean destinationTreeInitialized = false;

    private final BooleanProperty imagesLoaded = new SimpleBooleanProperty(false);
    // on which row the Move button pressed
    private final ObjectProperty<RowController> activeRow = new SimpleObjectProperty<>(null);
    private final ObjectProperty<DirEntry> selectedFolder = new SimpleObjectProperty<>(null);

    private RowController[] rowControllers;

    private final List<FolderController> folderControllers = new ArrayList<>();
    private final List<FolderController> parentControllers = new ArrayList<>();

    public static final Logger log = LoggerFactory.getLogger(MainPageController.class);

    @FXML
    private Button settingsButton;
    @FXML
    private RowController row1Controller;
    @FXML
    private RowController row2Controller;
    @FXML
    private RowController row3Controller;
    @FXML
    private RowController row4Controller;
    @FXML
    private RowController row5Controller;
    @FXML
    private TextField gotoField;
    @FXML
    private TextField searchField;
    @FXML
    private VBox parentFolders;
    @FXML
    private VBox childFolders;
    @FXML
    private Button startButton;
    @FXML
    private Button gotoButton;
    @FXML
    private Button prev1;
    @FXML
    private Button next1;
    @FXML
    private Button prevp;
    @FXML
    private Button nextp;
    @FXML
    private Button prev20;
    @FXML
    private Button next20;
    @FXML
    private Button prev200;
    @FXML
    private Button next200;
    @FXML
    private Label startingDateLabel;
    @FXML
    private HBox startingDateTitle;
    @FXML
    private Label selectedFolderLabel;
    @FXML
    private Label selectedModeLabel;
    @FXML
    private Button unselectButton;
    @FXML
    private Button searchButton;

    @FXML
    public void initialize() {

        log.info("Started");

        rowControllers = new RowController[] {row1Controller, row2Controller, row3Controller, row4Controller, row5Controller};

        startButton.setDisable(false);
        settingsButton.setDisable(false);
        disableButtons(true);

        for (RowController rc : rowControllers) {
            rc.setData(null, 0, new HashSet<>(), null);
            rc.setupObservables(imagesLoaded, activeRow);
        }
        config = Config.loadConfig();

        imagesMonitor = new ImagesMonitor(config, selectedFolder); //all files monitor

        selectedModeLabel.textProperty().bind(Bindings.createObjectBinding(
                () -> selectedFolder.get() != null ? "Media Files From Selected Folder Only" : "All Media Files From Root And Subfolders",
                selectedFolder
        ));
        selectedFolderLabel.textProperty().bind(Bindings.createObjectBinding(
                () -> selectedFolder.get() != null ? selectedFolder.get().getPath().toString() : "",
                selectedFolder
        ));
        selectedFolder.addListener((obs, oldVal, newVal) -> {
            updatePosOnSelection(oldVal, newVal);
            unselectButton.setVisible(newVal != null);
        });

        unselectButton.setOnAction(e -> selectedFolder.setValue(null));

        setupSearchButton();
    }

    private void disableButtons(boolean disable) {
        prev1.setDisable(disable);
        prevp.setDisable(disable);
        prev20.setDisable(disable);
        prev200.setDisable(disable);
        next1.setDisable(disable);
        nextp.setDisable(disable);
        next20.setDisable(disable);
        next200.setDisable(disable);
        gotoButton.setDisable(disable);
        searchButton.setDisable(disable);
    }

    @FXML
    private void startAction() {

        String error = "";
        if (config == null) {
            error = "Error while reading config.json";
        } else {
            error = config.findConfigErrors();
        }
        if (!error.isEmpty()) {
            Tools.showError(error);
            System.exit(-4);
        }

        startButton.setDisable(true);
        settingsButton.setDisable(true);

        Task<Void> task = createLoadingTask();

        setStartDateLabel(config.getStartDate());
        startButton.setText("...");
        startButton.textProperty().bind(task.messageProperty());

        new Thread(task).start();
    }

    public void fullReset() {
        imagesLoaded.setValue(false);

        disableButtons(true);
        startButton.setText("Load");

        imagesMonitor.clearAllData();
        activeRow.setValue(null);
        selectedFolder.setValue(null);

        clickOnPath(null); //clear all 'move' folders
        folderControllers.clear();
        parentControllers.clear();

        for (int i = 0; i < NR; i++) {
            rowControllers[i].setData(null, 0, new HashSet<>(), null);
        }
        destinationTreeInitialized = false;
        setStartDateLabel("");

        startButton.setDisable(false);
        pos = 0;
    }

    public Task<Void> createLoadingTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                imagesMonitor.loadDirsAndImages(this::updateMessage);
                return null;
            }

            @Override
            protected void succeeded() {
                imagesMonitor.buildData();
                startButton.textProperty().unbind();
                updateNumberOfFiles();
                log.info("Loaded {}", startButton.getText());

                duplicateService = new DuplicateService(imagesMonitor.getMapBySize());
                duplicateService.normalize();

                disableButtons(false);
                settingsButton.setDisable(false);
                imagesLoaded.setValue(true);
                initParentFolder();

                navigateToPos(); //always the last command
                //imagesMonitor.printAllDirs();
            };
        };
    }

    private void updateNumberOfFiles() {
        startButton.setText(imagesMonitor.getImagesListSize() + " files");
    }

    private HashSet<DirEntry> getSameDateDirs(String date) {
        String d = date.substring(0, 10); //extract date only
        List<ImageData> sameDateImages = imagesMonitor.getSameDateImages(d);
        HashSet<DirEntry> foldersUsedThatDay = new HashSet<>();

        for (ImageData imageData: sameDateImages) {
            foldersUsedThatDay.add(imageData.getDirEntry());
        }
        return foldersUsedThatDay;
    }

    public void update() {
        navigateToPos();
    }

    private void navigateToPos() {
        activeRow.setValue(null);
        changeMoveMode(true);

        ArrayList<ImageData> newVisibleImages = createCurrentDisplayList();

        for (int i = 0; i < newVisibleImages.size(); i++) {
            ImageData imageData = newVisibleImages.get(i);
            HashSet<DirEntry> sameDateDirs = getSameDateDirs(imageData.dateTime);
            rowControllers[i].setData(imageData, i + pos, sameDateDirs, this);
        }
        for (int i = newVisibleImages.size(); i < NR; i++) {
            rowControllers[i].setData(null, 0, new HashSet<>(), null);
        }

        duplicateService.updateVisibleImages(newVisibleImages);
    }

    public void updatePosOnSelection(DirEntry old, DirEntry now) {
        if (now == null) {
            pos = lastCommonPos;
        } else {
            if (old == null) lastCommonPos = pos;
            pos = 0;
        }
        navigateToPos();
    }

    private ArrayList<ImageData> createCurrentDisplayList() {

        ArrayList<ImageData> source = imagesMonitor.getCurrentImageList();

        int length = source.size();
        if (pos >= length) pos = length - 1;    //length might be 0
        if (pos < 0) pos = 0;

        ArrayList<ImageData> images = new ArrayList<>();
        for (int i = 0; i + pos < length && i < NR; i++) {
            images.add(source.get(i + pos));
        }
        return images;
    }

    @FXML
    private void next1Action() {
        ++pos;
        navigateToPos();
    }
    @FXML
    private void nextPageAction() {
        pos += NR;
        navigateToPos();
    }
    @FXML
    private void next20Action() {
        pos += 20;
        navigateToPos();
    }
    @FXML
    private void next200Action() {
        pos += 200;
        navigateToPos();
    }

    @FXML
    private void prev1Action() {
        --pos;
        navigateToPos();
    }
    @FXML
    private void prevPageAction() {
        pos -= NR;
        navigateToPos();
    }
    @FXML
    private void prev20Action() {
        pos -= 20;
        navigateToPos();
    }
    @FXML
    private void prev200Action() {
        pos -= 200;
        navigateToPos();
    }
    @FXML
    private void goAction() {
        try {
            if (gotoField.getText().equals("d")) {
                firstDuplicateSearch();
                return;
            }
            if (gotoField.getText().contains("-")) {
                firstDateSearch();
                return;
            }
            pos = Integer.parseInt(gotoField.getText()) - 1;
        } catch (Exception ignore) {
            pos = imagesMonitor.getImagesListSize();
        }
        navigateToPos();
    }

    public void handleKey(KeyEvent e) {
        if (imagesLoaded.getValue() /* && !(e.getTarget() instanceof TextInputControl)*/) {
            switch (e.getCode()) {
                case PAGE_UP -> prevPageAction();
                case PAGE_DOWN -> nextPageAction();
                case DOWN ->  next1Action();
                case UP -> prev1Action();
            }
        }
    }

    public void delete(ImageData imageData) {

        String name = imageData.getDirEntry().getPath().toString() + "\\" + imageData.getFileName();
        try {
            File file = new File(name);

            boolean deleted = false;
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                deleted = desktop.moveToTrash(file);
                if (deleted) {
                    log.info("Moved to trash bin {}", name);
                } else {
                    log.info("Failed to move to trash bin {}", name);
                }
            }
            if (!deleted){
                log.info("Permanently deleting file {} ...", name);
                deleted = file.delete();
                log.info("result is {}", deleted);
            }
            if (deleted) {
                imagesMonitor.deleteImage(imageData);
                duplicateService.removeImageData(imageData);
            }
            updateNumberOfFiles();
            updateFolderFiles(imageData.getDirEntry());
        } catch (Exception ex) {
            log.error("Error while deleting file {}", name, ex);
        }
        update();
    }

    public void initParentFolder() {
        if (!destinationTreeInitialized) { //if the tree is not initialized
            addParentFolder(imagesMonitor.getRootDir());
        }
    }

    public void addParentFolder(DirEntry dir) {
        if (dir == null) return;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/folder_row.fxml"));
            HBox box = loader.load();

            FolderController controller = loader.getController();
            controller.setData(this, dir, dir.parentDir != null, true, selectedFolder);
            parentControllers.add(controller);
            VBox.setMargin(box, new Insets(0, 0, 0, (parentFolders.getChildren().size() - 1) * 6));
            parentFolders.getChildren().add(box);

            destinationTreeInitialized = true;
        } catch (Exception ex) {
            log.error("Error while creating dir button {}", dir.getPath(), ex);
            return;
        }

        setSubDirsFor(dir);
    }

    public void clickOnPath(DirEntry dir) { //dir == null -> remove all
        ObservableList<Node> children = parentFolders.getChildren();

        for (int i = parentControllers.size() - 1; i >= 0; i--) {
            FolderController controller = parentControllers.get(i);
            if (controller.getDir() == dir && dir != null) break;
            parentControllers.remove(i);
            children.remove(i);
        }
        setSubDirsFor(dir);
    }

    private void setSubDirsFor(DirEntry dir) {
        childFolders.getChildren().clear();
        folderControllers.clear();
        if (dir == null) return; //clear only

        List<DirEntry> dirs = dir.subDirs.stream().sorted(Comparator.comparing(a -> a.getPath().toString().toLowerCase())).toList();
        ArrayList<HBox> folderBoxes = new ArrayList<>();

        try {
            for (DirEntry subDir: dirs) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/folder_row.fxml"));
                HBox box = loader.load();

                FolderController controller = loader.getController();
                controller.setData(this, subDir, true, false, selectedFolder);
                folderControllers.add(controller);

                folderBoxes.add(box);
                controller.setDisableMove(activeRow.get() == null);
            }
        } catch (Exception ex) {
            log.error("Error while creating dir button {}", dir.getPath(), ex);
            return;
        }

        childFolders.getChildren().addAll(folderBoxes);
    }

    public void changeMoveMode(boolean disable) {
        for (FolderController controller: folderControllers) {
            controller.setDisableMove(disable);
        }
    }

    public Config getConfig() {
        return config;
    }

    public ObjectProperty<RowController> activeRowProperty() {
        return activeRow;
    }

    public void updateFolderNames() {
        for (RowController row: rowControllers) {
            row.updateFolderName();
        }
    }

    @FXML
    private void settingsAction() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/settings.fxml"));
            Parent root = loader.load();
            SettingsController settingsController = loader.getController();

            Bounds bounds = settingsButton.localToScreen(settingsButton.getBoundsInLocal());

            Stage stage = new Stage();
            stage.setX(bounds.getMinX() + 10.0);
            stage.setY(bounds.getMaxY() - 5.0);

            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root);
            stage.setScene(scene);
            scene.setOnKeyPressed(e -> {
                if (e.getCode() == KeyCode.ESCAPE) {
                    stage.close();
                }
            });
            settingsController.setData(config, imagesLoaded.getValue(), this);
            stage.showAndWait();

        } catch (Exception e) {
            log.error("Error while loading settings", e);
        }
    }

    private void firstDuplicateSearch() {
        int index = imagesMonitor.getFirstDuplicateIndex();
        if (index >= 0) {
            pos = index;
            navigateToPos();
        } else {
            gotoField.setText("");
        }
    }

    private void firstDateSearch() {
        int index = imagesMonitor.getFirstDateMatch(gotoField.getText());
        if (index >= 0) {
            pos = index;
            navigateToPos();
        } else {
            gotoField.setText("");
        }
    }

    public ImagesMonitor getImagesMonitor() {
        return imagesMonitor;
    }

    public DirEntry getRootDir() {
        return getImagesMonitor().getRootDir();
    }

    public void updateFolderFiles(DirEntry dir) {
        for (FolderController fc: parentControllers) {
            if (fc.dir == dir) {
                fc.updateFolderLabel(dir.getShortName());
            }
        }
        for (FolderController fc: folderControllers) {
            if (fc.dir == dir) {
                fc.updateFolderLabel(dir.getShortName());
            }
        }
    }

    private void setStartDateLabel(String text) {
        startingDateLabel.setText(text);
        startingDateTitle.setVisible(!text.isEmpty());
        startingDateTitle.setManaged(!text.isEmpty());
    }

    private void setupSearchButton() {

        searchButton.setOnAction(e -> {
            searchField.setVisible(true);
            searchField.setManaged(true);
            searchField.requestFocus();
        });
        searchField.setOnAction(e -> hideSearch());
        searchField.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) hideSearch();
        });
        searchField.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                hideSearch();
            }
        });

        ContextMenu suggestions = new ContextMenu();

        searchField.textProperty().addListener((obs, oldVal, text) -> {
            suggestions.getItems().clear();
            searchField.selectAll();

            if (text == null || text.isEmpty()) {
                suggestions.hide();
                return;
            }

            Map<String, DirEntry> matches = imagesMonitor.getFoldersByText(text.toLowerCase());

            ListView<String> listView = new ListView<>();
            listView.getItems().addAll(matches.keySet());

            listView.setMaxWidth(320);
            listView.setPrefWidth(320);
            listView.setMaxHeight(400);

            listView.setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    String name = listView.getSelectionModel().getSelectedItems().getFirst();
                    if (name != null) {
                        hideSearch();
                        suggestions.hide();
                        List<DirEntry> parents = new ArrayList<>();
                        DirEntry dir = matches.get(name);
                        while (dir != null) {
                            parents.add(dir);
                            dir = dir.parentDir;
                        }

                        clickOnPath(null); //clear all parent dirs
                        for (int i = parents.size() - 1; i >= 0; i--) {
                            addParentFolder(parents.get(i));
                        }
                    }
                }
            });

            CustomMenuItem item = new CustomMenuItem(listView, false);
            suggestions.getItems().setAll(item);

            if (!suggestions.getItems().isEmpty()) {
                suggestions.show(searchField, Side.BOTTOM, 0, 0);
            } else {
                suggestions.hide();
            }
        });
    }

    private void hideSearch() {
        searchField.setVisible(false);
        searchField.setManaged(false);
    }
}