package com.example.faceverification;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.InterpreterApi;
import org.tensorflow.lite.InterpreterFactory;
import org.tensorflow.lite.gpu.GpuDelegate;
import org.tensorflow.lite.schema.Padding;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.DequantizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MainActivity extends AppCompatActivity {
    ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView) findViewById(R.id.imageView);
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        try{
            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeWithCropOrPadOp(224, 224))
                    .add(new DequantizeOp(0, 1/255.0F))
                    .build();
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "/model.tflite");
            MLHandler model = new MLHandler(file, imageProcessor);

            Bitmap bitmap1 = Utils.resizeKeepRation(Utils.loadImage(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image1.jpg"), 224, 224);
            Bitmap bitmap2 = Utils.resizeKeepRation(Utils.loadImage(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image2.jpg"), 224, 224);
            Bitmap bitmap3 = Utils.resizeKeepRation(Utils.loadImage(Environment.getExternalStorageDirectory().getAbsolutePath() + "/image3.jpg"), 224, 224);


            float[] feature1 = model.extractFeature(bitmap1);
            float[] feature2 = model.extractFeature(bitmap2);
            float[] feature3 = model.extractFeature(bitmap3);

            Toast.makeText(getApplicationContext(),
                    Math.round(1000*MyMaths.cosineSimilarity(feature1, feature2)) + " "
                    + Math.round(1000*MyMaths.cosineSimilarity(feature1, feature3)),
                    Toast.LENGTH_SHORT).show();

            Bitmap bitmap = model.processImage(bitmap1);
            imageView.setImageBitmap(bitmap);
            Log.d("DEBUG", bitmap.getHeight() + " " + bitmap.getWidth());

        } catch (IOException e){
            Log.e("Error", "Error infer", e);
        }


    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(MainActivity.this, "Permission denied!", Toast.LENGTH_SHORT).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
}