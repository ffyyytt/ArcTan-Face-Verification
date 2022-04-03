package com.example.demofaceidapp.detector.tflite;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FaceIDDetector {

    public static final int TF_OD_API_INPUT_SIZE = 112;
    public static final boolean TF_OD_API_IS_QUANTIZED = false;
    public static final String TF_OD_API_MODEL_FILE = "mobile_face_net.tflite";
    public static final String TF_OD_API_LABELS_FILE = "file:///android_asset/labelmap.txt";

    private SimilarityClassifier mDetector;

    public FaceIDDetector(Context context) {
        try {
            mDetector =
                    TFLiteObjectDetectionAPIModel.create(
                            context.getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
        } catch (IOException ignored) {
        }
    }

    public void registerFaces(Map<Integer, List<List<Float>>> faces) {
        if (isInitFail()) return;
        mDetector.registerFaces(faces);
    }

    public int getRegisterCount() {
        if (isInitFail()) return 0;
        return mDetector.getRegisterCount();
    }

    public List<SimilarityClassifier.Recognition> recognizeImage(Bitmap faceBmp, boolean storeExtra) {
        if (isInitFail()) return new ArrayList<>();
        return mDetector.recognizeImage(faceBmp, storeExtra, false);
    }

    public void configureUseNNAPI(boolean isChecked) {
        if (isInitFail()) return;
        mDetector.setUseNNAPI(isChecked);
    }

    public void configureNumThreads(int numThreads) {
        if (isInitFail()) return;
        mDetector.setNumThreads(numThreads);
    }

    public boolean isInitFail() {
        return mDetector == null;
    }
}
