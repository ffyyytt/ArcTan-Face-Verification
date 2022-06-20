package com.example.demofaceidapp.face;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.InterpreterFactory;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MLHandler {
    InterpreterApi model;
    ImageProcessor imageProcessor;
    MLHandler(File file, ImageProcessor _imageProcessor) {
        try {
            model = loadModel(file);
            imageProcessor = _imageProcessor;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public MLHandler(AssetManager assetManager, String modeAssetName, ImageProcessor _imageProcessor) {
        try {
            model = loadModelFromAsset(assetManager, modeAssetName);
            imageProcessor = _imageProcessor;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Bitmap processImage(Bitmap bitmap)
    {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        return imageProcessor.process(tensorImage).getBitmap();
    }

    public float[] extractFeature(TensorImage tensorImage) {
        TensorBuffer feature = TensorBuffer.createFixedSize(new int[]{1, 1024}, DataType.FLOAT32);
        model.run(imageProcessor.process(tensorImage).getBuffer(), feature.getBuffer());
        return feature.getFloatArray();
    }

    public float[] extractFeature(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        return extractFeature(tensorImage);
    }

    public float[] extractSpoofFeature(TensorImage tensorImage) {
        TensorBuffer feature = TensorBuffer.createFixedSize(new int[]{1, 1}, DataType.FLOAT32);
        model.run(imageProcessor.process(tensorImage).getBuffer(), feature.getBuffer());
        return feature.getFloatArray();
    }

    public float[] extractSpoofFeature(Bitmap bitmap) {
        TensorImage tensorImage = new TensorImage(DataType.FLOAT32);
        tensorImage.load(bitmap);
        return extractSpoofFeature(tensorImage);
    }

    public static InterpreterApi loadModel(File file) throws IOException {
        //Interpreter model = new Interpreter(file);

        FileInputStream fileInputStream = new FileInputStream(file);
        FileChannel fileChannel = fileInputStream.getChannel();
        MappedByteBuffer mappedByteBuffer =  fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileChannel.size());

        Interpreter.Options tfliteOptions = new Interpreter.Options();
        //tfliteOptions.addDelegate(new GpuDelegate());
        tfliteOptions.setNumThreads(8);
        InterpreterApi model = new InterpreterFactory().create(mappedByteBuffer, tfliteOptions);

        return model;
    }

    public static InterpreterApi loadModelFromAsset(AssetManager assets, String modelFilename) throws IOException {
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        tfliteOptions.setNumThreads(4);
        return new Interpreter(loadModelFile(assets, modelFilename), tfliteOptions);
    }

    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }
}
