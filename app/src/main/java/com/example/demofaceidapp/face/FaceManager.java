package com.example.demofaceidapp.face;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.demofaceidapp.MainApplication;
import com.example.demofaceidapp.data.Face;
import com.example.demofaceidapp.data.FaceData;
import com.example.demofaceidapp.data.Result;

import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FaceManager {

    public static final int MODEL_INPUT_SIZE = 224;
    public static final double CONFIDENCE_THRESHOLD1 = 0.72;
    public static final double CONFIDENCE_THRESHOLD2 = 2.0;
    private MLHandler mlHandler;
    private List<FaceData> faceBank;

    public FaceManager(Context context, MainApplication application) {
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE))
                .add(new DequantizeOp(0, 1 / 255.0F))
                .build();
        mlHandler = new MLHandler(context.getAssets(), "rec_model.tflite", imageProcessor);
        faceBank = application.getFaceData();
    }

    public float[] extract(Bitmap face) {
        return mlHandler.extractFeature(face);
    }

    public List<Result> verify(Bitmap candidateFace, int countResult) { // get top countResult
        List<Result> results = new ArrayList<>();
        List<Result> finalResults = new ArrayList<>();
        float[] feature = mlHandler.extractFeature(candidateFace);
        FaceData candidateFaceData = new FaceData(-1, new Face("", feature));
        if (faceBank != null) {
            for (int i = 0; i < faceBank.size(); i++) {
                FaceData faceData = faceBank.get(i);
                Result result = new Result();
                result.faceData = faceData;
                result.similarity = MyMaths.cosineSimilarity(candidateFaceData.feature, faceData.feature);
                results.add(result);
            }
        }
        Collections.sort(results, (o1, o2) -> Double.compare(o2.similarity, o1.similarity));
        for (int i = 0; i < results.size(); i++) {
            if (Math.abs(results.get(i).similarity) >= CONFIDENCE_THRESHOLD1 && finalResults.size() < countResult) {
                finalResults.add(results.get(i));
            }
        }
        return finalResults;
    }

    public Result verify(Bitmap candidateFace) {
        List<Result> results = verify(candidateFace, 5);
        if (results.size() > 0)
        {
            int key = 0;
            Map<Integer, Integer> map = new HashMap<Integer, Integer>();
            for (int i = 0; i < results.size(); i++)
            {
                key = results.get(i).faceData.userId;
                if (map.containsKey(key))
                    map.put(key, map.get(key) + 1 - i/10);
                else
                    map.put(key, 1 - i/10);
            }
            for (int i = 0; i < results.size(); i++)
            {
                key = results.get(i).faceData.userId;
                results.get(i).similarity += map.get(key);
            }
            Collections.sort(results, (o1, o2) -> Double.compare(o2.similarity, o1.similarity));
            if (results.get(0).similarity > CONFIDENCE_THRESHOLD2)
                return results.get(0);
        }
        return null;
    }

    public int getRegisterCount() {
        if (faceBank != null) return faceBank.size();
        return 0;
    }
}
