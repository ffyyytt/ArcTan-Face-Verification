package com.example.demofaceidapp.face;

import java.util.List;

public class MyMaths {
    public static double cosineSimilarity(float [] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += Math.pow(vectorA[i], 2);
            normB += Math.pow(vectorB[i], 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static double cosineSimilarity(List<Float> vectorA, List<Float> vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.size(); i++) {
            dotProduct += vectorA.get(i) * vectorB.get(i);
            normA += Math.pow(vectorA.get(i), 2);
            normB += Math.pow(vectorB.get(i), 2);
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public static float[] norm(float[] _data) {
        float sss = (float) Math.sqrt(sumSquares(_data));
        float[] data = _data.clone();
        for (int i = 0; i < data.length; i++)
        {
            data[i] = data[i]/sss;
        }
        return data;
    }

    public static float sumSquares(float[] data) {
        float ans = 0.0f;
        for (int k = 0; k < data.length; k++) {
            ans += data[k] * data[k];
        }
        return (ans);
    }
}
