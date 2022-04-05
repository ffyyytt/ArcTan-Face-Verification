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
import java.util.List;

public class FaceManager {

    public static final int MODEL_INPUT_SIZE = 224;
    public static final int CONFIDENCE_THRESHOLD = 400;
    private MLHandler mlHandler;
    private List<FaceData> faceBank;

    public FaceManager(Context context, MainApplication application) {
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE))
                .add(new DequantizeOp(0, 1 / 255.0F))
                .build();
        mlHandler = new MLHandler(context.getAssets(), "model.tflite", imageProcessor);

//            Bitmap bitmap1 = Utils.resizeKeepRation(Utils.loadImage(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image1.jpg"), 224, 224);
//            Bitmap bitmap2 = Utils.resizeKeepRation(Utils.loadImage(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image2.jpg"), 224, 224);
//            Bitmap bitmap3 = Utils.resizeKeepRation(Utils.loadImage(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image3.jpg"), 224, 224);
//
//
//            float[] feature1 = model.extractFeature(bitmap1);
//            float[] feature2 = model.extractFeature(bitmap2);
//            float[] feature3 = model.extractFeature(bitmap3);
//
//            Toast.makeText(getApplicationContext(),
//                    Math.round(1000*MyMaths.cosineSimilarity(feature1, feature2)) + " "
//                            + Math.round(1000*MyMaths.cosineSimilarity(feature1, feature3)),
//                    Toast.LENGTH_SHORT).show();
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
                result.similarity = Math.round(1000 * MyMaths.cosineSimilarity(candidateFaceData.feature, faceData.feature));
                results.add(result);
            }
        }
        Collections.sort(results, (o1, o2) -> Double.compare(Math.abs(o2.similarity), Math.abs(o1.similarity)));
        for (int i = 0; i < results.size(); i++) {
            if (Math.abs(results.get(i).similarity) >= CONFIDENCE_THRESHOLD && finalResults.size() < countResult) {
                finalResults.add(results.get(i));
            }
        }
        return finalResults;
    }

    public Result verify(Bitmap candidateFace) {
        List<Result> results = verify(candidateFace, 1);
        if (results.size() > 0) return results.get(0);
        return null;
    }

    public int getRegisterCount() {
        if (faceBank != null) return faceBank.size();
        return 0;
    }
}
