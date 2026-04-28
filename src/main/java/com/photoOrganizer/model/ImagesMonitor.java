package com.photoOrganizer.model;

import com.photoOrganizer.service.DirsScanner;
import com.photoOrganizer.Utils.Tools;
import javafx.beans.property.ObjectProperty;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static com.photoOrganizer.controller.MainPageController.log;

public class ImagesMonitor {
    private final ArrayList<ImageData> imagesList = new ArrayList<>(); //ordered by date. function scanAll() puts all files here
    private final TreeMap<String, List<ImageData>> imageMap = new TreeMap<>(); //ordered by date only. The source is imagesList
    private final TreeMap<Long, TreeMap<String, List<ImageData>>> mapBySize = new TreeMap<>(); //for duplicates search: size->file name->list of images
    private final Map<Path, DirEntry> dirMap = new TreeMap<>(Comparator.comparing(a -> a.toString().toLowerCase())); //Directories structure

    private final Config config;
    private DirEntry rootDir; //for the directory tree
    private ObjectProperty<DirEntry> selectedFolder;

    public ImagesMonitor(Config config, ObjectProperty<DirEntry> selectedFolder) {
        this.config = config;
        this.selectedFolder = selectedFolder;
    }

    public void clearAllData() {
        imagesList.clear();
        imageMap.clear();
        mapBySize.clear();
        dirMap.clear();
    }

    public void loadDirsAndImages(Consumer<String> progressCallback) {
        try {
            Path root = Path.of(config.getRootFolder());
            DirsScanner dirsScanner = new DirsScanner();
            rootDir = dirsScanner.scanAll(root, dirMap, imagesList, config, progressCallback);
            Tools.printSkipped(dirsScanner.skippedExts);
        } catch (Exception e) {
            log.error("Error while scanning images {}", config.getRootFolder(), e);
        }
    }

    public void buildData() {
        for (ImageData imageData : imagesList) {
            imageMap.computeIfAbsent(imageData.getDate(), k -> new ArrayList<>())
                    .add(imageData);
        }
        for (ImageData img : imagesList) {
            mapBySize.computeIfAbsent(img.size, k -> new TreeMap<>()).computeIfAbsent(img.getFileName(), k -> new ArrayList<>()).add(img);
        }

        imagesList.sort(Comparator.comparing(a -> a.dateTime));

        if (imagesList.isEmpty() || dirMap.size() <= 1) {
            Tools.showError("No files or subfolders");
            System.exit(-1);
        }
    }

    public void renameDirs(Path oldPath, Path newPath) {
        List<DirEntry> updates = new ArrayList<>();

        dirMap.forEach((k, v) -> {
            if (k.startsWith(oldPath)) {
                v.setPath(newPath.resolve(oldPath.relativize(k)));
                updates.add(v);
            }
        });

        dirMap.keySet().removeIf(path -> path.startsWith(oldPath));

        for (DirEntry dir : updates) {
            dirMap.put(dir.getPath(), dir);
        }
    }

    public TreeMap<Long, TreeMap<String, List<ImageData>>> getMapBySize() {
        return mapBySize;
    }

    public int getImagesListSize() {
        return imagesList.size();
    }

    public List<ImageData> getSameDateImages(String date) {
        return imageMap.get(date);
    }

    public DirEntry getRootDir() {
        return rootDir;
    }

    public int getFirstDuplicateIndex() {
        if (mapBySize.isEmpty()) {
            return -1;
        }
        if (selectedFolder.getValue() == null) {
            ImageData anyImage = mapBySize.firstEntry().getValue().firstEntry().getValue().getFirst(); //anything
            if (anyImage != null) {
                return imagesList.indexOf(anyImage);
            }
        } else {
            int i = 0;
            for (ImageData image : getCurrentImageList()) {
                TreeMap<String, List<ImageData>> sizeMap = mapBySize.get(image.size);
                if (sizeMap != null) {
                    List<ImageData> list = sizeMap.get(image.getFileName());
                    if (list != null) {
                        return i;
                    }
                }
                i++;
            }
        }
        return -1;
    }

    public int getFirstDateMatch(String date) {

        ArrayList<ImageData> source = getCurrentImageList();
        for (int i = 0; i < source.size(); i++) {
            if (source.get(i).dateTime.startsWith(date)) {
                return i;
            }
        }
        return -1;
    }

    public void deleteImage(ImageData imageData) {
        imageData.getDirEntry().files.remove(imageData.getFileName());
        imagesList.remove(imageData);
        List<ImageData> list = imageMap.get(imageData.getDate());
        list.remove(imageData);
    }

    public Map<String, DirEntry> getFoldersByText(String text) {
        text = text.toLowerCase();

        Map<String, DirEntry> map = new HashMap<>();
        for (Map.Entry<Path, DirEntry> entry: dirMap.entrySet()) {
            String key = entry.getKey().toString();
            if (key.toLowerCase().contains(text)) {
                map.put(key, entry.getValue());
            }
        }
        return map;
    }

    public ArrayList<ImageData> getCurrentImageList() {

        ArrayList<ImageData> source;
        if (selectedFolder.get() == null) {
            source = imagesList;
        } else {
            source = new ArrayList<>(selectedFolder.get().files.values());
            source.sort(Comparator.comparing(a -> a.dateTime));
        }

        return source;
    }


    public void printAllDirs() {
        int initLevel = rootDir.getPath().toString().split("\\\\").length; //do not print d:\Root
        printADir(rootDir, initLevel, initLevel);
    }

    public static void printADir(DirEntry dir, int level, int initLevel) {
        String[] paths = dir.getPath().toString().split("\\\\");

        if (level != initLevel &&  (!dir.subDirs.isEmpty() || !dir.files.isEmpty())) {
            String indent = " ".repeat(2 * (level - initLevel - 1));
            String files = dir.files.isEmpty() ? "" : "(" + dir.files.size() + ")";
            System.out.println(indent + paths[level - 1] + files);
        }

        for (DirEntry subdir : dir.subDirs) {
            printADir(subdir, level + 1, initLevel);
        }
    }
}
