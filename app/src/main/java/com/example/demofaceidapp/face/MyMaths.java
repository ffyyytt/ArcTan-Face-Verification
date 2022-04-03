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

    public static double norm(double[] data) {
        return (Math.sqrt(sumSquares(data)));
    }

    public static double norm(int[] data) {
        return (Math.sqrt(sumSquares(data)));
    }

    public static double sumSquares(double[] data) {
        double ans = 0.0;
        for (int k = 0; k < data.length; k++) {
            ans += data[k] * data[k];
        }
        return (ans);
    }

    public static double sumSquares(double[][] data) {
        double ans = 0.0;
        for (int k = 0; k < data.length; k++) {
            for (int l = 0; l < data[k].length; l++) {
                ans += data[k][l] * data[k][l];
            }
        }
        return (ans);
    }

    public static int sumSquares(int[] data) {
        int ans = 0;
        for (int k = 0; k < data.length; k++) {
            ans += data[k] * data[k];
        }
        return (ans);
    }

    public static int sumSquares(int[][] data) {
        int ans = 0;
        for (int k = 0; k < data.length; k++) {
            for (int l = 0; l < data[k].length; l++) {
                ans += data[k][l] * data[k][l];
            }
        }
        return (ans);
    }
}
