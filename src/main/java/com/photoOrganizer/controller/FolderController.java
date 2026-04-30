package com.photoOrganizer.controller;

import com.photoOrganizer.Utils.Tools;
import com.photoOrganizer.model.DirEntry;
import com.photoOrganizer.model.ImageData;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

import static com.photoOrganizer.controller.MainPageController.log;

public class FolderController {

    public DirEntry dir;
    private MainPageController mainPageController;
    private boolean isParentPath;
    private ObjectProperty<DirEntry> selectedFolder;
    private ChangeListener<DirEntry> selectionListener; //keep it here

    @FXML
    private Button moveHereButton;
    @FXML
    private Button folderButton;
    @FXML
    private Label folderLabel;
    @FXML
    private ImageView plusIcon;
    @FXML
    private HBox folderButtonGraphic;

    public void setData(MainPageController mainPageController, DirEntry dir, boolean shortName, boolean isParentPath, ObjectProperty<DirEntry> selectedFolder) {
        this.dir = dir;
        this.mainPageController = mainPageController;
        this.isParentPath = isParentPath;
        this.selectedFolder = selectedFolder;

        String name = shortName ? dir.getShortName() : dir.getPath().toString();
        updateFolderLabel(name);

        moveHereButton.setVisible(!isParentPath);
        moveHereButton.managedProperty().bind(moveHereButton.visibleProperty());
        plusIcon.setVisible(!dir.subDirs.isEmpty());

        if (isParentPath) {
            folderButton.setOnAction((e) -> mainPageController.clickOnPath(dir));
        } else {
            folderButton.setOnAction((e) -> {
                if (!dir.subDirs.isEmpty()) {
                    mainPageController.addParentFolder(dir);
                }
            });
        }

        ContextMenu menu = getContextMenu();
        folderButton.setContextMenu(menu);

        setSelectionStyle(selectedFolder.getValue()); //init
        selectionListener = (obs, oldVal, newVal) -> setSelectionStyle(newVal);
        selectedFolder.addListener(selectionListener);
    }

    private void setSelectionStyle(DirEntry newVal) {
        if (newVal == dir) {
            folderButtonGraphic.getStyleClass().add("selected-folder-button");
        } else {
            folderButtonGraphic.getStyleClass().remove("selected-folder-button");
        }
    }

    private ContextMenu getContextMenu() {
        MenuItem item1 = new MenuItem("Create subfolder");
        item1.setOnAction(e -> createSubfolderDialog());
        MenuItem item3 = new MenuItem("Select folder");
        item3.setOnAction(e -> selectSubfolder());

        ContextMenu menu;
        if (dir.equals(mainPageController.getRootDir())) {
            menu = new ContextMenu(item1, item3);
        } else {
            MenuItem item2 = new MenuItem("Rename Folder");
            item2.setOnAction(e -> createRenameDialog());
            menu = new ContextMenu(item1, item2, item3);
        }
        return menu;
    }

    private void selectSubfolder() {
        selectedFolder.setValue(dir);
    }

    private void createRenameDialog() {
        Path parentPath = dir.parentDir.getPath();

        final String currentFolderName = dir.getPath().toString();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Rename Folder");
        dialog.setHeaderText("Enter new Folder name");
        dialog.setContentText("Folder name:");

        TextField input = dialog.getEditor();
        input.setText(DirEntry.toShortName(currentFolderName).replace("\\", ""));
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        input.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = isValidFolderName(newVal) && !Files.exists(parentPath.resolve(newVal));
            okButton.setDisable(!valid);
        });

        dialog.showAndWait().ifPresent(name -> {
            final Path newFolder = parentPath.resolve(name);

            if (Files.exists(newFolder)) {
                Tools.showError("Folder already exists");
                return;
            }

            try {
                Files.move(Path.of(currentFolderName), newFolder);
                mainPageController.getImagesMonitor().renameDirs(dir.getPath(), newFolder);
                dir.setPath(newFolder);

                updateFolderLabel(name);
                mainPageController.updateFolderNames();

                log.info("Renamed: from {} to {}", currentFolderName, newFolder);
            } catch (IOException ex) {
                log.error("Error while renaming folder from {} to {}", currentFolderName, newFolder, ex);
                Tools.showError("Renaming failed: " + ex.getMessage());
            }
        });
    }

    public void updateFolderLabel(String name) {
        int nf = dir.files.size();
        if (nf > 0) {
            name += " (" + nf + ")";
        }
        folderLabel.setText(name);
        Tooltip tooltip = new Tooltip(folderLabel.getText());
        tooltip.setShowDelay(Duration.millis(300));
        folderLabel.setTooltip(tooltip);
    }

    private void createSubfolderDialog() {
        Path curentPath = dir.getPath();
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Create Folder");
        dialog.setHeaderText("Enter subfolder name");
        dialog.setContentText("Folder name:");

        TextField input = dialog.getEditor();
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setDisable(true);

        input.textProperty().addListener((obs, oldVal, newVal) -> {
            boolean valid = isValidFolderName(newVal) && !Files.exists(curentPath.resolve(newVal));
            okButton.setDisable(!valid);
        });

        dialog.showAndWait().ifPresent(name -> {
            final Path newFolder = curentPath.resolve(name);
            try {
                if (Files.exists(newFolder)) {
                    Tools.showError("Folder already exists");
                    return;
                }

                Files.createDirectory(newFolder);
                final DirEntry newDir = new DirEntry(newFolder, dir);
                dir.subDirs.add(newDir);

                plusIcon.setVisible(true);
                if (isParentPath) {
                    mainPageController.clickOnPath(dir); //update sub dirs
                }
                log.info("Created: {}", newDir.getPath());
            } catch (Exception e) {
                log.error("Failed to create folder: {}", newFolder);
                Tools.showError("Failed to create folder:\n" + e.getMessage());
            }
        });
    }

    public void setDisableMove(boolean disable) {
        moveHereButton.setDisable(disable || isSameFolder());
    }

    @FXML
    private void moveHereAction() {
        RowController activeRow = mainPageController.activeRowProperty().getValue();
        if (activeRow == null) return;

        ImageData data = activeRow.getData();
        activeRow.moveTo(data, dir);
        mainPageController.update();
    }

    private boolean isSameFolder() {  //do not move to same dir
        RowController activeRow = mainPageController.activeRowProperty().getValue();
        if (activeRow == null) return false;

        String currentLocation = activeRow.getData().getDirEntry().getPath().toString();
        String moveToLocation = dir.getPath().toString();

        return currentLocation.equals(moveToLocation);
    }

    public DirEntry getDir() {
        return dir;
    }

    private boolean isValidFolderName(String name) {
        if (name == null || name.isBlank()) {
            return false;
        }
        if (name.matches(".*[\\\\/:*?\"<>|].*")) {
            return false;
        }
        try {
            Path.of(name); //to check if it is valid
            return true;
        } catch (InvalidPathException e) {
            return false;
        }
    }
}
