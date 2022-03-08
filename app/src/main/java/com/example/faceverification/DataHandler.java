package com.example.faceverification;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class DataHandler {
    public void saveFile(float[] array, String filename) throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(4*array.length);
        byteBuffer.clear();
        byteBuffer.asFloatBuffer().put(array);

        FileChannel fileChannel = new FileOutputStream(filename).getChannel();
        fileChannel.write(byteBuffer);
        fileChannel.close();
    }

    public float[] loadFile(String filename, int size) throws IOException {
        float[] result = new float[size];

        FileChannel fileChannel = new FileOutputStream(filename).getChannel();
        ByteBuffer byteBuffer = ByteBuffer.allocate(4*size);
        byteBuffer.clear();
        fileChannel.read(byteBuffer);
        byteBuffer.rewind();
        byteBuffer.asFloatBuffer().get(result);
        return result;
    }
}
