package com.factory.procedure.pictureapp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.theartofdev.edmodo.cropper.CropImage;

import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import static android.view.View.GONE;
import static com.factory.procedure.pictureapp.Constants.METADATA;

/**
 * Showing Procedure Details here
 */

public class ProcedureActivity extends AppCompatActivity {

    private static String TAG = "Procedure Activity";
    private ImageView mImageView, videoImageView, videoOverlay;
    private ImageButton captureBtn, back_btn, save_btn, delete_btn, videoBtn, del_videoBtn, edit_photo_btn, crop_img_btn;
    private EditText subtitle_txt, explanation_txt;
    private LinearLayout title_bar;
    private RelativeLayout title_area;
    private TextView title;

//    This is used for the camera activity
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final int REQUEST_IMG_EDIT = 2;
    private final int REQUEST_IMG_CAPTURE = 3;

    private String mCurrentPhotoPath, mCurrentVideoPath, mTemporaryPhoto;
    private File filepath;
    private Procedure currentProcedure;
    private ArrayList<Procedure> procedureList;
    private Product selectedProduct;
    private float x1, x2;

//    URL use
    private String addProcedureurladdr = "/plugins/chn1802/addUpdateProcedure";
    private String removeProcedureurladdr = "/plugins/chn1802/deleteProcedure";

//    Dialog Use
    private View dialogView;
    private AlertDialog alertDialog;

    private boolean changed = false;


    @Override
    @SuppressLint("ClickableViewAccessibility")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.procedure_home);

//        Getting the procedure data from the overview page
        Bundle bundle = getIntent().getBundleExtra(Constants.PROCEDURE);
        currentProcedure = (Procedure) bundle.getSerializable(Constants.PROCEDURE);
        selectedProduct = (Product) bundle.getSerializable(Constants.PRODUCT);
//        Pointer to reduce code writing
        try {
            procedureList = selectedProduct.getProcedureList();
        }catch (NullPointerException ne){
            Toast.makeText(getApplicationContext(),"ERROR", Toast.LENGTH_SHORT).show();
            Log.e("ERROR", "Exception", ne);
            finish();
        }

        title = findViewById(R.id.titletext);
        subtitle_txt = findViewById(R.id.procedure_title);
        subtitle_txt.addTextChangedListener(textWatcher);
        explanation_txt = findViewById(R.id.procedure_explain);
        explanation_txt.addTextChangedListener(textWatcher);
        title_bar = findViewById(R.id.title_bar);

        mImageView = findViewById(R.id.preview_image);
        mImageView.setVisibility(GONE);
        mImageView.setOnClickListener(mediabuttonListener);
        videoImageView = findViewById(R.id.preview_video);
        videoImageView.setOnClickListener(mediabuttonListener);
        videoImageView.setVisibility(GONE);

        videoOverlay = findViewById(R.id.VideoPreviewPlayButton);
        videoOverlay.setVisibility(GONE);

        title_area = findViewById(R.id.title_area);
        title_area.setOnTouchListener(touchListener);

        // Add a listener to the Capture button
        captureBtn = findViewById(R.id.camera_btn);
        captureBtn.setOnClickListener(mediabuttonListener);
        edit_photo_btn = findViewById(R.id.edit_img);
        edit_photo_btn.setOnClickListener(mediabuttonListener);
        crop_img_btn = findViewById(R.id.crop_img);
        crop_img_btn.setOnClickListener(mediabuttonListener);

        videoBtn = findViewById(R.id.video_btn);
        videoBtn.setOnClickListener(mediabuttonListener);
        del_videoBtn = findViewById(R.id.delete_video_btn);
        del_videoBtn.setOnClickListener(mediabuttonListener);

        save_btn = findViewById(R.id.save_btn);
        save_btn.setOnClickListener(buttonListener);
        delete_btn = findViewById(R.id.delete_btn);
        delete_btn.setOnClickListener(buttonListener);

        back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(buttonListener);


        if (getIntent().getIntExtra(Constants.Mode_Type, 0) != Constants.EDIT_MODE){
//            Get the data that was passed
            Log.d(TAG, "READ MODE");
            enableReadOnly();
        }
        setMode(getIntent().getIntExtra(Constants.Mode_Type, 0));

        loadProcedure(currentProcedure);
//        Hide the soft keyboard on load
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        createDialog();
    }

    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            changed = true;
        }
    };


    //  This function loads the Settings from Shared Preferences
    private String loadSettings(){
        SharedPreferences pref = getSharedPreferences(Constants.SHAREDPREF, MODE_PRIVATE);
        String ipaddress = pref.getString(Constants.IP_ADD, Constants.IP_DEFAULT);
        return ipaddress;
    }

    //    Setting colors depending on the mode
    private void setMode(int mode){
        if(mode == Constants.EDIT_MODE){
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                title_bar.setBackgroundColor(getColor(Constants.EDIT_COLOR));
            }else{
                title_bar.setBackgroundColor(getResources().getColor(Constants.EDIT_COLOR));
            }
        }else{
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                title_bar.setBackgroundColor(getColor(Constants.VIEW_COLOR));
            }else{
                title_bar.setBackgroundColor(getResources().getColor(Constants.VIEW_COLOR));
            }
        }
    }

    //    Disable editing if it's read only
    private void enableReadOnly(){
        subtitle_txt.setEnabled(false);
        edit_photo_btn.setVisibility(View.INVISIBLE);
        crop_img_btn.setVisibility(View.INVISIBLE);
        save_btn.setVisibility(View.INVISIBLE);
        delete_btn.setVisibility(View.INVISIBLE);
        explanation_txt.setEnabled(false);
        captureBtn.setVisibility(View.INVISIBLE);
        videoBtn.setVisibility(View.INVISIBLE);
        del_videoBtn.setVisibility(View.INVISIBLE);
    }


    //    Fetchs the Products from the L-One
    private void createDialog(){
//        Building the Alert Dialog
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(ProcedureActivity.this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogView = inflater.inflate(R.layout.splash_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);
        alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        Window window = alertDialog.getWindow();
        window.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);

        Button ok_btn = dialogView.findViewById(R.id.acceptbtn);
        TextView title = dialogView.findViewById(R.id.dialogtitle);
        title.setText(R.string.procedure_send);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setVisibility(View.VISIBLE);
        ok_btn.setVisibility(GONE);
    }

    private void showAlertDialog(){
        Button ok_btn = dialogView.findViewById(R.id.acceptbtn);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setVisibility(View.VISIBLE);
        ok_btn.setVisibility(View.GONE);
        alertDialog.show();
    }

//    Update the dialog
    private void updateDialog(String subtitletext){
        if (dialogView != null) {
            TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
            subtitle.setText(subtitletext);
        }
    }

    //    Called after the async task finishes
    private void tasksFinished(Boolean success, String text){
        Button ok_btn = dialogView.findViewById(R.id.acceptbtn);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setVisibility(GONE);
        TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
        ok_btn.setVisibility(View.VISIBLE);
        if(success) {
            subtitle.setText(text);
            alertDialog.dismiss();
        }else{
            subtitle.setText(text);
            ok_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                }
            });
        }
    }

//    **********************************************************
//      Data Control Functions
//    **********************************************************


    //    Loading the data needed for the selected procedure
    private void loadProcedure(Procedure currentProcedure){
//        Populating the widgets
        title.setText(currentProcedure.getProduct_name());
        subtitle_txt.setText(currentProcedure.getProcedure_title());
        explanation_txt.setText(currentProcedure.getProcedure_details());
//        Adding the path for the images
        filepath = new File(getExternalFilesDir(null), Constants.DATA_FOLDER);
//        Making photo path
        if (currentProcedure.getImage() != null) {
            File photo_path = new File(filepath, currentProcedure.getImage());
            mCurrentPhotoPath = photo_path.getAbsolutePath();
        }

//        Making video path
        if(currentProcedure.getVideo() != null){
            File video_path = new File(filepath, currentProcedure.getVideo());
            mCurrentVideoPath = video_path.getAbsolutePath();
        }else{
            mCurrentVideoPath = null;
        }

//        Setting Photo Images
        if(mCurrentPhotoPath != null && !mCurrentPhotoPath.equals("")) {
            setPic();
            changed = false;
        }else{
//            If this happens is a new procedure
            changed = true;
            Log.d(TAG, "No photo");
            mImageView.setVisibility(GONE);
            mImageView.setImageResource(0);
        }

//      Setting Video Paths
        if(mCurrentVideoPath != null) {
            setVideo(mCurrentVideoPath);
            videoImageView.setVisibility(View.VISIBLE);
            videoOverlay.setVisibility(View.VISIBLE);
        }else{
            videoImageView.setVisibility(GONE);
            videoOverlay.setVisibility(GONE);
        }
    }

//    Save the data into a text file and the procedure Object
    private boolean saveProcedure(){
        String subtitle = subtitle_txt.getText().toString();
        String explanation = explanation_txt.getText().toString();
//        Image Must need to have
        if (mCurrentPhotoPath != null) {
            File photofile = new File(mCurrentPhotoPath);
            currentProcedure.setImage(photofile.getName());
        }else{
            Log.d(TAG, "No picture here");
//            It needs a picture to continue
            return false;
        }
//        Video
        if(mCurrentVideoPath != null){
            File videofile = new File(mCurrentVideoPath);
            currentProcedure.setVideo(videofile.getName());
        }else{
            currentProcedure.setVideo(null);
        }

        currentProcedure.setProcedure_title(subtitle);
        currentProcedure.setProcedure_details(explanation);

        Log.d(TAG, "Saving here procedure " + currentProcedure.getObjectId());
        updateOrAddProcedure();

//        Update metadata
        String resp = updateMetadata();

        if (resp == null){
            return false;
        }
        return true;
    }

    // Updates or adds the new procedure into the list
    private void updateOrAddProcedure(){
        for (int i=0; i < procedureList.size(); i++){
            Log.d(TAG, "Procedure with id in list " + procedureList.get(i).getObjectId());
            if(procedureList.get(i).getObjectId() == currentProcedure.getObjectId()){
                Log.d(TAG, "Updating " + procedureList.get(i).getObjectId());
                procedureList.set(i, currentProcedure);
                return ;
            }
        }
//  Adding to list
        procedureList.add(currentProcedure);
    }

//    Updates the metadata.json file and writes to it
    private String updateMetadata(){
//        Update time
        Long tsLong = System.currentTimeMillis();
        selectedProduct.setModifiedTime(tsLong);
        selectedProduct.setProcedureList(procedureList);
        Log.d(TAG, "Procedure list size " + selectedProduct.getProcedureList().size());
        File metadata = new File(filepath, METADATA);
//        Delete the metadatafile
//        Update json file
        Gson gson = new Gson();
        String jsondata = gson.toJson(selectedProduct);
        Log.d(TAG, "Adding to metadata " + jsondata);
        FileOutputStream outputStream;
        try {
            Log.d(TAG, "Writing to file");
            outputStream = new FileOutputStream(metadata, false);
            outputStream.write(jsondata.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return jsondata;
    }

//  Delete the procedure from the list and folders
    private void deleteProcedure(Procedure procedure){
//        Checking if the image exists
//        Delete the Photo
        if (mCurrentPhotoPath != null) {
            File photofile = new File(mCurrentPhotoPath);
            if ( !photofile.delete()) {
//                If photo was not deleted successfully
                Toast.makeText(getApplicationContext(), "DEBUG : PHOTO COULDN'T BE DELETED", Toast.LENGTH_LONG ).show();
                Log.d(TAG, "No Photo but deleting");
            }
//          Remove Video
            if(mCurrentVideoPath != null){
                File videofile = new File(mCurrentVideoPath);
                if(! videofile.delete()){
                    Toast.makeText(getApplicationContext(), "DEBUG : VIDEO COULDN'T BE DELETED", Toast.LENGTH_LONG ).show();
                    Log.d(TAG, "No Video but deleting");
                }
            }
//            Removes the procedure from the product procedure list
                removeProcedureFromList(procedure);
//            Update metadata
                String jsondata = updateMetadata();
                Log.d(TAG, "REMOVE ITEM");
                if(jsondata != null) {
//              Inform the Server of delete
                    String url = Constants.HTTPHEADER + loadSettings() + removeProcedureurladdr;
                    HashMap<String, String> metadata = new HashMap<>();
                    metadata.put("metadata", jsondata);
                    updateDialog(getString(R.string.procedure_change));
                    showAlertDialog();
                    new SendData(ProcedureActivity.this, url, metadata, new SendData.SendResponse() {
                        @Override
                        public void transferComplete() {
                            Intent returnIntent = new Intent();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable(Constants.PROCEDURE_LIST, procedureList);
                            returnIntent.putExtra(Constants.PROCEDURE_LIST, bundle);
                            setResult(Activity.RESULT_OK, returnIntent);
                            tasksFinished(true, getApplicationContext().getString(R.string.procedure_delete_success));
                            finish();
                        }
                        @Override
                        public void onTransfer(int status){}
                        @Override
                        public void transferWithData(JSONArray data) {}
                        @Override
                        public void connectionLost(String error) {
//                            Show error here
                            Log.e(TAG, error);
//                            Toast.makeText(getApplicationContext(), error, Toast.LENGTH_LONG).show();
                            tasksFinished(false, error);
                        }
                    }).execute();
                }else{
//                    Write to file error
                    Toast.makeText(getApplicationContext(), "DEBUG : ERROR ON WRITING FILE", Toast.LENGTH_LONG ).show();
                }
        }else{
//                Do no need to do anything since it cannot be saved without a picture
            Log.e(TAG, "ERROR Image not found");
            Toast.makeText(ProcedureActivity.this, getResources().getText(R.string.delete_alert_finish), Toast.LENGTH_SHORT).show();
            Intent returnIntent = new Intent();
            Bundle bundle = new Bundle();
            bundle.putSerializable(Constants.PROCEDURE_LIST, procedureList);
            returnIntent.putExtra(Constants.PROCEDURE_LIST, bundle);
            setResult(Activity.RESULT_OK, returnIntent);
            finish();
        }
    }

    //    Deletes the procedure from the list
    private void removeProcedureFromList(Procedure procedure){
        int selectedid = procedure.getObjectId();
        int idtodelete = 99;

//        Removing the procedure from List
        for (int i=0; i<procedureList.size(); i++){
            if (procedureList.get(i).getObjectId() == selectedid){
                idtodelete =  i;
            }
        }
        if (idtodelete != 99) {
            procedureList.remove(idtodelete);
        }else{
            Log.d(TAG, "Nothing delete from Array");
        }
    }

//  Take picture intent
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
//                No need to delete picture here, it will be deleted on the server side
//                All data is deleted when the new product is selected
                Log.d(TAG, "New picture taken here");
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e(TAG, "Cannot Create File");
                Toast.makeText(getApplicationContext(), "ERROR : IMAGE ERROR", Toast.LENGTH_SHORT).show();
                ex.printStackTrace();
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.factory.procedure.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMG_CAPTURE);
            }
        }
    }

    //    Creates Image File to save
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
//        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File storageDir = filepath;
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        mTemporaryPhoto = image.getAbsolutePath();
        Log.d(TAG, "Image path " + mTemporaryPhoto);
        return image;
    }


//  Delete the video
    private void deleteVideo(){
        File videofile = new File(mCurrentVideoPath);
        Boolean delans = videofile.delete();
        if (delans){
//      Refresh the video
            mCurrentVideoPath = null;
            videoImageView.setVisibility(View.GONE);
            videoImageView.setImageDrawable(null);
            videoOverlay.setVisibility(View.GONE);
        }
    }

//    Might not need this
//    Sets a scaled down picture so the application doesn't run out of memory
    private void setPic() {
        if(mCurrentPhotoPath != null) {
            mImageView.setVisibility(View.VISIBLE);
            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
            BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            Bitmap bitmap = BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
            mImageView.setImageBitmap(bitmap);
        }
    }

    private void setVideo(String path) {
        Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
        videoImageView.setImageBitmap(bitmap);
    }

    private void saveUriToFile(Uri source){

        String sourceFilename= source.getPath();
        String destinationFilename = mCurrentPhotoPath;

        BufferedInputStream bis = null;
        BufferedOutputStream bos = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(sourceFilename));
            bos = new BufferedOutputStream(new FileOutputStream(destinationFilename, false));
            byte[] buf = new byte[8192];
            bis.read(buf);
            do {
                bos.write(buf);
            } while(bis.read(buf) != -1);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bis != null) bis.close();
                if (bos != null) bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //    OnclickListener for the buttons
    private View.OnClickListener mediabuttonListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            switch (v.getId()){
                case R.id.edit_img:
                    Log.d(TAG, "Edit image");
                    if(mCurrentPhotoPath != null) {
                        changed = true;
                        Intent editintent = new Intent(ProcedureActivity.this, EditImage.class);
                        // Pass data object in the bundle and populate details activity.
                        editintent.putExtra(MediaDetails.IMAGE_DETAIL, mCurrentPhotoPath);
                        startActivityForResult(editintent, REQUEST_IMG_EDIT);
                    }else{
                        Toast.makeText(getApplicationContext(), R.string.no_img, Toast.LENGTH_SHORT).show();
                    }
                    break;

                case R.id.crop_img:
//                    Apache Library
                    if(mCurrentPhotoPath != null) {
                        changed = true;
                        CropImage.activity(Uri.fromFile(new File(mCurrentPhotoPath)))
                                .start(ProcedureActivity.this);
                    }else{
                        Toast.makeText(getApplicationContext(), R.string.no_img, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case R.id.camera_btn:
//                    Do this check if it already has an image
                    if(mCurrentPhotoPath != null) {
                        changed = true;
                        new AlertDialog.Builder(ProcedureActivity.this, R.style.Theme_AppCompat_Dialog)
                                .setTitle(getResources().getText(R.string.overwrite_image_title))
                                .setMessage(getResources().getText(R.string.overwrite_explain))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        dispatchTakePictureIntent();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null).show();
                    }else{
                        dispatchTakePictureIntent();
                    }
                    break;
                case R.id.video_btn:
                    Log.d(TAG, "RECORD VIDEO");
                    if(mCurrentVideoPath != null){
                        changed = true;
                        new AlertDialog.Builder(ProcedureActivity.this, R.style.Theme_AppCompat_Dialog)
                                .setTitle(getResources().getText(R.string.overwrite_video_title))
                                .setMessage(getResources().getText(R.string.overwrite_explain))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                                        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                                            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
                                        }
                                    }
                                }).setNegativeButton(android.R.string.no, null).show();
                    }else{
                        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
                        takeVideoIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0);
                        takeVideoIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,180);
                        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
                            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
                        }
                    }
                    break;
                case R.id.delete_video_btn:
                    Log.d(TAG, "Delete Video");
                    if(mCurrentVideoPath != null) {
                        new AlertDialog.Builder(ProcedureActivity.this, R.style.Theme_AppCompat_Dialog)
                                .setTitle(getResources().getText(R.string.delete_video_title))
                                .setMessage(getResources().getText(R.string.overwrite_explain))
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        deleteVideo();
                                    }

                                }).setNegativeButton(android.R.string.no, null).show();
                    }

                    break;
                case R.id.preview_image:
                    Log.d(TAG, "Preview Image taken " + mCurrentPhotoPath);
                    final View imageview = findViewById(R.id.preview_image);
                    Intent imgintent = new Intent(ProcedureActivity.this, MediaDetails.class);
                    // Pass data object in the bundle and populate details activity.
                    imgintent.putExtra(MediaDetails.IMAGE_DETAIL, mCurrentPhotoPath);
                    ActivityOptionsCompat imgoptions = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(ProcedureActivity.this, imageview, "image");
                    startActivity(imgintent, imgoptions.toBundle());
                    break;
                case R.id.preview_video:
                    Log.d(TAG, "Preview Video taken");
                    final View videoview = findViewById(R.id.preview_video);
                    Intent videointent = new Intent(ProcedureActivity.this, MediaDetails.class);
                    // Pass data object in the bundle and populate details activity.
                    Uri videoUri = Uri.fromFile(new File(mCurrentVideoPath));
                    videointent.putExtra(MediaDetails.VIDEO_DETAIL, videoUri.toString());
                    ActivityOptionsCompat videooptions = ActivityOptionsCompat.
                            makeSceneTransitionAnimation(ProcedureActivity.this, videoview, "video");
                    startActivity(videointent, videooptions.toBundle());
                    break;
            }
        }
    };

    private View.OnClickListener buttonListener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
            switch (v.getId()) {
                case R.id.save_btn:
//                    Saving the procedure
                    showAlertDialog();
                    if(saveProcedure()) {
                        final File[] files;
//                    Creating resources needed for the sending to server
//                        It needs to have a picture to continue
                        File imgfile = new File(mCurrentPhotoPath);
                        File metadata = new File(filepath, METADATA);
                        if (mCurrentVideoPath != null && mCurrentPhotoPath != null) {
//                            showDialog(getString(R.string.zipfile_txt));
                            File videofile = new File(mCurrentVideoPath);
                            files = new File[]{imgfile, metadata, videofile};
                        }else{
                            files = new File[]{imgfile, metadata};
                        }
//                    Data to send to the server
                        HashMap<String, String> datatosend = new HashMap<>();
                        datatosend.put("id", String.valueOf(selectedProduct.getProductid()));
//                    Img and the metadata update to server
                        String url = Constants.HTTPHEADER + loadSettings() + addProcedureurladdr;

                        new SendData(ProcedureActivity.this, url, files, datatosend, new SendData.SendResponse() {
                            @Override
                            public void transferComplete() {
//                                    Sending the data to back to
                                changed = false;
                                Intent returnIntent = new Intent();
                                Bundle bundle = new Bundle();
                                bundle.putSerializable(Constants.PROCEDURE_LIST, procedureList);
                                returnIntent.putExtra(Constants.PROCEDURE_LIST, bundle);
                                setResult(Activity.RESULT_OK, returnIntent);
                                tasksFinished(true, getApplicationContext().getString(R.string.procedure_save));
                                Toast.makeText(ProcedureActivity.this, getText(R.string.procedure_save), Toast.LENGTH_SHORT).show();
                            }
                            @Override
                            public void onTransfer(int status){
                                switch (status){
                                    case SendData.SET_RESOURCES:
                                        updateDialog(getString(R.string.getting_res));
                                        break;
                                    case SendData.ZIP_RESOURCES:
                                        updateDialog(getString(R.string.zip_file));
                                        break;
                                    case SendData.POST_RESOURCES:
                                        updateDialog(getString(R.string.saving));
                                }
                            }
                            @Override
                            public void transferWithData(JSONArray data) {}

                            @Override
                            public void connectionLost(String errors) {
                                if (errors != null) {
                                    Log.e(TAG, errors);
                                    tasksFinished(false, errors);
                                }else{
                                    tasksFinished(false, getString(R.string.senddata_error));
                                }

                            }
                        }).execute();

                    } else {

//                        Send false result
                        if(mCurrentPhotoPath == null || mCurrentPhotoPath.equals("")){
//                        This is if there is no picture
                            tasksFinished(false, getString(R.string.image_error));
//                            Toast.makeText(getApplicationContext(), getText(R.string.image_error), Toast.LENGTH_SHORT).show();
                        }else{
//                          Metadata error
                            Log.d(TAG, "Couldn't write the metadata");
                            tasksFinished(false, "ERROR : Couldn't write metadata to save, try again.");
//                            Toast.makeText(getApplicationContext(), "ERROR : Couldn't write metadata to save, try again.", Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
                case R.id.delete_btn:
//                    On Delete, prompt the alert to confirm
                    new AlertDialog.Builder(ProcedureActivity.this, R.style.Theme_AppCompat_Dialog)
                            .setTitle(getResources().getText(R.string.delete_alert_title))
                            .setMessage(getResources().getText(R.string.delete_alert_explain))
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int whichButton) {
                                    deleteProcedure(currentProcedure);
                                }})
                            .setNegativeButton(android.R.string.no, null).show();
                    break;
                case R.id.back_btn:
                    finish();
                    break;
            }
        }
    };

    private void goBackProcedure(){
        int index;
        try {
            index = 0;
//      Checking the current index of the current item
            for (int i=0; i < procedureList.size(); i++){
                if (currentProcedure.getObjectId() == procedureList.get(i).getObjectId()){
                    index = i - 1;
                }
            }
            if(index < 0){
                index = 0;
                Toast.makeText(getApplicationContext(), getString(R.string.no_prev_procedure), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), getString(R.string.next_procedure), Toast.LENGTH_SHORT).show();
            }
            currentProcedure = procedureList.get(index);
            loadProcedure(currentProcedure);
        }catch (NullPointerException e){
            Log.e(TAG, "No more procedures");
        }catch (Exception ex){
            Log.e(TAG, "No procedures");
        }
    }

    private void nextProcedure(){
        int index;
        try {
            index = 0;
//                        Checking the current index of the current item
            for (int i=0; i < procedureList.size(); i++){
                if (currentProcedure.getObjectId() == procedureList.get(i).getObjectId()){
                    index = i + 1;
                }
            }
//            Max achieved
            if(index > procedureList.size() -1){
                index = procedureList.size() -1;
                Toast.makeText(getApplicationContext(), getString(R.string.no_next_procedure), Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getApplicationContext(), getString(R.string.prev_procedure), Toast.LENGTH_SHORT).show();
            }
            currentProcedure = procedureList.get(index);
            loadProcedure(currentProcedure);
        }catch (NullPointerException e){
            Log.e(TAG, "No more procedure");
        }catch (Exception ex){
            Log.e(TAG, "No procedures");
        }
    }

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            // TODO Auto-generated method stub
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    x1 = event.getX();
                    return true;
                case MotionEvent.ACTION_UP:
                    x2 = event.getX();
                    float deltaX = x2 - x1;
                    //check if the user's funger has moved significantly. Tiny bit of movement is very usual and common during touch, so >0 and <0 won't work.
                    if (deltaX < -210) {
                        if (changed){
                            Toast.makeText(getApplicationContext(), getText(R.string.save_procedure), Toast.LENGTH_SHORT).show();
                        }else {
                            nextProcedure();
                        }
                    } else if (deltaX > 210) {
                        //Swipes LtoR move to the previous month
                        if (changed){
                            Toast.makeText(getApplicationContext(), getText(R.string.save_procedure), Toast.LENGTH_SHORT).show();
                        }else {
                            goBackProcedure();
                        }
                    } else if (deltaX > -210 && deltaX < 210) { //Adjust those numbers for your needs
//                       ON PRESS DO NOTHING
                    }
                    break;
            }

            return false;
        }
    };


    //    This is called on the camera activity to set the picture into the imageView
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMG_CAPTURE && resultCode == RESULT_OK) {
//            Set the Image Path here
            mCurrentPhotoPath = mTemporaryPhoto;
            setPic();
        }
        if(requestCode == REQUEST_IMG_CAPTURE && resultCode == RESULT_CANCELED){
            Log.d(TAG, "Cancelled by user");
//            Remove the empty photo
            File photofile = new File(mTemporaryPhoto);
            photofile.delete();
        }

        if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            showAlertDialog();
            updateDialog(getString(R.string.compress_video));
            Uri videoUri = data.getData();

            new CompressTask(ProcedureActivity.this, videoUri, new CompressTask.TaskUpdate() {
                @Override
                public void onFinish(File compressed_result) {
                    tasksFinished(true, getString(R.string.video_success));
                    mCurrentVideoPath = compressed_result.getAbsolutePath();
                    videoImageView.setVisibility(View.VISIBLE);
                    videoOverlay.setVisibility(View.VISIBLE);
                    setVideo(mCurrentVideoPath);
                }

                @Override
                public void onError() {
                    tasksFinished(false, getString(R.string.video_error));
                }
            }).execute();


        }

//        Crop finished
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();
                Log.d(TAG, "Success");
                saveUriToFile(resultUri);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }


    @Override
    protected void onResume() {
        setPic();
        super.onResume();
    }
}
