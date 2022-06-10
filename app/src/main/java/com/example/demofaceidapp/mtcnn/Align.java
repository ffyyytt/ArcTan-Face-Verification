package com.example.demofaceidapp.mtcnn;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;


/**
 * 人脸对齐矫正
 */
public class Align {

    /**
     * 仿射变换
     * @param bitmap 原图片
     * @param landmarks landmarks
     * @return 变换后的图片
     */
    public static Bitmap face_align(Bitmap bitmap, Point[] landmarks) {
        float diffEyeX = landmarks[1].x - landmarks[0].x;
        float diffEyeY = landmarks[1].y - landmarks[0].y;

        float fAngle;
        if (Math.abs(diffEyeY) < 1e-7) {
            fAngle = 0.f;
        } else {
            fAngle = (float) (Math.atan(diffEyeY / diffEyeX) * 180.0f / Math.PI);
        }
        Matrix matrix = new Matrix();
        matrix.setRotate(-fAngle);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

    public static Bitmap cropFaceFromContour(Bitmap frame, Box face) {
        // only get bounding box
        RectF rect = new RectF(face.transform2Rect());
        Matrix matrix = new Matrix();
        matrix.postTranslate(-rect.centerX(), -rect.centerY());
//        matrix.postRotate(face.getHeadEulerAngleZ());
        matrix.mapRect(rect);
        matrix.postTranslate(-rect.left, -rect.top);

        int width = (int) (rect.right - rect.left);
        int height = (int) (rect.bottom - rect.top);
        Bitmap faceBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas cvCropFaceToAdd = new Canvas(faceBmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        cvCropFaceToAdd.drawBitmap(frame, matrix, paint);
        return faceBmp;
    }

}
