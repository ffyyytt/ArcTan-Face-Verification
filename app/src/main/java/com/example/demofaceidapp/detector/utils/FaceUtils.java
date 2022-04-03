package com.example.demofaceidapp.detector.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;

import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FaceUtils {

    public static RectF getRectOfContour(FaceContour contour) {
        List<PointF> pts = contour.getPoints();
        RectF rect = new RectF(Float.MAX_VALUE, Float.MAX_VALUE, 0, 0);
        for (PointF p : pts) {
            rect.left = Math.min(rect.left, p.x);
            rect.top = Math.min(rect.top, p.y);
            rect.right = Math.max(rect.right, p.x);
            rect.bottom = Math.max(rect.bottom, p.y);
        }
        return rect;
    }

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

    public static Bitmap cropFaceFromContour(Bitmap frame, Face face, boolean useContour, boolean grayScale) {
        FaceContour contour = face.getContour(FaceContour.FACE);
        if (contour != null && useContour) {
            RectF rect = getRectOfContour(contour);

            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(0XFF000000);

            Path path = new Path();
            boolean first = true;
            for (PointF p : contour.getPoints()) {
                if (first) {
                    path.moveTo(p.x, p.y);
                    first = false;
                } else {
                    path.lineTo(p.x, p.y);
                }
            }
            path.close();

            Matrix matrix = new Matrix();
            matrix.postTranslate(-rect.centerX(), -rect.centerY());
            matrix.postRotate(face.getHeadEulerAngleZ());
            path.transform(matrix);
            RectF rect2 = new RectF();
            path.computeBounds(rect2, true);
            matrix.postTranslate(-rect2.left, -rect2.top);

            int width = (int) (rect2.right - rect2.left);
            int height = (int) (rect2.bottom - rect2.top);

            Bitmap faceBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas cvCropFaceToAdd = new Canvas(faceBmp);
            Matrix matrix2 = new Matrix();
            matrix2.postTranslate(-rect2.left, -rect2.top);
            path.transform(matrix2);
            cvCropFaceToAdd.drawPath(path, paint);

            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

            // gray scale
            if (grayScale) {
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                paint.setColorFilter(f);
            }
            cvCropFaceToAdd.drawBitmap(frame, matrix, paint);
            return faceBmp;
        } else {

            // only get bounding box

            RectF rect = new RectF(face.getBoundingBox());
            Matrix matrix = new Matrix();
            matrix.postTranslate(-rect.centerX(), -rect.centerY());
            matrix.postRotate(face.getHeadEulerAngleZ());
            matrix.mapRect(rect);
            matrix.postTranslate(-rect.left, -rect.top);

            int width = (int) (rect.right - rect.left);
            int height = (int) (rect.bottom - rect.top);
            Bitmap faceBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas cvCropFaceToAdd = new Canvas(faceBmp);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            if (grayScale) {
                // gray scale
                ColorMatrix cm = new ColorMatrix();
                cm.setSaturation(0);
                ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
                paint.setColorFilter(f);
            }
            cvCropFaceToAdd.drawBitmap(frame, matrix, paint);
            return faceBmp;
        }
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

    public static boolean isValidFace(Face face) {
        return Math.abs(face.getHeadEulerAngleY()) <= 45;
    }

    public static String getFaceDirection(Face detectedFace) {
        float thresholdCenter = 10;
        float offsetHorizontal = 10;
        float offsetVertical = 8;
        if (Math.abs(detectedFace.getHeadEulerAngleX()) <= thresholdCenter && Math.abs(detectedFace.getHeadEulerAngleY()) <= thresholdCenter) {
            return Constant.SIDE_STRAIGHT;
        }
        if (detectedFace.getHeadEulerAngleY() > Math.abs(detectedFace.getHeadEulerAngleX()) + offsetHorizontal) {
            return Constant.SIDE_LEFTWARD;
        } else if (detectedFace.getHeadEulerAngleY() < -Math.abs(detectedFace.getHeadEulerAngleX()) - offsetHorizontal) {
            return Constant.SIDE_RIGHTWARD;
        } else if (detectedFace.getHeadEulerAngleX() > Math.abs(detectedFace.getHeadEulerAngleY()) + offsetVertical) {
            return Constant.SIDE_UPWARD;
        } else if (detectedFace.getHeadEulerAngleX() < -Math.abs(detectedFace.getHeadEulerAngleY()) - offsetVertical) {
            return Constant.SIDE_DOWNWARD;
        }
        return "";
    }
}
