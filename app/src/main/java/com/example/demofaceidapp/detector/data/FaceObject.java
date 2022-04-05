package com.example.demofaceidapp.detector.data;


import android.graphics.Rect;

public class FaceObject {

    public FaceData faceData;
    public Rect rect;
    public float similarity;

    public FaceObject(FaceData faceData, Rect rect, float similarity) {
        this.faceData = faceData;
        this.rect = rect;
        this.similarity = similarity;
    }

}
