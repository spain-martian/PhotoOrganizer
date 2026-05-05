package com.photoOrganizer.service;

import com.photoOrganizer.Utils.ImageMetaData;
import com.photoOrganizer.Utils.Tools;
import com.photoOrganizer.model.Config;
import com.photoOrganizer.model.DirEntry;
import com.photoOrganizer.model.ImageData;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

import static com.photoOrganizer.controller.MainPageController.log;

public class DirsScanner {
    private DirEntry rootDir;
    private int counter = 0;
    public final Map<String, Integer> skippedExts = new TreeMap<>();

    //returns root dir entry
    public DirEntry scanAll(
            Path rootPath,
            Map<Path, DirEntry> dirMap,
            ArrayList<ImageData> imageList,
            Config config,
            Consumer<String> progressCallback) throws IOException {

        Files.walkFileTree(rootPath, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult preVisitDirectory(Path dirPath, BasicFileAttributes attrs) {

                DirEntry parentDir = null;
                Path parentPath = dirPath.getParent();

                if (dirPath.compareTo(rootPath) != 0) {
                    if (parentPath == null) {
                        System.err.println("No Parent Path for " + dirPath);
                        System.exit(-2);
                        //must not occur
                    } else {
                        parentDir = dirMap.get(parentPath);
                    }
                }//root must have no parent

                if (config.isAllowedFolder(dirPath.toString())) {
                    DirEntry dir = new DirEntry(dirPath, parentDir);
                    if (parentDir != null) {
                        parentDir.subDirs.add(dir);
                    } else {
                        if (rootDir == null) {
                            rootDir = dir;
                        }
                    }
                    dirMap.put(dirPath, dir);
                } else {
                    return FileVisitResult.SKIP_SUBTREE;
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path filePath, BasicFileAttributes attrs) {

                String name = filePath.toString().toLowerCase();
                if (attrs.size() > 0 && config.isAllowedType(name)) {
                    DirEntry dirEntry = dirMap.get(filePath.getParent());

                    if (dirEntry == null) {
                        log.error("DIR NULL {}", filePath.getParent());
                        //The folder is not correct
                    } else {
                        try {
                            String created = fileDateToString(attrs.creationTime());
                            String modified = fileDateToString(attrs.lastModifiedTime());
                            String time = created.compareTo(modified) > 0 ? modified : created; //there is no reliable date here

                            String dateFromName = Tools.tryToGetDateFromName(filePath.getFileName().toString());
                            if (dateFromName != null) {
                                //sometimes the name contains the real creation time
                                if (time.compareTo(dateFromName) > 0) {
                                    if (!dateFromName.substring(0, 10).equals(time.substring(0, 10))) { //if the dates differ
                                        time = dateFromName;
                                    }
                                }
                            }
                            ImageData imageData = ImageMetaData.extractMetadata(filePath.toFile(), attrs.size(), time, dirEntry, config);
                            dirEntry.files.put(imageData.getFileName(), imageData);
                            imageList.add(imageData);
                            if (++counter % 100 == 0) {
                                progressCallback.accept(counter + "...");
                            }
                        } catch (Exception e) {
                            log.error("Failed to extract meta data from {}", filePath, e);
                        }
                    }
                } else {
                    int dot = name.lastIndexOf('.');
                    if (dot > 0) {
                        String ext = name.substring(dot + 1);
                        skippedExts.merge(ext, 1, Integer::sum);
                    }
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return rootDir;
    }

    private String fileDateToString(FileTime time) {
        return time.toString()
                .replace("'", "")
                .replace("T", " ")
                .replace("Z", "")
                .split("\\.")[0]; //remove fractions from the time
    }
}
