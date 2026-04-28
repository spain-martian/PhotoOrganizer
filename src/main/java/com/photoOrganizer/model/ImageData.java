package com.photoOrganizer.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;
import java.util.List;

public class ImageData {
    private String fileName;
    public final long size;
    public final String dateTime;  //yyyy-MM-dd HH:mm:ss
    public final String gpsLat; //number or empty
    public final String gpsLon; //number or empty
    private DirEntry dirEntry;

    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private final ObjectProperty<List<ImageData>> duplicatesList = new SimpleObjectProperty<>(null);

    public ImageData(String fileName, long size, String dateTime, String gpsLat, String gpsLon, DirEntry dirEntry) {
        this.fileName = fileName;
        this.size = size;
        this.dateTime = dateTime;
        this.gpsLat = gpsLat;
        this.gpsLon = gpsLon;
        this.dirEntry = dirEntry;
    }

    public ObjectProperty<List<ImageData>> duplicatesProperty() {
        return duplicatesList;
    }

    public List<ImageData> getDuplicates() {
        return duplicatesList.get();
    }

    public void setDuplicates(List<ImageData> value) {
        duplicatesList.set(value);
    }

    public boolean hasDuplicatesComputed() {
        return getDuplicates() != null;
    }

    public void removeDuplicate(ImageData imageData) {
        if (hasDuplicatesComputed()) {
            getDuplicates().remove(imageData);
        }
    }

    public BooleanProperty loadingProperty() {
        return loading;
    }

    public String getDate() {
        return dateTime.substring(0, 10);
    }

    public String getMapsLink() {
        if (gpsLat.isEmpty() || gpsLon.isEmpty()) return "";

        //https://www.google.com/maps?q=40.416775,-3.703790
        return "https://www.google.com/maps?q=" + gpsLat + "," + gpsLon;
    }

    public DirEntry getDirEntry() {
        return dirEntry;
    }

    public void setDirEntry(DirEntry dirEntry) {
        this.dirEntry = dirEntry;
    }

    public Path getPath() {
        return dirEntry.getPath();
    }

    public void setFileName(String name) {
        fileName = name;
    }

    public String getFileName() {
        return fileName;
    }

    @Override
    public String toString() {
        return "ImageData{" +
                "fileName='" + fileName + '\'' +
                ", dateTime='" + dateTime + '\'' +
                ", gpsLat='" + gpsLat + '\'' +
                ", gpsLon='" + gpsLon + '\'' +
                ", dirEntry=" + dirEntry +
                '}';
    }
}