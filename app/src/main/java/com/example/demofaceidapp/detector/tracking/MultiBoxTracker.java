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

package com.example.demofaceidapp.detector.tracking;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.text.TextUtils;
import android.util.Pair;
import android.util.TypedValue;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

import com.example.demofaceidapp.detector.env.BorderedText;
import com.example.demofaceidapp.detector.env.ImageUtils;
import com.example.demofaceidapp.detector.env.Logger;
import com.example.demofaceidapp.detector.tflite.SimilarityClassifier;

/**
 * A tracker that handles non-max suppression and matches existing objects to new detections.
 */
public class MultiBoxTracker {
    private static final float TEXT_SIZE_DIP = 18;
    private static final float MIN_SIZE = 16.0f;
    private static final int[] COLORS = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.YELLOW,
            Color.CYAN,
            Color.MAGENTA,
            Color.WHITE,
            Color.parseColor("#55FF55"),
            Color.parseColor("#FFA500"),
            Color.parseColor("#FF8888"),
            Color.parseColor("#AAAAFF"),
            Color.parseColor("#FFFFAA"),
            Color.parseColor("#55AAAA"),
            Color.parseColor("#AA33AA"),
            Color.parseColor("#0D0068")
    };
    private static final Logger LOGGER = new Logger();
    final List<Pair<Float, RectF>> screenRects = new LinkedList<Pair<Float, RectF>>();
    private final Logger logger = new Logger();
    private final Queue<Integer> availableColors = new LinkedList<Integer>();
    private final List<TrackedRecognition> trackedObjects = new LinkedList<TrackedRecognition>();
    private final Paint boxPaint = new Paint();
    private final float textSizePx;
    private final BorderedText borderedText;
    private Matrix frameToCanvasMatrix;
    private int frameWidth;
    private int frameHeight;
    private int sensorOrientation;
    private Point centerFaceMaskPoint;
    private int faceScanType = 0;
    private boolean isAddingFaceFlow;
    private float radiusCircleAllowed = 0;

    private Path mPath = new Path();

    public synchronized void setFrameConfiguration(
            final int width, final int height, final int sensorOrientation) {
        frameWidth = width;
        frameHeight = height;
        this.sensorOrientation = sensorOrientation;
    }

    public synchronized void drawDebug(final Canvas canvas) {
        final Paint textPaint = new Paint();
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(60.0f);

        final Paint boxPaint = new Paint();
        boxPaint.setColor(Color.RED);
        boxPaint.setAlpha(200);
        boxPaint.setStyle(Style.STROKE);

        for (final Pair<Float, RectF> detection : screenRects) {
            final RectF rect = detection.second;
            canvas.drawRect(rect, boxPaint);
            canvas.drawText("" + detection.first, rect.left, rect.top, textPaint);
            borderedText.drawText(canvas, rect.centerX(), rect.centerY() - borderedText.getHeightSize(), "" + detection.first);
        }
    }

    public void setFaceScanType(int type) {
        this.faceScanType = type;
    }

    public synchronized void trackResults(final List<SimilarityClassifier.Recognition> results, final long timestamp) {
        logger.i("Processing %d results from %d", results.size(), timestamp);
        processResults(results);
    }

    private Matrix getFrameToCanvasMatrix() {
        return frameToCanvasMatrix;
    }

    private Paint mSemiBlackPaint;

    public MultiBoxTracker(final Context context) {
        for (final int color : COLORS) {
            availableColors.add(color);
        }

        boxPaint.setColor(Color.RED);
        boxPaint.setStyle(Style.STROKE);
        boxPaint.setStrokeWidth(10);

        mSemiBlackPaint = new Paint();
        mSemiBlackPaint.setColor(Color.TRANSPARENT);
        mSemiBlackPaint.setStrokeWidth(10);

        textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, context.getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
    }

    public synchronized void draw(final Canvas canvas) {
        final boolean rotated = sensorOrientation % 180 == 90;
        final float multiplier =
                Math.min(
                        canvas.getHeight() / (float) (rotated ? frameWidth : frameHeight),
                        canvas.getWidth() / (float) (rotated ? frameHeight : frameWidth));
        frameToCanvasMatrix =
                ImageUtils.getTransformationMatrix(
                        frameWidth,
                        frameHeight,
                        (int) (multiplier * (rotated ? frameHeight : frameWidth)),
                        (int) (multiplier * (rotated ? frameWidth : frameHeight)),
                        sensorOrientation,
                        false);
        for (final TrackedRecognition recognition : trackedObjects) {
            final RectF trackedPos = new RectF(recognition.location);

            getFrameToCanvasMatrix().mapRect(trackedPos);
            boxPaint.setColor(recognition.color);

            float cornerSize = Math.min(trackedPos.width(), trackedPos.height()) / 8.0f;
            canvas.drawRoundRect(trackedPos, cornerSize, cornerSize, boxPaint);


            @SuppressLint("DefaultLocale") final String strConfidence =
                    recognition.detectionConfidence < 0
                            ? ""
                            : String.format(Locale.US, "[%.2f]", recognition.detectionConfidence) + "";

            final String labelString =
                    !TextUtils.isEmpty(recognition.title)
                            ? String.format("%s %s", recognition.title, strConfidence)
                            : strConfidence;

            borderedText.drawText(
                    canvas, trackedPos.left + cornerSize, trackedPos.top, labelString, boxPaint);
        }
    }

    private synchronized void drawAllowedFaceFrame(final Canvas canvas, int width, int height) {
        boxPaint.setColor(Color.parseColor("#FF6200EE"));
//        int width = canvas.getWidth();
//        int height = canvas.getHeight();
//        LOGGER.i("cW is %d, cH is %d, fW is %d, fH is %d", width, height, frameWidth, frameHeight);
        centerFaceMaskPoint = new Point(width / 2, (height - 100) / 2);
        int rectW = 4 * width / 5;

        boxPaint.setStrokeWidth(15);
        canvas.drawCircle(centerFaceMaskPoint.x, centerFaceMaskPoint.y, rectW * 1.0f / 2, boxPaint);
        radiusCircleAllowed = rectW * 1.0f / 2;

        boxPaint.setStrokeWidth(5);
        canvas.drawCircle(centerFaceMaskPoint.x, centerFaceMaskPoint.y, rectW * 1.0f / 2 - 15, boxPaint);

        mPath.reset();
        mPath.addCircle(centerFaceMaskPoint.x, centerFaceMaskPoint.y, rectW * 1.0f / 2 - 15, Path.Direction.CW);
        mPath.setFillType(Path.FillType.INVERSE_EVEN_ODD);

        canvas.drawPath(mPath, mSemiBlackPaint);
        canvas.clipPath(mPath);
        canvas.drawColor(Color.parseColor("#A6000000"));
    }

    public boolean checkFaceDetectedIsInFrame(RectF detectedFace) {
        if (centerFaceMaskPoint == null) return false;
        final RectF temp = new RectF(detectedFace);
        getFrameToCanvasMatrix().mapRect(temp);
        // get distance from center mask & center detected face on screen,
        LOGGER.i("detectedFace center is (%.0f, %.0f), centerFaceMask is (%d, %d)",
                temp.centerX(), temp.centerY(), centerFaceMaskPoint.x, centerFaceMaskPoint.y);
        double distance = Math.sqrt(Math.pow(centerFaceMaskPoint.x - temp.centerX(), 2) + Math.pow(centerFaceMaskPoint.y - temp.centerY(), 2));
        LOGGER.i("Distance between 2 center points is %.2f, radius is %.2f", distance, radiusCircleAllowed);
        return radiusCircleAllowed == 0 || (distance + temp.width() / 2) <= radiusCircleAllowed;
    }

    private void processResults(final List<SimilarityClassifier.Recognition> results) {
        final List<Pair<Float, SimilarityClassifier.Recognition>> rectsToTrack = new LinkedList<>();

        screenRects.clear();
        final Matrix rgbFrameToScreen = new Matrix(getFrameToCanvasMatrix());

        for (final SimilarityClassifier.Recognition result : results) {
            if (result.getLocation() == null) continue;

            final RectF detectionFrameRect = new RectF(result.getLocation());

            final RectF detectionScreenRect = new RectF();
            rgbFrameToScreen.mapRect(detectionScreenRect, detectionFrameRect);

            logger.v(
                    "Result! Frame: " + result.getLocation() + " mapped to screen:" + detectionScreenRect);

            screenRects.add(new Pair<Float, RectF>(result.getDistance(), detectionScreenRect));

            if (detectionFrameRect.width() < MIN_SIZE || detectionFrameRect.height() < MIN_SIZE) {
                logger.w("Degenerate rectangle! " + detectionFrameRect);
                continue;
            }

            rectsToTrack.add(new Pair<Float, SimilarityClassifier.Recognition>(result.getDistance(), result));
        }

        trackedObjects.clear();
        if (rectsToTrack.isEmpty()) {
            logger.v("Nothing to track, aborting.");
            return;
        }

        for (final Pair<Float, SimilarityClassifier.Recognition> potential : rectsToTrack) {
            final TrackedRecognition trackedRecognition = new TrackedRecognition();
            trackedRecognition.detectionConfidence = potential.first;
            trackedRecognition.location = new RectF(potential.second.getLocation());
            trackedRecognition.title = potential.second.getTitle();
            if (potential.second.getColor() != null) {
                trackedRecognition.color = potential.second.getColor();
            } else {
                trackedRecognition.color = COLORS[trackedObjects.size()];

            }
            trackedObjects.add(trackedRecognition);

            if (trackedObjects.size() >= COLORS.length) {
                break;
            }
        }
    }

    public void setAddingFaceFlow(boolean addingFaceFlow) {
        isAddingFaceFlow = addingFaceFlow;
    }

    private static class TrackedRecognition {
        RectF location;
        float detectionConfidence;
        int color;
        String title;
    }
}
