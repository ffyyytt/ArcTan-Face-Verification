package com.example.demofaceidapp.detector.data;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.example.demofaceidapp.detector.tflite.SimilarityClassifier;

public class FaceData {

    public int id;
    public String name;
    public String imagePath;
    public SimilarityClassifier.Recognition data;
    private transient Bitmap image;

    public FaceData(int id, String name, SimilarityClassifier.Recognition data, String imagePath) {
        this.id = id;
        this.name = name;
        this.data = data;
        this.imagePath = imagePath;
    }

    public FaceData(String name, SimilarityClassifier.Recognition data, String imagePath) {
        this.id = -1;
        this.name = name;
        this.data = data;
        this.imagePath = imagePath;
    }

    public FaceData(int id, Bitmap image) {
        this.id = id;
        this.image = image;
    }

    public Bitmap getImage() {
        if (image == null) {
            image = BitmapFactory.decodeFile(imagePath);
        }
        return image;
    }
}
