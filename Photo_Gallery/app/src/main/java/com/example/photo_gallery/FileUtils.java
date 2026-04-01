package com.example.photo_gallery;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.os.Build;

public class FileUtils {

    public static String getPathFromUri(Context context, Uri uri) {
        String path = null;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (DocumentsContract.isDocumentUri(context, uri)) {
                String docId = DocumentsContract.getTreeDocumentId(uri);
                String[] split = docId.split(":");

                if (split.length >= 2) {
                    String type = split[0];
                    String relativePath = split[1];

                    if ("primary".equalsIgnoreCase(type)) {
                        path = "/storage/emulated/0/" + relativePath;
                    } else {
                        path = "/storage/" + type + "/" + relativePath;
                    }
                } else if (split.length == 1) {
                    path = "/storage/emulated/0/" + docId;
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                path = getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                path = uri.getPath();
            }
        } else {
            path = getDataColumn(context, uri, null, null);
        }

        return path;
    }

    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {
        String column = "_data";
        String[] projection = { column };

        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection,
                selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
