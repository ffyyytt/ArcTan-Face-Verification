package com.example.demofaceidapp.data;

import java.util.List;

public class FaceData {

    public int userId;

    public Face face;

    public List<Float> feature;

    public FaceData(int userId, Face face) {
        this.userId = userId;
        this.face = face;
        this.feature = face.parseFaceVectorString();
    }
}
