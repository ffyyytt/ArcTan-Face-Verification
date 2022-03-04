package com.example.faceverification;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.ExifInterface;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class Utils {
    public static Bitmap loadImage(String path) throws IOException {
        File file = new File(path);
        Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
        bitmap = modifyOrientation(bitmap, path);
        return bitmap;
    }

    public static Bitmap resizeKeepRation(Bitmap targetBmp,int reqHeightInPixels,int reqWidthInPixels)
    {
        Matrix matrix = new Matrix();
        matrix.setRectToRect(new RectF(0, 0, targetBmp.getWidth(), targetBmp.getHeight()), new RectF(0, 0, reqWidthInPixels, reqHeightInPixels), Matrix.ScaleToFit.CENTER);
        Bitmap scaledBitmap = Bitmap.createBitmap(targetBmp, 0, 0, targetBmp.getWidth(), targetBmp.getHeight(), matrix, true);
        return scaledBitmap;
    }

    public static Bitmap modifyOrientation(Bitmap bitmap, String image_absolute_path) throws IOException {
        ExifInterface ei = new ExifInterface(image_absolute_path);
        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotate(bitmap, 90);

            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotate(bitmap, 180);

            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotate(bitmap, 270);

            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                return flip(bitmap, true, false);

            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                return flip(bitmap, false, true);

            default:
                return bitmap;
        }
    }

    public static Bitmap rotate(Bitmap bitmap, float degrees) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap flip(Bitmap bitmap, boolean horizontal, boolean vertical) {
        Matrix matrix = new Matrix();
        matrix.preScale(horizontal ? -1 : 1, vertical ? -1 : 1);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}
