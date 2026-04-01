package com.example.photo_gallery;

public class ImageItem {
    private String path;
    private String name;
    private long size;
    private long dateTaken;
    private long dateModified;

    public ImageItem(String path, String name, long size, long dateTaken) {
        this.path = path;
        this.name = name;
        this.size = size;
        this.dateTaken = dateTaken;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public long getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(long dateTaken) {
        this.dateTaken = dateTaken;
    }

    public long getDateModified() {
        return dateModified;
    }

    public void setDateModified(long dateModified) {
        this.dateModified = dateModified;
    }
}
