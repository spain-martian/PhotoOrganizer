package com.photoOrganizer.Utils;

import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.drew.metadata.mp4.Mp4Directory;
import com.photoOrganizer.model.Config;
import com.photoOrganizer.model.DirEntry;
import com.photoOrganizer.model.ImageData;

import java.io.File;

public class ImageMetaData {

    public static ImageData extractMetadata(File file, long size, String creationDateTime, DirEntry dirEntry, Config config) throws Exception {
        Metadata metadata = ImageMetadataReader.readMetadata(file);
        String lat = "";
        String lon = "";

        if (config.isVideo(file.getAbsolutePath())) {
            Mp4Directory mp4 = metadata.getFirstDirectoryOfType(Mp4Directory.class);

            if (mp4 != null) {
                Double dlat = mp4.getDoubleObject(Mp4Directory.TAG_LATITUDE);
                Double dlon = mp4.getDoubleObject(Mp4Directory.TAG_LONGITUDE);
                if (dlat != null || dlon != null) {
                    lat = String.format("%.8f", dlat);
                    lon = String.format("%.8f", dlon);
                }
            }
        }

        if (lat.isEmpty() || lon.isEmpty()) {
            GpsDirectory gps = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gps != null) {
                GeoLocation location = gps.getGeoLocation();
                if (location != null) {
                    lat = String.format("%.8f", location.getLatitude());
                    lon = String.format("%.8f", location.getLongitude());
                }
            }
        }

        ExifSubIFDDirectory exif = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

        if (exif != null) {
            String exifDate = exif.getString(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            if (exifDate != null) {
                if (!exifDate.startsWith("0000")) {
                    if (exifDate.charAt(4) == ':' && exifDate.charAt(7) == ':' ) {
                        exifDate = exifDate.substring(0, 4) + "-" + exifDate.substring(5, 7) + "-" + exifDate.substring(8);
                    }
                    creationDateTime = exifDate;
                }
            }
        }

        return new ImageData(file.getName(), size, creationDateTime, lat, lon, dirEntry);
    }
}

