/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.example.demofaceidapp.detector.tflite;

import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.os.Trace;
import android.util.Pair;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.gpu.CompatibilityList;
import org.tensorflow.lite.gpu.GpuDelegate;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.example.demofaceidapp.detector.data.FaceNear;
import com.example.demofaceidapp.detector.data.FaceNearest;
import com.example.demofaceidapp.detector.env.Logger;
import com.example.demofaceidapp.detector.utils.MyUtil;

/**
 * Wrapper for frozen detection models trained using the Tensorflow Object Detection API:
 * - https://github.com/tensorflow/models/tree/master/research/object_detection
 * where you can find the training code.
 * <p>
 * To use pretrained models in the API or convert to TF Lite models, please see docs for details:
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/detection_model_zoo.md
 * - https://github.com/tensorflow/models/blob/master/research/object_detection/g3doc/running_on_mobile_tensorflowlite.md#running-our-model-on-android
 */
public class TFLiteObjectDetectionAPIModel
        implements SimilarityClassifier {

    private static final Logger LOGGER = new Logger();

    //private static final int OUTPUT_SIZE = 512;
    private static final int OUTPUT_SIZE = 192;

    // Only return this many results.
    private static final int NUM_DETECTIONS = 1;

    // Float model
    private static final float IMAGE_MEAN = 128.0f;
    private static final float IMAGE_STD = 128.0f;

    // Number of threads in the java app
    private static final int NUM_THREADS = 4;
    private boolean isModelQuantized;
    // Config values.
    private int inputSize;
    // Pre-allocated buffers.
    private Vector<String> labels = new Vector<String>();
    private int[] intValues;
    // outputLocations: array of shape [Batchsize, NUM_DETECTIONS,4]
    // contains the location of detected boxes
    private float[][][] outputLocations;
    // outputClasses: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the classes of detected boxes
    private float[][] outputClasses;
    // outputScores: array of shape [Batchsize, NUM_DETECTIONS]
    // contains the scores of detected boxes
    private float[][] outputScores;
    // numDetections: array of shape [Batchsize]
    // contains the number of detected boxes
    private float[] numDetections;

    private float[][] faceVectors;

    private ByteBuffer imgData;

    private Interpreter tfLite;

    private AssetManager mAssetManager;
    private String mModelFileName;

    // Face Mask Detector Output
    private float[][] output;

    private HashMap<String, List<Recognition>> registered = new HashMap<>();
    private Map<Integer, List<List<Float>>> registeredFaces = new HashMap<>();

    private TFLiteObjectDetectionAPIModel() {
    }

    /**
     * Memory-map the model file in Assets.
     */
    private static MappedByteBuffer loadModelFile(AssetManager assets, String modelFilename)
            throws IOException {
        AssetFileDescriptor fileDescriptor = assets.openFd(modelFilename);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize     The size of image input
     * @param isQuantized   Boolean representing model is quantized or not
     */
    public static SimilarityClassifier create(
            final AssetManager assetManager,
            final String modelFilename,
            final String labelFilename,
            final int inputSize,
            final boolean isQuantized)
            throws IOException {

        final TFLiteObjectDetectionAPIModel d = new TFLiteObjectDetectionAPIModel();

        String actualFilename = labelFilename.split("file:///android_asset/")[1];
        InputStream labelsInput = assetManager.open(actualFilename);
        BufferedReader br = new BufferedReader(new InputStreamReader(labelsInput));
        String line;
        while ((line = br.readLine()) != null) {
            LOGGER.w(line);
            d.labels.add(line);
        }
        br.close();

        d.inputSize = inputSize;

        d.mAssetManager = assetManager;
        d.mModelFileName = modelFilename;

        Interpreter.Options options = new Interpreter.Options();
        CompatibilityList compatList = new CompatibilityList();

        if (compatList.isDelegateSupportedOnThisDevice()) {
            // if the device has a supported GPU, add the GPU delegate
            GpuDelegate.Options delegateOptions = compatList.getBestOptionsForThisDevice();
            GpuDelegate gpuDelegate = new GpuDelegate(delegateOptions);
            options.addDelegate(gpuDelegate);
        } else {
            // if the GPU is not supported, run on 4 threads
            options.setNumThreads(4);
        }

        try {
            d.tfLite = new Interpreter(loadModelFile(assetManager, modelFilename), options);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        d.isModelQuantized = isQuantized;
        // Pre-allocate buffers.
        int numBytesPerChannel;
        if (isQuantized) {
            numBytesPerChannel = 1; // Quantized
        } else {
            numBytesPerChannel = 4; // Floating point
        }
        d.imgData = ByteBuffer.allocateDirect(1 * d.inputSize * d.inputSize * 3 * numBytesPerChannel);
        d.imgData.order(ByteOrder.nativeOrder());
        d.intValues = new int[d.inputSize * d.inputSize];

        d.outputLocations = new float[1][NUM_DETECTIONS][4];
        d.outputClasses = new float[1][NUM_DETECTIONS];
        d.outputScores = new float[1][NUM_DETECTIONS];
        d.numDetections = new float[1];
        return d;
    }

    public void registerFaces(Map<Integer, List<List<Float>>> faceData) {
        if (faceData == null || faceData.isEmpty()) return;
        registeredFaces.clear();
        registeredFaces.putAll(faceData);
    }

    public void register(String name, Recognition rec) {
        if (registered.containsKey(name)) {
            registered.get(name).add(rec);
        } else {
            List<Recognition> recognitionList = new ArrayList<>();
            recognitionList.add(rec);
            registered.put(name, recognitionList);
        }
    }

    public int getRegisterCount() {
        if (registered == null) return 0;
        return registered.size();
    }

    private Pair<Integer, Float> findNearestFace(float[] emb) {
        Pair<Integer, Float> ret = null;
        for (Map.Entry<Integer, List<List<Float>>> entry : registeredFaces.entrySet()) {
            for (List<Float> faceVectorList : entry.getValue()) {
                float distance = 0;
                int faceVectorSize = faceVectorList.size();
                if (faceVectorSize < emb.length) continue;
                for (int i = 0; i < emb.length; i++) {
                    float diff = emb[i] - faceVectorList.get(i);
                    distance += diff * diff;
                }

                distance = (float) Math.sqrt(distance);
                if (ret == null || distance < ret.second) {
                    ret = new Pair<>(entry.getKey(), distance);
                }
            }
        }
        return ret;
    }

    private FaceNearest findNearestFace(float[] emb, int numberOfNextNearFaces) {
        FaceNearest faceNearest = null;
        List<FaceNear> nextNearFaces = new ArrayList<>();
        for (Map.Entry<Integer, List<List<Float>>> entry : registeredFaces.entrySet()) {
            for (List<Float> faceVectorList : entry.getValue()) {
                float distance = 0;
                int faceVectorSize = faceVectorList.size();
                if (faceVectorSize < emb.length) continue;
                for (int i = 0; i < emb.length; i++) {
                    float diff = emb[i] - faceVectorList.get(i);
                    distance += diff * diff;
                }

                distance = (float) Math.sqrt(distance);
                if (faceNearest == null || distance < faceNearest.distance) {
                    if (faceNearest == null) {
                        faceNearest = new FaceNearest();
                    }
                    faceNearest.id = entry.getKey();
                    faceNearest.distance = distance;
                }
                nextNearFaces.add(new FaceNear(entry.getKey(), distance, listToArr(faceVectorList)));
            }
        }
        Collections.sort(nextNearFaces, (o1, o2) -> Float.compare(o1.distance, o2.distance));
        if (faceNearest != null) {
            if (nextNearFaces.size() > numberOfNextNearFaces) {
                faceNearest.nextNearFaces = nextNearFaces.subList(0, numberOfNextNearFaces);
            } else {
                faceNearest.nextNearFaces = nextNearFaces;
            }
        }
        return faceNearest;
    }

    private float[] listToArr(List<Float> list) {
        float[] arr = new float[list.size()];
        int i = 0;
        for (Float f : list) {
            arr[i++] = f != null ? f : 0;
        }
        return arr;
    }

    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap, boolean storeExtra, boolean isForDetection) {
        // Log this method so that it can be analyzed with systrace.
        Trace.beginSection("recognizeImage");

        Trace.beginSection("preprocessBitmap");
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        imgData.rewind();
        for (int i = 0; i < inputSize; ++i) {
            for (int j = 0; j < inputSize; ++j) {
                int pixelValue = intValues[i * inputSize + j];
                if (isModelQuantized) {
                    // Quantized model
                    imgData.put((byte) ((pixelValue >> 16) & 0xFF));
                    imgData.put((byte) ((pixelValue >> 8) & 0xFF));
                    imgData.put((byte) (pixelValue & 0xFF));
                } else { // Float model
                    imgData.putFloat((((pixelValue >> 16) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat((((pixelValue >> 8) & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                    imgData.putFloat(((pixelValue & 0xFF) - IMAGE_MEAN) / IMAGE_STD);
                }
            }
        }
        Trace.endSection(); // preprocessBitmap

        Trace.beginSection("feed");

        Object[] inputArray = {imgData};

        Trace.endSection();

        Map<Integer, Object> outputMap = new HashMap<>();

        faceVectors = new float[1][OUTPUT_SIZE];
        outputMap.put(0, faceVectors);


        // Run the inference call.
        Trace.beginSection("run");
        //tfLite.runForMultipleInputsOutputs(inputArray, outputMapBack);
        tfLite.runForMultipleInputsOutputs(inputArray, outputMap);
        Trace.endSection();

        MyUtil.l2Normalize(faceVectors, 1e-10);

        float distance = Float.MAX_VALUE;
        int id = -1;
        String label = "?";

        FaceNearest faceNearest = null;
        if (registeredFaces.size() > 0 && !isForDetection) {
            faceNearest = findNearestFace(faceVectors[0], 20);//findNearest(faceVectors[0]);
            if (faceNearest != null) {
                final int faceId = faceNearest.id;
                label = String.valueOf(faceId);
                id = faceId;
                distance = faceNearest.distance;
            }
        }


        final int numDetectionsOutput = 1;
        final ArrayList<Recognition> recognitions = new ArrayList<>(numDetectionsOutput);
        Recognition rec = new Recognition(
                id,
                label,
                distance,
                new RectF());

        if (faceNearest != null) {
            rec.setNextNearFaces(faceNearest.nextNearFaces);
        }

        recognitions.add(rec);

        if (storeExtra) {
            rec.setExtra(faceVectors);
        }

        Trace.endSection();
        return recognitions;
    }

    @Override
    public void enableStatLogging(final boolean logStats) {
    }

    @Override
    public String getStatString() {
        return "";
    }

    @Override
    public void close() {
    }

    public void setNumThreads(int num_threads) {
    }

    @Override
    public void setUseNNAPI(boolean isChecked) {
        Interpreter.Options tfliteOptions = new Interpreter.Options();
        tfliteOptions.setUseNNAPI(isChecked);
        try {
            tfLite = new Interpreter(loadModelFile(mAssetManager, mModelFileName), tfliteOptions);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
