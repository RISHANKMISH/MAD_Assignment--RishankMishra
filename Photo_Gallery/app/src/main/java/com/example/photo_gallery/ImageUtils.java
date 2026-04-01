package com.example.photo_gallery;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ImageUtils {

    public static List<ImageItem> getImagesInFolder(String folderPath) {
        List<ImageItem> images = new ArrayList<>();

        // If it's a content URI (SAF), handle it differently
        if (folderPath != null && folderPath.startsWith("content://")) {
            return images; // Cannot access via File API
        }

        File folder = new File(folderPath);

        if (!folder.exists() || !folder.isDirectory()) {
            return images;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return images;
        }

        for (File file : files) {
            if (isImageFile(file)) {
                ImageItem item = new ImageItem(
                        file.getAbsolutePath(),
                        file.getName(),
                        file.length(),
                        file.lastModified()
                );
                item.setDateModified(file.lastModified());
                images.add(item);
            }
        }

        return images;
    }

    public static List<ImageItem> getImagesFromTreeUri(Context context, Uri treeUri) {
        List<ImageItem> images = new ArrayList<>();

        if (treeUri == null) {
            return images;
        }

        ContentResolver resolver = context.getContentResolver();
        String treeDocumentId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocumentId);

        String[] projection = new String[] {
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_SIZE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        };

        try (Cursor cursor = resolver.query(childrenUri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String documentId = cursor.getString(0);
                    String name = cursor.getString(1);
                    long size = cursor.getLong(2);
                    long lastModified = cursor.getLong(3);
                    String mimeType = cursor.getString(4);

                    if (mimeType != null && mimeType.startsWith("image/")) {
                        Uri documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId);
                        ImageItem item = new ImageItem(
                                documentUri.toString(),
                                name,
                                size,
                                lastModified
                        );
                        item.setDateModified(lastModified);
                        images.add(item);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return images;
    }

    public static int getImageCountInFolder(String folderPath) {
        return getImagesInFolder(folderPath).size();
    }

    public static boolean isImageFile(File file) {
        if (!file.isFile()) {
            return false;
        }

        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") ||
               name.endsWith(".png") || name.endsWith(".gif") ||
               name.endsWith(".bmp") || name.endsWith(".webp");
    }

    public static String formatFileSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.2f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    public static Uri getContentUriFromFile(Context context, String filePath) {
        Uri contentUri = null;
        ContentResolver contentResolver = context.getContentResolver();

        String[] projection = { MediaStore.Images.Media._ID };
        String selection = MediaStore.Images.Media.DATA + "=?";
        String[] selectionArgs = new String[] { filePath };

        Cursor cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
            contentUri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
            );
            cursor.close();
        }

        return contentUri;
    }
}
