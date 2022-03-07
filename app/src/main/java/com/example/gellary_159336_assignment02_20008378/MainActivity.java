package com.example.gellary_159336_assignment02_20008378;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.ThumbnailUtils;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.util.LruCache;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.SimpleTimeZone;

public class MainActivity extends AppCompatActivity {

    //gridview and columns
    private GridView mGridView;
    private static final int NCOLS = 3;
    private ArrayList<String> mImage;

    //bitmap cache
    private LruCache<String,Bitmap> mCache;

    //pinch gesture
    private ScaleGestureDetector mScaleGestureDetector;

    //restore scroll position
    Parcelable mState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;
        mCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap value) {
                return value.getByteCount() / 1024;
            }
        };

        if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED){
            init();
        }else{
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
        }
    }

    //on resume after permission granted
    @Override
    protected void onResume() {
        super.onResume();
        mState = mGridView.onSaveInstanceState();
        init();
    }

    @SuppressLint("SetTextI18n")
    public void init(){
        mGridView = findViewById(R.id.gridView);
        mGridView.setNumColumns(NCOLS);
        //set gridview
        String[] projection = {
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DATE_ADDED,
        };
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
        assert cursor != null;
        int index = cursor.getColumnIndex(MediaStore.Images.Media.DATA);

        mImage = new ArrayList<>();
        while(cursor.moveToNext()) {
            mImage.add(cursor.getString(index));
        }

        cursor.close();

        ThumbnailAdapter thumbnailAdapter = new ThumbnailAdapter();
        mGridView.setAdapter(thumbnailAdapter);

        //restore scroll location
        if(mState != null) {
            mGridView.onRestoreInstanceState(mState);
        }



        //display info
        TextView textView = findViewById(R.id.textView2);
        textView.setVisibility(View.VISIBLE);
        Date currentTime = Calendar.getInstance().getTime();
        textView.setText(mImage.size() + " Images\n" +"Updated " + currentTime.toString()+"\nChao Yue 20008378");

        mGridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                openFullImageActivity(mImage.get(position));
                openFullImageActivity(mImage,position);
            }
        });


        //set gesture
        mScaleGestureDetector = new ScaleGestureDetector(this,new ScaleGestureDetector.OnScaleGestureListener(){

            @Override
            public void onScaleEnd(ScaleGestureDetector detector){}

            @Override
            public  boolean onScaleBegin(ScaleGestureDetector detector) {
                return true;
            }

            private float mCols = NCOLS;

            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                mCols = mCols/ detector.getScaleFactor();
                if(mCols<1) mCols=1;
                if(mCols>6) mCols=6;
                mGridView.setNumColumns((int)mCols);

                for(int i = 0; i< mGridView.getChildCount(); i++) {
                    if(mGridView.getChildAt(i)!=null) {
                        mGridView.getChildAt(i).setMinimumHeight(mGridView.getWidth()/(int)(mCols));
                    }
                }

                mGridView.invalidate();
                return true;
            }
        });


        mGridView.setOnTouchListener((View view, MotionEvent motionEvent) -> {
            mScaleGestureDetector.onTouchEvent(motionEvent);
            return false;
        });

    }

    public void openFullImageActivity(ArrayList<String> image, int position) {
        ArrayList<String> arrayList = new ArrayList<>(image);
        Intent intent = new Intent(this,FullImageActivity.class);
        intent.putExtra("POSITION",position);
        intent.putExtra("ARRAYLIST", arrayList);
        this.startActivity(intent);
    }


    public class ThumbnailAdapter extends BaseAdapter {

        class ViewHolder{
            int position;
            ImageView image;
        }
        @Override
        public int getCount() {
            return mImage.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @SuppressLint("StaticFieldLeak")
        @Override
        public View getView(final int i, View convertView, ViewGroup viewGroup) {
            ViewHolder vh;
            if(convertView == null){
                convertView = getLayoutInflater().inflate(R.layout.thumbnail_gallery, viewGroup, false);
                vh = new ViewHolder();
                vh.image = convertView.findViewById(R.id.thumbnailGallery);
                convertView.setTag(vh);
            }else{
                vh = (ViewHolder) convertView.getTag();
            }

            //fit thumbnails to square
            int width = mGridView.getWidth() / mGridView.getNumColumns();
            vh.image.setLayoutParams(new LinearLayout.LayoutParams(width,width));

            vh.position = i;
            vh.image.setImageBitmap(null);

            Bitmap thumbnail = mCache.get(String.valueOf(vh.position));

            if(thumbnail!= null) {
                vh.image.setImageBitmap(thumbnail);
            }

            new AsyncTask<ViewHolder, Void, Bitmap>() {
                private ViewHolder vh;

                @Override
                protected Bitmap doInBackground(ViewHolder... viewHolders) {
                    vh = viewHolders[0];
                    String path = mImage.get(i);
                    if (i != vh.position) return null;

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 8;

                    //extract thumbnail
                    Bitmap bitmap = BitmapFactory.decodeFile(path, options);

                    //get and change orientation
                    int rotate = getImageOrientation(path);
                    Matrix matrix = new Matrix();
                    matrix.postRotate(rotate);
                    bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                    Bitmap thumbnail = ThumbnailUtils.extractThumbnail(bitmap, 200, 200);
                    addBitmapToMemoryCache(String.valueOf(i), thumbnail);

                    return thumbnail;
                }

                @Override
                protected void onPostExecute(Bitmap bmp) {
                    if (vh.position == i) {
                        vh.image.setImageBitmap(bmp);
                    }
                }
            }.execute(vh);
            return convertView;
        }
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

    //add bitmap to cache
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mCache.put(key, bitmap);
        }
    }

    public Bitmap getBitmapFromMemCache(String key) {
        return mCache.get(key);
    }

    //on pause keep gridview position
    @Override
    public void onPause() {
        // Save ListView state @ onPause
         mState = mGridView.onSaveInstanceState();
        super.onPause();
    }

}