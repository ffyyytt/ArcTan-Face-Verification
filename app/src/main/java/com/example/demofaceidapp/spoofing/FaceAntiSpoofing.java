package com.example.demofaceidapp.spoofing;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import com.example.demofaceidapp.detector.utils.MyUtil;
import com.example.demofaceidapp.face.MLHandler;
import com.example.demofaceidapp.mtcnn.Box;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Face Anti-Spoofing for Android.
 */


public class FaceAntiSpoofing {
    public static final int MODEL_INPUT_SIZE = 224;
    private final MLHandler mlHandler;

    private static final String MODEL_FILE = "spoof_model.tflite";
    public static final float THRESHOLD = 0.7f; // Set a threshold, greater than this value is considered an attack
    public static final int LAPLACE_THRESHOLD = 50; // Laplace sampling threshold
    public static final int LAPLACIAN_THRESHOLD = 700; // Image clarity threshold

    public FaceAntiSpoofing(Context context) {
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE))
                .add(new DequantizeOp(0, 1 / 255.0F))
                .build();
        mlHandler = new MLHandler(context.getAssets(), MODEL_FILE, imageProcessor);
    }

    /**
     * Liveness detection
     * @param faceCrop
     * @return bool (true means real, false means fake)
     */
    public float antiSpoofing(Bitmap faceCrop) {
        float[] features = mlHandler.extractSpoofFeature(faceCrop);
        return features[0];
    }


    /**
     * Laplacian algorithm to calculate sharpness.
     * @param faceCrop
     * @return score
     */
    public int laplacian(Bitmap faceCrop) {
        int[][] laplace = {{0, 1, 0}, {1, -4, 1}, {0, 1, 0}};
        int size = laplace.length;
        int[][] img = MyUtil.convertGreyImg(faceCrop);
        int height = img.length;
        int width = img[0].length;

        int score = 0;
        for (int x = 0; x < height - size + 1; x++){
            for (int y = 0; y < width - size + 1; y++){
                int result = 0;
                // convolution steps
                for (int i = 0; i < size; i++){
                    for (int j = 0; j < size; j++){
                        result += (img[x + i][y + j] & 0xFF) * laplace[i][j];
                    }
                }
                if (result > LAPLACE_THRESHOLD) {
                    score++;
                }
            }
        }
        return score;
    }
}