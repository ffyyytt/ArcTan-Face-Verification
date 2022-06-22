package com.example.demofaceidapp.detector.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import com.example.demofaceidapp.mtcnn.Box;
import java.io.File;
import java.util.Locale;

public class FaceUtils {

    private static final float outputW = 224;
    private static final float outputH = 224;
    private static final float scaleW = outputW / 112;
    private static final float scaleH = outputH / 112;

    private static Rect extendRect(Rect rect, float extendPercent) {
        int width = rect.width();
        int height = rect.height();
        rect.left -= width * extendPercent;
        rect.right += width * extendPercent;
        rect.top -= height * extendPercent;
        rect.bottom += height * extendPercent;
        return rect;
    }

    /**
     * @param bmp        input bitmap
     * @param contrast   0..10 1 is default
     * @param brightness -255..255 0 is default
     * @return new bitmap
     */
    public static Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness) {
        if (bmp == null) return null;
        ColorMatrix cm = new ColorMatrix(new float[]
                {
                        contrast, 0, 0, 0, brightness,
                        0, contrast, 0, 0, brightness,
                        0, 0, contrast, 0, brightness,
                        0, 0, 0, 1, 0
                });

        Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

        Canvas canvas = new Canvas(ret);

        Paint paint = new Paint();
        paint.setColorFilter(new ColorMatrixColorFilter(cm));
        canvas.drawBitmap(bmp, 0, 0, paint);

        return ret;
    }

    public static File getFaceKeyImageFile(Context context, int staffId, int faceId) {
        if (faceId == -1) {
            File faceFolder = getFaceKeyFolder(context);
            String[] children = faceFolder.list();
            if (children != null) {
                for (String child : children) {
                    try {
                        int id = Integer.parseInt(child.split("_")[0]);
                        if (staffId == id) {
                            return new File(faceFolder, child);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return new File(getFaceKeyFolder(context), String.format(Locale.US, "%d_%d.jpg", staffId, faceId));
    }

    public static File getFaceKeyFolder(Context context) {
        File file = new File(context.getFilesDir(), "face_image");
        if (!file.exists()) file.mkdir();
        return file;
    }

    public static void deleteAllFaceKey(Context context) {
        File faceKeyFolder = getFaceKeyFolder(context);
        String[] children = faceKeyFolder.list();
        if (children != null) {
            for (String child : children) {
                File imageFile = new File(faceKeyFolder, child);
                if (imageFile.exists()) {
                    imageFile.delete();
                }
            }
        }
    }

    public static void deleteFaceKey(Context context, int staffId) {
        File faceFolder = getFaceKeyFolder(context);
        String[] children = faceFolder.list();
        if (children != null) {
            for (String child : children) {
                try {
                    int id = Integer.parseInt(child.split("_")[0]);
                    if (staffId == id) {
                        File faceImageFile = new File(faceFolder, child);
                        if (faceImageFile.exists()) {
                            faceImageFile.delete();
                        }
                    }
                } catch (Exception ignored) {
                }
            }
        }
    }

    public static Bitmap cropEyeFromOri(Bitmap bitmap, Point eye, int eye_w, int eye_h) {
        return Bitmap.createBitmap(bitmap, eye.x - (eye_w / 2), eye.y - (eye_h / 2), eye_w, eye_h);
    }

    public static Bitmap rgbToGrayscale(Bitmap bmpOriginal)
    {
        int width, height;
        height = bmpOriginal.getHeight();
        width = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

}
