package com.photoOrganizer.service;

import com.photoOrganizer.model.ImageData;

import java.util.*;

public class DuplicateService {

    private final Map<Long, TreeMap<String, List<ImageData>>> mapBySize;

    public DuplicateService(Map<Long, TreeMap<String, List<ImageData>>> mapBySize) {
        this.mapBySize = mapBySize;
    }

    public void normalize() { //Remove uniques, leave only duplicates
        mapBySize.entrySet().removeIf(entry -> {
            entry.getValue().entrySet().removeIf(e -> e.getValue().size() < 2);
            return entry.getValue().isEmpty();
        });
//        Tools.printSizeMap(bySize);
    }

    public void updateVisibleImages(ArrayList<ImageData> newVisible) {
        for (ImageData img : newVisible) {
            if (!img.hasDuplicatesComputed()) {
                createDuplicates(img);
            }
        }
    }

    private void createDuplicates(ImageData image) {
        List<ImageData> duplicates = new ArrayList<>();
        Map<String, List<ImageData>> sameSizeImages = mapBySize.get(image.size);
        if (sameSizeImages != null) {
            List<ImageData> sameSizeAndNameImages = sameSizeImages.get(image.getFileName());
            if (sameSizeAndNameImages != null) {
                for (ImageData img : sameSizeAndNameImages) {
                    if (img != image) {
                        duplicates.add(img);
                    }
                }
            }
        }
        image.setDuplicates(duplicates);
    }

    public void removeImageData(ImageData imageData) {
        Map<String, List<ImageData>> byName = mapBySize.get(imageData.size);
        if (byName != null) {
            List<ImageData> list = byName.get(imageData.getPath().toString());
            if (list != null) {
                list.remove(imageData);
                if (list.size() < 2) {
                    byName.remove(imageData.getFileName());
                    if (byName.isEmpty()) {
                        mapBySize.remove(imageData.size);
                    }
                }
            }
        }
        removeMeFromMyDuplicates(imageData);
    }

    private void removeMeFromMyDuplicates(ImageData imageData) {
        List<ImageData> dups = imageData.getDuplicates();
        if (dups != null) {
            for (ImageData dup : dups) {
                dup.removeDuplicate(imageData);
            }
        }
    }
}