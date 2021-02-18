package com.factory.procedure.pictureapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

/**
 * This class is the launcher class
 * This class will take care of creating the files needed for the application to run
 */


public class Launcher extends AppCompatActivity{

//    Buttons needed
    private Button editbtn, viewbtn, settingsbtn;
    private static String TAG = "Launcher";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.launcher);

        editbtn = findViewById(R.id.edit_btn);
        editbtn.setOnClickListener(buttonListener);
        viewbtn = findViewById(R.id.view_btn);
        viewbtn.setOnClickListener(buttonListener);
        settingsbtn = findViewById(R.id.settings_btn);
        settingsbtn.setOnClickListener(buttonListener);


        checkSharedPrefs();
        checkApplicationFiles();
    }


//    Checking the Shared Preferences
    private void checkSharedPrefs(){
        SharedPreferences pref = getSharedPreferences(Constants.SHAREDPREF, MODE_PRIVATE);
        if(! pref.contains(Constants.IP_ADD)){
            SharedPreferences.Editor editor = pref.edit();
            editor.putString(Constants.IP_ADD, Constants.IP_DEFAULT);
            editor.commit();
        }
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }


//    Check Files
    private void checkApplicationFiles() {
        if(isExternalStorageWritable() && isExternalStorageReadable()) {
           File file = new File(getExternalFilesDir(null), Constants.DATA_FOLDER);
           if (!file.mkdirs()) {
               Log.e(TAG, "Directory not created");
           }
           File working = new File(getExternalFilesDir(null), Constants.WORKING_FOLDER);
            if (!working.mkdirs()) {
                Log.e(TAG, "Working dir not created");
            }
       }
    }



    // The click listener for the buttons
    // If edit more change the title color to green if not orange
    private View.OnClickListener buttonListener = new View.OnClickListener() {
        Intent intent;

        @Override
        public void onClick(View v) {
            switch (v.getId()){
                case R.id.edit_btn:
//                    If Edit mode
                    intent = new Intent(getApplicationContext(), ProductSelectionActivity.class);
                    intent.putExtra(Constants.Mode_Type, Constants.EDIT_MODE);
                    startActivity(intent);
                    break;
                case R.id.view_btn:
//                    Else View Mode
                    intent = new Intent(getApplicationContext(), ProductSelectionActivity.class);
                    intent.putExtra(Constants.Mode_Type, Constants.VIEW_MODE);
                    startActivity(intent);
                    break;
                case R.id.settings_btn:
                    intent = new Intent(getApplicationContext(), SettingsActivity.class);
                    startActivity(intent);
                    break;
            }
        }
    };
}

