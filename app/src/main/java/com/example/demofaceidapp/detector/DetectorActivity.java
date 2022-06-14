/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.demofaceidapp.detector;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.hardware.camera2.CameraCharacteristics;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Base64;
import android.util.Size;
import android.util.TypedValue;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.example.demofaceidapp.R;
import com.example.demofaceidapp.data.Result;
import com.example.demofaceidapp.detector.customview.OverlayView;
import com.example.demofaceidapp.detector.env.BorderedText;
import com.example.demofaceidapp.detector.env.ImageUtils;
import com.example.demofaceidapp.detector.env.Logger;
import com.example.demofaceidapp.detector.tflite.SimilarityClassifier;
import com.example.demofaceidapp.detector.tracking.MultiBoxTracker;
import com.example.demofaceidapp.detector.utils.Constant;
import com.example.demofaceidapp.detector.utils.FaceUtils;
import com.example.demofaceidapp.face.FaceManager;
import com.example.demofaceidapp.ml.EyeClsModel;
import com.example.demofaceidapp.mtcnn.Align;
import com.example.demofaceidapp.mtcnn.Box;
import com.example.demofaceidapp.mtcnn.MTCNN;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import io.paperdb.Paper;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {

    final static long MIN_LAST_ADD_FACE_MS = 100;

    public static final String KEY_CAMERA_MODE = "camera_mode";
    public static final String KEY_USER_ID = "user_id";
    public static final String KEY_ADDING_FACE = "adding_face";

    private static final Logger LOGGER = new Logger();
    private static final boolean MAINTAIN_ASPECT = false;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(1280, 720);
    // PaperDB
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    long lastAddFaceTime = 0;
    private Integer sensorOrientation;
    private FaceManager faceManager;
    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;
    private boolean computingDetection = false;
    private boolean isAddingFaceFlow = false;
    private String[] DISPLAY_ADDING_FACE_STEP_NAME;
    private int currentAddingFaceStep = 0;
    private int totalAddingFaceStep = 5;
    private long timestamp = 0;
    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private MultiBoxTracker tracker;
    private BorderedText borderedText;
    // Face detector
    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;
    // here the face is cropped and drawn
    private Bitmap faceBmp = null;
    private List<String> listFacesVectorString;
    private List<String> listFacesImagePath;
    private List<Bitmap> listFacesBmpPreview;
    private int countFace = 0;
    private boolean isTakenPicture;
    private int userId;
    private int resizeImageSize = 24;
    private MTCNN mtcnn;
    private String result1;
    private String result2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = getIntent().getIntExtra(KEY_USER_ID, -1);
        Paper.init(this);

        try {
            mtcnn = new MTCNN(getAssets());

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (getIntent().getIntExtra(KEY_CAMERA_MODE, Constant.MODE_MANUAL) == Constant.MODE_MANUAL) {
            countFace = 5;
        }
        if (countFace > 0) totalAddingFaceStep = countFace;
        listFacesVectorString = new ArrayList<>();
        listFacesImagePath = new ArrayList<>();
        listFacesBmpPreview = new ArrayList<>();
    }

    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        faceManager = new FaceManager(this, getApp());
        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);


        int targetW, targetH;
        if (sensorOrientation == 90 || sensorOrientation == 270) {
            targetH = previewWidth;
            targetW = previewHeight;
        } else {
            targetW = previewWidth;
            targetH = previewHeight;
        }
        int cropW = (int) (targetW / 2f);
        int cropH = (int) (targetH / 2f);

        croppedBitmap = Bitmap.createBitmap(cropW, cropH, Config.ARGB_8888);

        portraitBmp = Bitmap.createBitmap(targetW, targetH, Config.ARGB_8888);
        faceBmp = Bitmap.createBitmap(FaceManager.MODEL_INPUT_SIZE, FaceManager.MODEL_INPUT_SIZE, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropW, cropH,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);


        Matrix frameToPortraitTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        targetW, targetH,
                        sensorOrientation, MAINTAIN_ASPECT);


        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);

        isAddingFaceFlow = getIntent().getBooleanExtra(KEY_ADDING_FACE, false);
        updateInformationUI();
        tracker.setAddingFaceFlow(isAddingFaceFlow);
    }



    @Override
    protected void processImage() {
        if (rgbFrameBitmap == null || rgbFrameBitmap.isRecycled()) return;
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;

        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        Vector<Box> boxes = onBoxesDetected();

        if (boxes.size() == 0){
            updateResults(currTimestamp, new LinkedList<>());
        }
        else {
            runInBackground(
                    () -> {
                        try {
                            onFacesDetected(currTimestamp, boxes, isAddingFaceFlow);
                        } catch (Exception e) {
                            LOGGER.e("An error occurs %s", e.getMessage());
                        }
                    }
            );
        }
    }


    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
    }

    @Override
    protected boolean isAddingFaceFlow() {
        return isAddingFaceFlow;
    }

    @Override
    protected int getTotalAddingFaceStep() {
        return totalAddingFaceStep;
    }

    @Override
    protected int getCurrentAddingFaceStep() {
        return currentAddingFaceStep;
    }

    @Override
    protected int getCountFreeStep() {
        return countFace;
    }

    @Override
    protected String getDisplayCurrentAddingFaceStep() {
        if (DISPLAY_ADDING_FACE_STEP_NAME == null || DISPLAY_ADDING_FACE_STEP_NAME.length <= currentAddingFaceStep) {
            return "";
        }
        return DISPLAY_ADDING_FACE_STEP_NAME[currentAddingFaceStep];
    }

    @Override
    protected void addingFaceDone() {
        List<com.example.demofaceidapp.data.Face> faces = new ArrayList<>();
        for (int i = 0; i < listFacesVectorString.size(); i++) {
            faces.add(new com.example.demofaceidapp.data.Face(listFacesImagePath.get(i), listFacesVectorString.get(i)));
        }
        getApp().addFaces(userId, faces);
        finish();
    }

    @Override
    protected void retakeFace() {
        currentAddingFaceStep = 0;
        listFacesImagePath.clear();
        listFacesVectorString.clear();
        listFacesBmpPreview.clear();
        updateInformationUI();
    }

    @Override
    protected void takeFace() {
        isTakenPicture = true;
    }

    @Override
    protected void setNumThreads(final int numThreads) {
    }

    // Face Processing
    private Matrix createTransform(
            final int srcWidth,
            final int srcHeight,
            final int dstWidth,
            final int dstHeight,
            final int applyRotation) {

        Matrix matrix = new Matrix();
        if (applyRotation != 0) {
            if (applyRotation % 90 != 0) {
                LOGGER.w("Rotation of %d % 90 != 0", applyRotation);
            }

            // Translate so center of image is at origin.
            matrix.postTranslate(-srcWidth / 2.0f, -srcHeight / 2.0f);

            // Rotate around origin.
            matrix.postRotate(applyRotation);
        }

        // Account for the already applied rotation, if any, and then determine how
        // much scaling is needed for each axis.
//        final boolean transpose = (Math.abs(applyRotation) + 90) % 180 == 0;
//        final int inWidth = transpose ? srcHeight : srcWidth;
//        final int inHeight = transpose ? srcWidth : srcHeight;

        if (applyRotation != 0) {

            // Translate back from origin centered reference to destination frame.
            matrix.postTranslate(dstWidth / 2.0f, dstHeight / 2.0f);
        }

        return matrix;

    }

    private void updateResults(long currTimestamp, final List<SimilarityClassifier.Recognition> mappedRecognitions) {
        tracker.trackResults(mappedRecognitions, currTimestamp);
        trackingOverlay.postInvalidate();
        computingDetection = false;

        runOnUiThread(
                () -> {
                    LOGGER.i("showFaceCount: " + mappedRecognitions.size() + " faces");
                    LOGGER.i("showDataCount: " + faceManager.getRegisterCount() + " faces");
                    LOGGER.i("showInference: " + lastProcessingTimeMs + "ms");
                });
    }

    private int classifyEyeImage(EyeClsModel model, Bitmap image){
        // Creates inputs for reference.
        TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 24, 24, 3}, DataType.FLOAT32);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * resizeImageSize * resizeImageSize * 3);
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[resizeImageSize * resizeImageSize];
        image.getPixels(intValues, 0, image.getWidth(), 0, 0, image.getWidth(), image.getHeight());

        int pixel = 0;

        // Iterate over each pixel and extract R, G, and B values. Add those values individually to the byte buffer.
        for(int i = 0; i < resizeImageSize; i ++){
            for(int j = 0; j < resizeImageSize; j++){
                int val = intValues[pixel++]; // RGB
                byteBuffer.putFloat(((val >> 16) & 0xFF) * (1.f / 255));
                byteBuffer.putFloat(((val >> 8) & 0xFF) * (1.f / 255));
                byteBuffer.putFloat((val & 0xFF) * (1.f / 255));
            }
        }

        inputFeature0.loadBuffer(byteBuffer);

        // Runs model inference and gets result.
        EyeClsModel.Outputs outputs = model.process(inputFeature0);
        TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

        float[] confidences = outputFeature0.getFloatArray();

        // Find the index of the class with the highest confidence.
        int maxPos = 0;
        float maxConfidence = 0;
        for (int i = 0; i < confidences.length; i++) {
            if (confidences[i] > maxConfidence) {
                maxConfidence = confidences[i];
                maxPos = i;
            }
        }

        return maxPos;
    }

    private void classify(Bitmap image1, Bitmap image2){
        try {
            EyeClsModel model = EyeClsModel.newInstance(getApplicationContext());
            int maxPosImage1 = classifyEyeImage(model, image1);
            int maxPosImage2 = classifyEyeImage(model, image2);

            String[] classes = {"Open", "Close"};
            result1 = classes[maxPosImage1];
            result2 = classes[maxPosImage2];

            // Releases model resources if no longer used.
            model.close();

        } catch (IOException e) {
            // TODO Handle the exception
            LOGGER.e(e, "Failed to classify.");
        }
    }

    private Vector<Box> onBoxesDetected(){
        Vector<Box> boxes = new Vector<>();
        if (croppedBitmap == null) return boxes;
        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);

        boxes = mtcnn.detectFaces(croppedBitmap, croppedBitmap.getWidth() / 5); // 只有这句代码检测人脸，下面都是根据Box在图片中裁减出人脸

        if (boxes.size() == 0)
            return boxes;

        Box selectedBox = null;
        int selectedBoxSize = -1;
        for (Box box: boxes) {
            int boxSize = Math.max(box.width(), box.height());

            if (boxSize > selectedBoxSize){
                selectedBox = box;
                selectedBoxSize = boxSize;
            }
        }
        LOGGER.d("Boxes Detected!");

        // Face alignment, detect bounding box 1 one time
        cropCopyBitmap = Align.face_align(cropCopyBitmap, selectedBox.landmark);
        Vector<Box> boxes1 = mtcnn.detectFaces(cropCopyBitmap, cropCopyBitmap.getWidth() / 5);
        LOGGER.d("Boxes Aligned!");

        return boxes1;
    }

    private void onFacesDetected(long currTimestamp, Vector<Box> boxes, boolean add) {
        if (cropCopyBitmap == null) return;

        final Paint paint = new Paint();
        paint.setColor(Color.RED);
        paint.setStyle(Style.STROKE);
        paint.setStrokeWidth(2.0f);

        final List<SimilarityClassifier.Recognition> mappedRecognitions =
                new LinkedList<SimilarityClassifier.Recognition>();


        // Note this can be done only once
        int sourceW = rgbFrameBitmap.getWidth();
        int sourceH = rgbFrameBitmap.getHeight();
        int targetW = portraitBmp.getWidth();
        int targetH = portraitBmp.getHeight();
        Matrix transform = createTransform(
                sourceW,
                sourceH,
                targetW,
                targetH,
                sensorOrientation);
        final Canvas cv = new Canvas(portraitBmp);

        // draws the original image in portrait mode.
        cv.drawBitmap(rgbFrameBitmap, transform, null);

        final Canvas cvFace = new Canvas(faceBmp);

        Box selectedBox = null;
        int selectedBoxSize = -1;
        for (Box box: boxes) {
            int boxSize = Math.max(box.width(), box.height());

            if (boxSize > selectedBoxSize){
                selectedBox = box;
                selectedBoxSize = boxSize;
            }
        }
        LOGGER.d("Selected Box!");

        if (selectedBox != null) {
            selectedBox.toSquareShape();
            selectedBox.limitSquare(cropCopyBitmap.getWidth(), cropCopyBitmap.getHeight());
            Rect rect1 = selectedBox.transform2Rect();
            final RectF boundingBox = new RectF(rect1); // Rect Int -> Rect Float

            // maps crop coordinates to original
            cropToFrameTransform.mapRect(boundingBox);

            Bitmap faceCrop = Align.cropFaceFromContour(cropCopyBitmap, selectedBox);
            LOGGER.d("Face Cropped!");

            if (boundingBox != null && faceCrop != null) {
                LOGGER.d("Go to Face Extract!");

                float sx = ((float) FaceManager.MODEL_INPUT_SIZE) / faceCrop.getWidth();
                float sy = ((float) FaceManager.MODEL_INPUT_SIZE) / faceCrop.getHeight();
                Matrix matrix = new Matrix();
                matrix.postScale(sx, sy);

                cvFace.drawBitmap(faceCrop, matrix, null);

                String label = "";
                float confidence = -1f;
                int color = Color.BLACK;
                float[] faceVector = null;

                final long startTime = SystemClock.uptimeMillis();
                if (isAddingFaceFlow) {
                    LOGGER.d("Go to face register");

                    faceVector = faceManager.extract(faceBmp); // Register Stage
                } else {
                    LOGGER.d("Go to face verify");

                    Result result = faceManager.verify(faceBmp);    // Verify Stage
                    if (result != null) {
                        int eye_w = Math.round(faceCrop.getWidth() / 10) * 2; // Eye
                        int eye_h = Math.round(faceCrop.getHeight() / 10) * 2;
                        LOGGER.d("Eye width ori: %d || Eye height ori: %d", eye_w, eye_h);

                        Bitmap eye_img1 = FaceUtils.cropEyeFromOri(cropCopyBitmap, selectedBox.landmark[0], eye_w, eye_h);
                        Bitmap eye_img2 = FaceUtils.cropEyeFromOri(cropCopyBitmap, selectedBox.landmark[1], eye_w, eye_h);

                        Bitmap resize_eye_img1 = Bitmap.createScaledBitmap(eye_img1, resizeImageSize, resizeImageSize, true);
                        Bitmap resize_eye_img2 = Bitmap.createScaledBitmap(eye_img2, resizeImageSize, resizeImageSize, true);

                        classify(resize_eye_img1, resize_eye_img2);


                        LOGGER.d("Classify || Image 1: %s | Image 2: %s", result1, result2);
                        label = String.format("%s || %s | %s", getApp().getUser(result.faceData.userId).name, result1, result2);
                        color = Color.GREEN;


//                        color = Color.GREEN;
//                        label = String.format("%s (%f)", getApp().getUser(result.faceData.userId).name, result.similarity);

                    } else {
                        color = Color.RED;
                        label = "Không hợp lệ: (Người lạ)";
                    }

                    LOGGER.d("Face Recognized!");
                }

                lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                if (getCameraFacing() == CameraCharacteristics.LENS_FACING_FRONT) {

                    // camera is frontal so the image is flipped horizontally
                    // flips horizontally
                    Matrix flip = new Matrix();
                    if (sensorOrientation == 90 || sensorOrientation == 270) {
                        flip.postScale(1, -1, previewWidth / 2.0f, previewHeight / 2.0f);
                    } else {
                        flip.postScale(-1, 1, previewWidth / 2.0f, previewHeight / 2.0f);
                    }
                    //flip.postScale(1, -1, targetW / 2.0f, targetH / 2.0f);
                    flip.mapRect(boundingBox);
                }

                final SimilarityClassifier.Recognition result = new SimilarityClassifier.Recognition(
                        0, label, confidence, boundingBox);

                result.setColor(color);
                result.setLocation(boundingBox);
                if (add) result.setCrop(faceCrop);
                result.setValid(tracker.checkFaceDetectedIsInFrame(boundingBox));
                mappedRecognitions.add(result);
                if (faceVector != null && isAddingFaceFlow) {
                    long elapseRealTime = SystemClock.elapsedRealtime();
                    if (isTakenPicture && elapseRealTime - lastAddFaceTime >= MIN_LAST_ADD_FACE_MS) {
                        if (FaceUtils.isValidFace(selectedBox)) {
                            lastAddFaceTime = elapseRealTime;
                            isTakenPicture = false;
                            currentAddingFaceStep++;
                            addNewFaceVector(faceVector, faceCrop);
                            updateInformationUI();
                            updatePreviewUI();
                        } else {
                            isTakenPicture = false;
                            Toast.makeText(this, "Mặt không hợp lệ", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }

        updateResults(currTimestamp, mappedRecognitions);
    }

    private void addNewFaceVector(float[] faceVector, Bitmap faceBmp) {
        Bitmap cloneBitmap = faceBmp.copy(faceBmp.getConfig(), false);
        listFacesVectorString.add(parseFaceData2String(faceVector));
        listFacesBmpPreview.add(cloneBitmap);
        try {
            File imageFile = FaceUtils.getFaceKeyImageFile(getApplicationContext(), userId, listFacesImagePath.size());
            FileOutputStream outputStream = new FileOutputStream(imageFile);
            cloneBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream);
            listFacesImagePath.add(imageFile.getAbsolutePath());
        } catch (Exception ignored) {
        }
    }

    private List<Float> convertArrToList(float[] arr) {
        List<Float> list = new ArrayList<>();
        for (float v : arr) {
            list.add(v);
        }
        return list;
    }

    private String parseFaceData2String(float[] faceVector) {
        StringBuilder faceDataString = new StringBuilder();
        int faceVectorSize = faceVector.length;
        for (int i = 0; i < faceVectorSize; i++) {
            String x = Base64.encodeToString(ByteBuffer.allocate(4).putFloat(faceVector[i]).array(), Base64.DEFAULT);
            faceDataString.append(x);
            if (i < faceVectorSize - 1) {
                faceDataString.append(",");
            }
        }
        return faceDataString.toString();
    }

    @Override
    protected List<Bitmap> getAddedFaceList() {
        return listFacesBmpPreview;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
