package com.example.demofaceidapp.classification;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.demofaceidapp.face.MLHandler;

import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

public class EyeClassifier {
    public static final int MODEL_INPUT_SIZE = 24;
    private final MLHandler mlHandler;

    private static final String MODEL_FILE = "eye_cls.tflite";

    public EyeClassifier(Context context) {
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE))
                .add(new DequantizeOp(0, 1 / 255.0F))
                .build();
        mlHandler = new MLHandler(context.getAssets(), MODEL_FILE, imageProcessor);
    }

    /**
     * Eye Classification
     * @param eyeCrop
     * @return str
     */
    public String classify(Bitmap eyeCrop) {
        float[] confidences = mlHandler.extractEyeFeature(eyeCrop);
        // Find the index of the class with the highest confidence.
        int maxPos = 0;
        float maxConfidence = 0;
        for (int i = 0; i < confidences.length; i++) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i];
                maxPos = i;
            }
        }
        String[] classes = {"Open", "Close"};
        return classes[maxPos];
    }
}
