package com.photoOrganizer.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Config{
    private static final Logger log = LoggerFactory.getLogger(Config.class);
    private String rootFolder;
    private String unsortedFolder;
    private String startDate;
    private Set<String> imageTypes;
    private Set<String> videoTypes;
    private Set<String> excludedFolders;

    public Config() {
    }

    public void setRootFolder(String rootFolder) {
        this.rootFolder = rootFolder;
    }

    public Config(String rootFolder, String unsortedFolder, String startDate, Set<String> imageTypes, Set<String> videoTypes, Set<String> excludedFolders) {
        this.rootFolder = rootFolder;
        this.unsortedFolder = unsortedFolder;
        this.startDate = startDate;
        this.imageTypes = imageTypes;
        this.videoTypes = videoTypes;
        this.excludedFolders = excludedFolders;
    }

    public boolean isAllowedType(String name) {
        int dot = name.lastIndexOf('.');
        if (dot == -1) return false;

        String ext = name.substring(dot + 1).toLowerCase();
        return imageTypes.contains(ext) || videoTypes.contains(ext);
    }

    public boolean isAllowedFolder(String folder) {
        for (String excluded: excludedFolders) {
            if (folder.toLowerCase().endsWith("\\" + excluded.toLowerCase())) return false;
        }
        return true;
    }

    public static Config loadConfig() {
        Path configPath = getPath();
        Config config;

        try {
            if (Files.exists(configPath)) {
                return new ObjectMapper().readValue(configPath.toFile(), Config.class);
            } else {
                ObjectMapper mapper = new ObjectMapper();
                File configFile = new File(".\\src\\main\\resources\\config.json");
                config = mapper.readValue(configFile, Config.class);
            }
        } catch (Exception e) {
            log.warn("Config file not found, created default", e);
            config = new Config(
                    "C:\\Photo",
                    "*unsorted*",
                    "",
                    new TreeSet<>(List.of("jpg", "jpeg", "png", "bmp")),
                    new TreeSet<>(List.of("mp4")),
                    new TreeSet<>(List.of("Edited", "Camera"))
            );
        }
        config.save(); //it doesn't exist yet

        return config;
    }

    public void save() {
        Path configPath = getPath();
        try {
            Files.createDirectories(configPath.getParent());
            new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(configPath.toFile(), this);
        } catch (Exception e) {
            log.error("Failed to save config file", e);
        }
    }

    private static Path getPath() {
        return Path.of(System.getProperty("user.home"), ".photoOrganizer", "config.json");
    }

    public String findConfigErrors() {
        String error = findRootError(rootFolder);
        if (!error.isEmpty()) return error;

        return "";
    }

    public static String findRootError(String rootFolder) {
        Path path;
        try {
            path = Path.of(rootFolder);
        } catch (InvalidPathException e) {
            return "Invalid root folder: " + rootFolder;
        }

        path = path.normalize();

        String p = path.toString();
        if (p.matches("^[a-zA-Z]:$") || path.getParent() == null) {  // D: is not enough
            return "The root must be a folder: " + rootFolder + "\nSelect the root folder using the settings button.";
        }

        if (!Files.isDirectory(path)) {// Must exist and be a directory
            return "The root must be a existing folder: " + rootFolder + "\nSelect the root folder using the settings button.";
        }
        return "";
    }

    public boolean isFolderUnsorted(String fullName) {
        String name = fullName;
        int i = fullName.lastIndexOf("\\");
        if (i >= 0) name = fullName.substring(i + 1);

        name = name.toLowerCase();
        if (unsortedFolder.startsWith("*")) {
            if (unsortedFolder.endsWith("*")) {
                return name.contains(unsortedFolder.substring(1, unsortedFolder.length() - 1));
            } else {
                return name.endsWith(unsortedFolder.substring(1));
            }
        } else {
            if (unsortedFolder.endsWith("*")) {
                return name.startsWith(unsortedFolder.substring(0, unsortedFolder.length() - 1));
            } else {
                return name.equals(unsortedFolder);
            }
        }
    }

    public boolean isVideo(String path) {
        path = path.toLowerCase();
        int dot = path.lastIndexOf('.');
        if (dot == -1) return false;

        String ext = path.substring(dot + 1).toLowerCase();
        return videoTypes.contains(ext);
    }

    public String getRootFolder() {
        return rootFolder;
    }

    public String getUnsortedFolder() {
        return unsortedFolder;
    }

    public String getStartDate() {
        return startDate;
    }

    public Set<String> getImageTypes() {
        return imageTypes;
    }

    public Set<String> getVideoTypes() {
        return videoTypes;
    }

    public Set<String> getExcludedFolders() {
        return excludedFolders;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public void setUnsortedFolder(String unsortedFolder) {
        this.unsortedFolder = unsortedFolder;
    }
}

