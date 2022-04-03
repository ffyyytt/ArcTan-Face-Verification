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
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

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
    private FaceDetector faceDetector;
    // here the preview image is drawn in portrait way
    private Bitmap portraitBmp = null;
    // here the face is cropped and drawn
    private Bitmap faceBmp = null;
    private double recognizeFaceThreshold = 0.75f;
    private List<String> listFacesVectorString;
    private List<String> listFacesImagePath;
    private List<Bitmap> listFacesBmpPreview;
    private int countFace = 0;
    private boolean isTakenPicture;
    private int userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        userId = getIntent().getIntExtra(KEY_USER_ID, -1);
        Paper.init(this);
        FaceDetectorOptions options =
                new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
                        .build();

        DISPLAY_ADDING_FACE_STEP_NAME = new String[]{getString(R.string.camera_guide_face_left), getString(R.string.camera_guide_face_right), getString(R.string.camera_guide_face_upward), getString(R.string.camera_guide_face_downward), getString(R.string.camera_guide_face_straight)};
        faceDetector = FaceDetection.getClient(options);
        if (getIntent().getIntExtra(KEY_CAMERA_MODE, Constant.MODE_MANUAL) == Constant.MODE_MANUAL) {
            countFace = 5;
        } else {
            countFace = 0;
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

        InputImage image = InputImage.fromBitmap(croppedBitmap, 0);
        faceDetector
                .process(image)
                .addOnSuccessListener(faces -> {
                    if (faces.size() == 0) {
                        updateResults(currTimestamp, new LinkedList<>());
//                        uiThreadHandler.post(() -> methodChannel.invokeMethod("face_recognition#faceAdded", "", null));
                        return;
                    }
                    runInBackground(
                            () -> {
                                try {
                                    onFacesDetected(currTimestamp, faces, isAddingFaceFlow);
                                } catch (Exception e) {
                                    LOGGER.e("An error occurs %s", e.getMessage());
                                }
                            });
                });


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

//        // Account for the already applied rotation, if any, and then determine how
//        // much scaling is needed for each axis.
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

    private void onFacesDetected(long currTimestamp, List<Face> faces, boolean add) {
        if (croppedBitmap == null) return;

        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
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

        Face selectedFace = null;
        int selectedFaceSize = -1;
        for (Face face : faces) {

            int faceWidth = face.getBoundingBox().right - face.getBoundingBox().left;
            int faceHeight = face.getBoundingBox().bottom - face.getBoundingBox().top;
            int faceSize = Math.max(faceWidth, faceHeight);

            if (faceSize > selectedFaceSize) {
                selectedFace = face;
                selectedFaceSize = faceSize;
            }
        }
        // get face has max size
        if (selectedFace != null) {
            final RectF boundingBox = new RectF(selectedFace.getBoundingBox());

            // maps crop coordinates to original
            cropToFrameTransform.mapRect(boundingBox);

            Bitmap faceCrop = FaceUtils.cropFaceFromContour(cropCopyBitmap, selectedFace, false, false);
            if (boundingBox != null && faceCrop != null) {

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
                    faceVector = faceManager.extract(faceBmp);
                } else {
                    Result result = faceManager.verify(faceBmp);
                    if (result != null) {
                        color = Color.GREEN;
                        label = String.format("%s (%f)", getApp().getUser(result.faceData.userId).name, result.similarity);
                    } else {
                        color = Color.RED;
                        label = "Stranger";
                    }
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
                    if (countFace > 0) {
                        if (isTakenPicture && elapseRealTime - lastAddFaceTime >= MIN_LAST_ADD_FACE_MS) {
                            if (FaceUtils.isValidFace(selectedFace)) {
                                lastAddFaceTime = elapseRealTime;
                                isTakenPicture = false;
                                currentAddingFaceStep++;
                                addNewFaceVector(faceVector, faceCrop);
                                updateInformationUI();
                                updatePreviewUI();
                            } else {
                                isTakenPicture = false;
                                Toast.makeText(this, "invalid face", Toast.LENGTH_SHORT).show();
                            }
                        }
                    } else {
                        String addFaceStatus = FaceUtils.getFaceDirection(selectedFace);
                        if (!addFaceStatus.isEmpty() && elapseRealTime - lastAddFaceTime >= MIN_LAST_ADD_FACE_MS) {
                            lastAddFaceTime = elapseRealTime;
                            switch (currentAddingFaceStep) {
                                case 0:
                                    // left
                                    if (addFaceStatus.equals(Constant.SIDE_LEFTWARD)) {
                                        currentAddingFaceStep = 1;
                                        updateInformationUI();
                                        addNewFaceVector(faceVector, faceCrop);
                                    }
                                    break;
                                case 1:
                                    // right
                                    if (addFaceStatus.equals(Constant.SIDE_RIGHTWARD)) {
                                        currentAddingFaceStep = 2;
                                        updateInformationUI();
                                        addNewFaceVector(faceVector, faceCrop);
                                    }
                                    break;
                                case 2:
                                    // up
                                    if (addFaceStatus.equals(Constant.SIDE_UPWARD)) {
                                        currentAddingFaceStep = 3;
                                        updateInformationUI();
                                        addNewFaceVector(faceVector, faceCrop);
                                    }
                                    break;
                                case 3:
                                    // down
                                    if (addFaceStatus.equals(Constant.SIDE_DOWNWARD)) {
                                        currentAddingFaceStep = 4;
                                        updateInformationUI();
                                        addNewFaceVector(faceVector, faceCrop);
                                    }
                                    break;
                                case 4:
                                    // center
                                    if (addFaceStatus.equals(Constant.SIDE_STRAIGHT)) {
                                        // Step done
                                        currentAddingFaceStep = 5;
                                        updateInformationUI();
                                        addNewFaceVector(faceVector, faceCrop);
                                        updatePreviewUI();
                                    }
                                    break;
                            }
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
