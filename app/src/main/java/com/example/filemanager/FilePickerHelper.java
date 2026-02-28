package com.example.filemanager;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FilePickerHelper {
    
    public static File getFileFromUri(Context context, Uri uri) throws Exception {
        String fileName = getFileName(context, uri);
        File tempFile = new File(context.getCacheDir(), fileName);
        
        try (InputStream inputStream = context.getContentResolver().openInputStream(uri);
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile;
    }
    
    private static String getFileName(Context context, Uri uri) {
        String fileName = null;
        
        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            }
        }
        
        if (fileName == null) {
            fileName = uri.getPath();
            if (fileName != null) {
                int cut = fileName.lastIndexOf('/');
                if (cut != -1) {
                    fileName = fileName.substring(cut + 1);
                }
            }
        }
        
        if (fileName == null || fileName.isEmpty()) {
            fileName = "file_" + System.currentTimeMillis();
            String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                context.getContentResolver().getType(uri));
            if (extension != null) {
                fileName += "." + extension;
            }
        }
        
        return fileName;
    }
}