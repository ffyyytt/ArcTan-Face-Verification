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

public class FaceAntiSpoofing {
    public static final int MODEL_INPUT_SIZE = 224;
    private MLHandler mlHandler;

    private static final String MODEL_FILE = "spoofing_model.tflite";
    public static final float THRESHOLD = 0.5f; // 设置一个阙值，大于这个值认为是攻击
    public static final int LAPLACE_THRESHOLD = 50; // 拉普拉斯采样阙值
    public static final int LAPLACIAN_THRESHOLD = 1000; // 图片清晰度判断阙值



    public FaceAntiSpoofing(Context context) {
        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(MODEL_INPUT_SIZE, MODEL_INPUT_SIZE))
                .add(new DequantizeOp(0, 1 / 255.0F))
                .build();
        mlHandler = new MLHandler(context.getAssets(), MODEL_FILE, imageProcessor);
    }

    /**
     * 活体检测
     * @param faceCrop
     * @return 评分
     */
    public boolean antiSpoofing(Bitmap faceCrop) {
        int laplacianScore = laplacian(faceCrop);
        if (laplacianScore < LAPLACIAN_THRESHOLD){
            return false;
        }
        float[] features = mlHandler.extractSpoofFeature(faceCrop);
        if (features[0] >= THRESHOLD){
            return false;
        }
        return true;
    }


    /**
     * 拉普拉斯算法计算清晰度
     * @param faceCrop
     * @return 分数
     */
    private int laplacian(Bitmap faceCrop) {
        int[][] laplace = {{0, 1, 0}, {1, -4, 1}, {0, 1, 0}};
        int size = laplace.length;
        int[][] img = MyUtil.convertGreyImg(faceCrop);
        int height = img.length;
        int width = img[0].length;

        int score = 0;
        for (int x = 0; x < height - size + 1; x++){
            for (int y = 0; y < width - size + 1; y++){
                int result = 0;
                // 对size*size区域进行卷积操作
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

    public static boolean isValidFace(Box face) {
        return true;
    }
}