package com.factory.procedure.pictureapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.VideoView;


public class MediaDetails extends AppCompatActivity {

    private static String TAG = "MediaDetails";
    public static String IMAGE_DETAIL = "ImagePath";
    public static String VIDEO_DETAIL = "VideoPath";

    private ImageView imageView;
    private ImageButton closeMedia;
    private VideoView videoView;
    private String imagePath, videoUriString;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.media_layout);

        Log.d(TAG, "OnCreate MediaDetails");

        Bundle extras = getIntent().getExtras();
        imagePath = extras.getString(IMAGE_DETAIL);
        videoUriString = extras.getString(VIDEO_DETAIL);

//        Image Path
        if(imagePath != null) {
            Log.d(TAG, "Image here");
            imageView = findViewById(R.id.detail_image);
            imageView.setVisibility(View.VISIBLE);
            Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
            imageView.setImageBitmap(bitmap);
            imageView.requestLayout();

        //        Video Path
        }else{
            Uri videoUri = Uri.parse(videoUriString);
            videoView = findViewById(R.id.detail_video);
            videoView.setVisibility(View.VISIBLE);
            MediaController mediaController = new MediaController(this);
            mediaController.setAnchorView(videoView);
            videoView.setVideoURI(videoUri);
            videoView.setMediaController(mediaController);
            videoView.start();
        }


        closeMedia = findViewById(R.id.close_media);
        closeMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                supportFinishAfterTransition();
            }
        });

    }

}
