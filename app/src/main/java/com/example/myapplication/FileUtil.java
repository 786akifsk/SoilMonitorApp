package com.example.myapplication;


import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class FileUtil {
    public static File from(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new IOException("Unable to open input stream for URI: " + uri);
        }
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);

        String fileName = "temp_file";
        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1 && cursor.moveToFirst()) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }

        File file = new File(context.getCacheDir(), fileName);
        FileOutputStream outputStream = new FileOutputStream(file);
        byte[] buffers = new byte[1024];
        int read;
        while ((read = inputStream.read(buffers)) != -1) {
            outputStream.write(buffers, 0, read);
        }

        inputStream.close();
        outputStream.close();
        if (file.length() == 0) {
            throw new IOException("Generated file is empty: " + file.getAbsolutePath());
        }
        return file;
    }
}

