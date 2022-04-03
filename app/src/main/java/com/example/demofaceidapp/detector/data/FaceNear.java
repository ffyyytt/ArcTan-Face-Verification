package com.example.demofaceidapp.detector.data;

public class FaceNear {

    public int id;

    public float distance;

    public float[] faceVector;

    public FaceNear(int id, float distance, float[] faceVector) {
        this.id = id;
        this.distance = distance;
        this.faceVector = faceVector;
    }
}
