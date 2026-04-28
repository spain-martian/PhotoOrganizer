package com.photoOrganizer.Utils;

import com.photoOrganizer.controller.MainPageController;
import com.photoOrganizer.model.DirEntry;
import com.photoOrganizer.model.ImageData;
import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Tools {

    public static void printAllDirs(Map<Path, DirEntry> dirMap) {
        for (DirEntry entry: dirMap.values()) {
            System.out.println(entry);
        }
    }

    public static void printAllFiles(TreeMap<String, List<ImageData>> imageMap) {
        for (String date: imageMap.keySet()) {
            System.out.println(date + ":");
            for (ImageData entry: imageMap.get(date)) {
                System.out.println(entry);
            }
        }
    }

    public static void printSkipped(Map<String, Integer> extMap) {
        if (!extMap.isEmpty()) {
            MainPageController.log.info("Skipped files with extensions:");
            int sum = 0;
            for (Map.Entry<String, Integer> entry: extMap.entrySet()) {
                sum += entry.getValue();
                MainPageController.log.info("{}: {}", entry.getKey(), entry.getValue());
            }
            MainPageController.log.info("Total skipped files: {}", sum);
        }
    }

    public static void printSizeMap(Map<Long, TreeMap<String, List<ImageData>>> bySize) {
        System.out.println("--------- By Size map --------------" + bySize.size());
        bySize.forEach((key, map) -> {
            System.out.println("Size " + key + " entries " + map.size());
            map.forEach((name, list) -> {
                System.out.println("Name " + name + " list " + list.size());
            });
        });
    }

    public static String tryToGetDateFromName(String fileName) {
        String name = removeExtension(fileName);
        String mask = name.replaceAll("[0-9]", "*");

        String dateTime = null;
        if (mask.equals("IMG-********-WA****") || mask.equals("VID-********-WA****")) {
            name = name.substring(4, 12) + "_000001"; //date only here
            mask = name.replaceAll("[0-9]", "*");
        } else {
            if (mask.equals("IMG_********_******") || mask.equals("VID_********_******")) {
                name = name.substring(4);
                mask = name.replaceAll("[0-9]", "*");
            }
        }

        if (mask.equals("********_******")) {
            dateTime = name.substring(0, 4) + "-" + name.substring(4, 6) + "-" + name.substring(6, 8) + " " +
                    name.substring(9, 11) + ":" + name.substring(11, 13) + ":" + name.substring(13, 15);
        } else {
            if (mask.equals("****-**-** **.**.**")) {
                dateTime = name.replace(".", ":");
            } else {
                if (mask.startsWith("p********")) {
                    dateTime = name.substring(5, 9) + "-" + name.substring(3, 5) + "-" + name.substring(1, 3) + " 00:00:01";
                }
            }
        }

        if (dateTime != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm:ss").withResolverStyle(ResolverStyle.STRICT);
            try {
                LocalDateTime localDate = LocalDateTime.parse(dateTime, formatter);
//                System.out.println(fileName + " -> " + localDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            } catch (DateTimeException ignore) {
                dateTime = null;
            }
        }

        return dateTime;
    }

    public static String removeExtension(String fileName) {
        int index = fileName.lastIndexOf(".");
        if (index >= 0) return fileName.substring(0, index);
        return fileName;
    }

    public static void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText("Error");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
