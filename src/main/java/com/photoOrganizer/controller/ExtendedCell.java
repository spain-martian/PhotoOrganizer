package com.photoOrganizer.controller;

import javafx.beans.property.ObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

class ExtendedCell extends ListCell<String> {
    private final HBox box;
    private final Label label;
    private final TextField editor = new TextField();

    public ExtendedCell(ObjectProperty<ExtendedCell> editedCell, boolean editNotAllowed) {
        box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPrefWidth(80);

        editingProperty().addListener(e -> editedCell.setValue(editingProperty().get() ? this : null));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button deleteBtn = new Button("");
        deleteBtn.getStyleClass().add("list-del-button");

        label = new Label();
        label.getStyleClass().add("list-label");

        box.getStyleClass().add("list-container");
        box.getChildren().addAll(label, spacer, deleteBtn);

        deleteBtn.setFocusTraversable(false);  //to avoid having 2 actions simultaneously: deleting and gaining focus
        deleteBtn.setOnAction(e -> {
            if (editNotAllowed) return;
            ListView<String> list = getListView();
            if (list != null && getIndex() >= 0) {
                list.getItems().remove(getIndex());
            }
        });

        editor.focusedProperty().addListener((obs, old, now) -> {
            if (!now && isEditing()) {
                commitEdit(editor.getText());
            }
        });

        //Cancel on ESC
        editor.setOnKeyPressed(e -> {
            switch (e.getCode()) {
                case ESCAPE -> cancelEdit();
            }
        });

        editingProperty().addListener((obs, wasEditing, isNowEditing) -> {
            if (wasEditing && !isNowEditing) {
                commitEdit(editor.getText());
            }
        });
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);

        if (empty || item == null) {
            setGraphic(null);
        } else if (isEditing()) {
            editor.setText(item);
            setGraphic(editor);
        } else {
            label.setText(item);
            setGraphic(box);
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();

        editor.setText(getItem());
        editor.setMaxWidth(70);
        setGraphic(editor);
        editor.requestFocus();
        editor.selectAll();
    }

    @Override
    public void commitEdit(String newValue) {
        String oldValue = getItem();

        if (newValue == null || newValue.isBlank()) {
            newValue = oldValue;
        } else {
            newValue = newValue.trim().toLowerCase().replaceFirst("^\\.", "");
        }

        //do nothing if unchanged
        if (newValue.equals(oldValue)) {
            cancelEdit();
            return;
        }

        getListView().getItems().set(getIndex(), newValue);
        super.commitEdit(newValue);
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        setGraphic(box);
    }
}