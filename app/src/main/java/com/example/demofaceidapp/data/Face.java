package com.example.demofaceidapp.data;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Face {

    public String pathImage;

    public String encodedFeature;

    public Face(String pathImage, float[] feature) {
        this.pathImage = pathImage;
        this.encodedFeature = parseFaceData2String(feature);
    }

    public Face(String pathImage, String encodedFeature) {
        this.pathImage = pathImage;
        this.encodedFeature = encodedFeature;
    }

    private String parseFaceData2String(float[] faceVector) {
        StringBuilder faceDataString = new StringBuilder();
        int faceVectorSize = faceVector.length;
        for (int i = 0; i < faceVectorSize; i++) {
            faceDataString.append(faceVector[i]);
            if (i < faceVectorSize - 1) {
                faceDataString.append(",");
            }
        }
        return faceDataString.toString();
    }

    public List<Float> parseFaceVectorString() {
        if (encodedFeature == null) return null;
        List<Float> data = new ArrayList<>();
        String[] arr = encodedFeature.split(",");
        for (String s : arr) {
            try {
                if (s.contains("."))
                    data.add(Float.valueOf(s));
                else {
                    byte[] bytes = android.util.Base64.decode(s, android.util.Base64.DEFAULT);
                    data.add(ByteBuffer.wrap(bytes).order(ByteOrder.BIG_ENDIAN).getFloat());
                }
            } catch (Exception ignored) {
            }
        }
        return data;
    }
}
