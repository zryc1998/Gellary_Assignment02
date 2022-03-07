package com.example.gellary_159336_assignment02_20008378;

import static android.view.MotionEvent.INVALID_POINTER_ID;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MotionEventCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class FullImageActivity extends AppCompatActivity {

    private ImageView mImageView;

    //pinch zoom variables
    private ScaleGestureDetector mScaleGestureDetector;
    private float mScaleFactor = 1.0f;

    //left and right buttons
    private int mPosition;
    private int size;
    ImageButton mLeft;
    ImageButton mRight;
    ArrayList<String> mImage;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_image);
        mLeft = findViewById(R.id.leftButton);
        mRight = findViewById(R.id.rightButton);
        mImageView=findViewById(R.id.imageView);

        //get intent
        Intent intent = getIntent();
        int position = intent.getIntExtra("POSITION",0);
        mPosition = position;
        mImage =(ArrayList<String>) intent.getSerializableExtra("ARRAYLIST");
        size = mImage.size();

        //set left/right button onclick listener
        mLeft.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPosition <= 0){
                    mPosition = 0;

                    Toast  toast = Toast.makeText(getApplicationContext(),"This is the first image",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0,0);
                    toast.show();
                }
                else mPosition--;
                init();
            }
        });

        mRight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mPosition >= size-1) {
                    mPosition = size-1;

                    Toast  toast = Toast.makeText(getApplicationContext(),"This is the last image",Toast.LENGTH_SHORT);
                    toast.setGravity(Gravity.CENTER, 0,0);
                    toast.show();
                }
                else mPosition++;
                init();
            }
        });

        init();
    }

    private void init(){

        assert mImage != null;
        String path = mImage.get(mPosition);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 2;
        Bitmap mBitmap = BitmapFactory.decodeFile(path, options);

        //get and change orientation
        int rotate = getImageOrientation(path);
        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        mBitmap = Bitmap.createBitmap(mBitmap , 0, 0, mBitmap.getWidth(), mBitmap.getHeight(), matrix, true);

        mImageView.setImageBitmap(mBitmap);

        mScaleGestureDetector = new ScaleGestureDetector(this, new ScaleListener());
    }

    private int getImageOrientation(String path) {
        int rotate = 0;
        try {
            File imageFile = new File(path);
            ExifInterface exif = new ExifInterface(
                    imageFile.getAbsolutePath());
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return rotate;
    }


    public boolean onTouchEvent(MotionEvent motionEvent) {
        mScaleGestureDetector.onTouchEvent(motionEvent);
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector scaleGestureDetector){
            mScaleFactor *= scaleGestureDetector.getScaleFactor();
            mScaleFactor = Math.max(0.1f,
                    Math.min(mScaleFactor, 10.0f));
            mImageView.setScaleX(mScaleFactor);
            mImageView.setScaleY(mScaleFactor);
            return true;
        }
    }
}