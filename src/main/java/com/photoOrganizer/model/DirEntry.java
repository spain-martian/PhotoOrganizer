package com.photoOrganizer.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class DirEntry {
    private Path path;
    public final DirEntry parentDir;
    public final List<DirEntry> subDirs = new ArrayList<>();
    public final Map<String, ImageData> files = new TreeMap<>(); //sorted by name

    public DirEntry(Path path, DirEntry parentDir) {
        this.path = path;
        this.parentDir = parentDir;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getShortName() {
        return toShortName(path.toString());
    }

    public static String toShortName(String name) {
        int i = name.lastIndexOf("\\");
        if (i > 0) name = name.substring(i);
        return name;
    }

    @Override
    public String toString() {
        return "DirEntry{" +
                "path=" + path +
                ", parentDir=" + (parentDir == null ? "NULL" : parentDir.getPath().toString()) +
                ", subDirs=" + subDirs.size() +
                ", files=" + files.size() +
                '}';
    }
}