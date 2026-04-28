package com.photoOrganizer.controller;

import com.photoOrganizer.model.Config;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import static com.photoOrganizer.controller.MainPageController.log;


public class SettingsController {

    @FXML
    private TextField imgExtField;
    @FXML
    private TextField videoExtField;
    @FXML
    private TextField unsortedMaskField;
    @FXML
    private TextField startDateField;
    @FXML
    private TextField excludedListField;
    @FXML
    private ListView<String> imgExtList;
    @FXML
    private ListView<String> videoExtList;
    @FXML
    private ListView<String> excludedList;
    @FXML
    private Button imgExtAddButton;
    @FXML
    private Button imgExtSaveButton;
    @FXML
    private Button videoExtAddButton;
    @FXML
    private Button videoExtSaveButton;
    @FXML
    private Button excludedAddButton;
    @FXML
    private Button excludedSaveButton;
    @FXML
    private Button chooserButton;
    @FXML
    private Button startDateButton;
    @FXML
    private Button unsortedButton;
    @FXML
    private Button reloadButton;
    @FXML
    private Label warningLabel;
    @FXML
    private Label rootLabel;
    @FXML
    private Label startLabel;
    @FXML
    private Label unsortedLabel;

    private MainPageController mainPageController;
    private final DirectoryChooser directoryChooser = new DirectoryChooser();
    private Runnable saveFun;
    private Config config;

    public void initialize() {
        directoryChooser.setInitialDirectory(new File("\\"));
        directoryChooser.setTitle("Choose a folder");
    }

    public void setData(Config config, boolean started, MainPageController mainPageController) {
        this.mainPageController = mainPageController;
        this.config = config;
        saveFun = started ? null : config::save;

        new EditListUI(config.getImageTypes(), imgExtList, imgExtField, imgExtAddButton, imgExtSaveButton, this::correctExt, saveFun);
        new EditListUI(config.getVideoTypes(), videoExtList, videoExtField, videoExtAddButton, videoExtSaveButton, this::correctExt, saveFun);
        new EditListUI(config.getExcludedFolders(), excludedList, excludedListField, excludedAddButton, excludedSaveButton, this::correctExcluded, saveFun);
        resetRootLabel(config.getRootFolder());
        resetStartDateLabel(config.getStartDate());
        resetUnsortedLabel(config.getUnsortedFolder());

        if (started) {
            disableAll();
            reloadButton.setDisable(false);
        } else {
            reloadButton.setDisable(true);
        }
    }

    private String correctExt(String text) {
        return text.trim().toLowerCase().replaceFirst("^\\.", "");
    }

    private String correctExcluded(String text) {
        return text.trim().toLowerCase().replaceFirst("^\\\\+", "").replace("\\\\", "\\");
    }

    @FXML
    private void onSaveImgExt() {
        //The changes are saved when the field loses focus. Then the button is disabled
    }

    @FXML
    private void onSaveVideoExt() {
        //The changes are saved when the field loses focus. Then the button is disabled
    }

    @FXML
    private void onSaveExcluded() {
        //The changes are saved when the field loses focus. Then the button is disabled
    }

    @FXML
    private void onAddStartDate() {
        String date = correctDate(startDateField.getText());
        if (date.startsWith("Invalid")) {
            startDateField.setText(date);
            return;
        }
        resetStartDateLabel(date);
        startDateField.setText("");
        config.setStartDate(date);
        saveFun.run();
    }

    private String correctDate(String input) {
        int[] nums = Pattern.compile("\\d+")
                .matcher(input)
                .results()
                .map(MatchResult::group)
                .mapToInt(Integer::parseInt)
                .limit(3)
                .toArray();
        if (nums.length == 0) { //valid result
            return "";
        }

        int y = nums[0];
        int m = nums.length > 1 ? nums[1] : 0;
        int d = nums.length > 2 ? nums[2] : 0;
        String date = String.format("%04d-%02d-%02d", y, m, d);

        try {
            LocalDate localDate = LocalDate.of(y, m, d);
            if (y > 1990) {
                return localDate.toString();
            }
        } catch (DateTimeException ignore) {
        }
        return "Invalid date: " + date;
    }

    private void resetStartDateLabel(String text) {
        startLabel.setText("Starting date for the search: " + (text.isEmpty() ? "Not set" : text));
    }

    private void resetUnsortedLabel(String text) {
        unsortedLabel.setText("Mask for the 'unsorted' folders: " + (text.isEmpty() ? "Not set" : text));
    }

    @FXML
    private void onAddUnsorted() {
        String mask = correctUnsortedFolder(unsortedMaskField.getText());
        resetUnsortedLabel(mask);
        unsortedMaskField.setText("");
        config.setUnsortedFolder(mask);
        saveFun.run();
    }

    private String correctUnsortedFolder(String text) {//Asterisks are allowed at the beginning and end of the text
        if (text.length() <= 2) return text;

        return text.charAt(0)
                + text.substring(1, text.length() - 1).replace("*", "")
                + text.charAt(text.length() - 1);
    }

    private void resetRootLabel(String text) {
        rootLabel.setText(text);
    }

    @FXML
    private void onChangeRoot() {
        File selectedDir = directoryChooser.showDialog(null);
        if (selectedDir == null) return;
        String root = selectedDir.getAbsolutePath();

        config.setRootFolder(root);
        saveFun.run();
        resetRootLabel(root);
    }

    @FXML
    private void closeAction() {
        Stage stage = (Stage) imgExtField.getScene().getWindow();
        stage.close();
    }

    private void disableAll() {
        warningLabel.setManaged(true);
        imgExtList.setEditable(false);
        imgExtField.setDisable(true);
        videoExtList.setEditable(false);
        videoExtField.setDisable(true);
        excludedList.setEditable(false);
        excludedListField.setDisable(true);
        startDateField.setDisable(true);
        unsortedMaskField.setDisable(true);
        chooserButton.setDisable(true);
        startDateButton.setDisable(true);
        unsortedButton.setDisable(true);
    }


    @FXML
    private void reloadAction() {
        closeAction();
        mainPageController.fullReset();
    }
}

/*
    It is designed for three ListView sections that have similar functionality.
 */
class EditListUI {
    private final ObjectProperty<ExtendedCell> editedCell = new SimpleObjectProperty<>(null);
    private final Collection<String> source;
    private final ObservableList<String> observableList;
    private final TextField addField;
    private final Function<String, String> correct;
    private final Runnable save;

    public EditListUI(Collection<String> source,
                      ListView<String> itemsViewList,
                      TextField addField,
                      Button addButton,
                      Button saveButton,
                      Function<String, String> correct,
                      Runnable save) {
        this.source = source;
        this.addField = addField;
        this.correct = correct;
        this.save = save;

        observableList = FXCollections.observableArrayList(source);
        observableList.sort(null); //   observableList.sort(String::compareTo);
        itemsViewList.setItems(observableList);
        itemsViewList.setEditable(true);
        itemsViewList.setCellFactory(list -> new ExtendedCell(editedCell, save == null));
        itemsViewList.setOnMouseClicked(e -> {
            if (e.getClickCount() == 1) {
                int index = itemsViewList.getSelectionModel().getSelectedIndex();
                if (index >= 0) {
                    itemsViewList.edit(index);
                }
            }
        });

        observableList.addListener(this::saveChanges);

        addField.textProperty().addListener(e -> {
            String text = correct.apply(addField.getText());
            addButton.setDisable(text.isEmpty() || observableList.contains(text));
        });

        editedCell.addListener(v -> {
            saveButton.setDisable(editedCell.get() == null);
        });

        addField.setOnAction(e -> onAdd());
        addButton.setOnAction(e -> onAdd());
    }

    private void saveChanges(ListChangeListener.Change<? extends String> change) {
        while (change.next()) {
            if (change.wasAdded()) {
                source.add(change.getAddedSubList().getFirst());
                save.run();
            }
            if (change.wasRemoved()) {
                source.remove(change.getRemoved().getFirst());
                save.run();
            }
            if (change.wasUpdated()) {
                log.warn("Impossible Updated Change {}", change.getFrom());
            }
            if (change.wasPermutated()) {
                log.warn("Impossible Permutated Change");
            }
        }
    }

    @FXML
    private void onAdd() {
        String text = correct.apply(addField.getText());

        if (!text.isEmpty()) {
            if (!observableList.contains(text)) {
                observableList.add(text);
            }
            addField.clear();
        }
    }
}