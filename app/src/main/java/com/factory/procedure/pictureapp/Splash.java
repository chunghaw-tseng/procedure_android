package com.factory.procedure.pictureapp;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;



/**
 * This class shows the Splash screen and does the permissions
 */
public class Splash extends AppCompatActivity {

    private static String TAG = "Splash";

    //    permissions
    private static final int PERMISSIONS_REQUEST_ACCESS_CAMERA = 1;
    private static final int PERMISSIONS_REQUEST_ACCESS_MICROPHONE = 2;
    private static final int PERMISSIONS_REQUEST_ACCESS_STORAGE = 3;
    private static final int REQUEST_MULTI_PERMISSIONS = 1;
    Permission camera, microphone, ex_storage;
    ArrayList<Permission> app_permissions;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //        Checking hardware
        if(!checkCameraHardware(this)){
            Log.e(TAG, "No camera available");
        }

//       Checking permissions
        camera = new Permission(Manifest.permission.CAMERA, getString(R.string.camera_rationale), PERMISSIONS_REQUEST_ACCESS_CAMERA);
        microphone = new Permission(Manifest.permission.RECORD_AUDIO, getString(R.string.microphone_rationale), PERMISSIONS_REQUEST_ACCESS_MICROPHONE);
        ex_storage = new Permission(Manifest.permission.WRITE_EXTERNAL_STORAGE, getString(R.string.ex_storage_rationale), PERMISSIONS_REQUEST_ACCESS_STORAGE);
        app_permissions = new ArrayList<>();
        app_permissions.add(camera);
        app_permissions.add(microphone);
        app_permissions.add(ex_storage);

//        Checking starting components
        checkApplicationPermissions();

    }



    /** Check if this device has a camera hardwarewise*/
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }


    //Check Permissions
    private void checkApplicationPermissions(){

        ArrayList<Permission> required_permissions = new ArrayList<>();
        boolean need_explain=false;

        // Check the status of each application permission
        for (Permission permission: app_permissions) {
            int permissionCheck;
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                permissionCheck = ContextCompat.checkSelfPermission(getApplicationContext(), permission.permission);
            }else{
                permissionCheck = PermissionChecker.checkSelfPermission(getApplicationContext(), permission.permission);
            }

            if (permissionCheck != PackageManager.PERMISSION_GRANTED){

                required_permissions.add(permission);
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission.permission)) {
                    need_explain=true;
                }

            }

        }

        if(required_permissions.size()==0){
            Log.d(TAG,"Start next activity.");
            Intent intent = new Intent(getApplicationContext(), Launcher.class);
            startActivity(intent);
            finish();
            return;
        }


        Log.d(TAG,"required permissions:" + required_permissions);
        final String[] pms_str = new String[required_permissions.size()];
        int i=0;
        for(Permission p:required_permissions){
            pms_str[i] = p.permission;
            i++;
        }


        if(need_explain){
            permissions_explain(pms_str);
        }else{
           ActivityCompat.requestPermissions(Splash.this, pms_str, 1);
        }


    }


    //  Showing reason for the permissions
    private void permissions_explain(final String[] pms_str){


        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(Splash.this);
        builder.setTitle("Permissions required")
                .setMessage("In this application, permission to write camera, microphone, external storage is required.")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ActivityCompat.requestPermissions(Splash.this, pms_str, 1);
                    }
                }).setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Toast.makeText(getBaseContext(), getString(R.string.msg_user_denied_permission), Toast.LENGTH_LONG).show();
                        finish();
                    }
                }
        );

        builder.create().show();
    }


    //    On Permission Result callback
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        Log.d(TAG,"(onRequestPermissionsResult");
        int req_perms_num = permissions.length;
        int resolved_num = 0;

        if(requestCode==REQUEST_MULTI_PERMISSIONS){

            for(int i=0;i < permissions.length;i++){

                if(permissions[i].equals(Manifest.permission.CAMERA)){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                        Log.d(TAG, "RESULT : CAMERA was granted");
                        resolved_num++;
                    }else{
                        Log.d(TAG, "RESULT : CAMERA was not granted");
                    }


                }else if(permissions[i].equals(Manifest.permission.RECORD_AUDIO)){

                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                        Log.d(TAG, "RESULT : RECORD_AUDIO was granted");
                        resolved_num++;
                    }else{
                        Log.d(TAG, "RESULT : RECORD_AUDIO was not granted");
                    }


                }else if(permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED){
                        Log.d(TAG, "RESULT : WRITE_EXTERNAL_STORAGE was granted");
                        resolved_num++;
                    }else{
                        Log.d(TAG, "RESULT : WRITE_EXTERNAL_STORAGE was not granted");
                    }

                }



            }

            if(req_perms_num==resolved_num){
                Intent intent = new Intent(getApplicationContext(), Launcher.class);
                startActivity(intent);
                finish();

            }else{
                Toast.makeText(getBaseContext(), getString(R.string.msg_user_denied_permission), Toast.LENGTH_LONG).show();
                finish();
            }

        }

    }
}




/**
 * Storage class for permissions used by this app.
 */
class Permission {
    private static int nextid=0;
    // Autogenerated
    int id;
    // System permission
    String permission;
    // Permission explanation for user
    String rationale;

    Permission(String permission, String rationale, int id){
        this.permission = permission;
        this.rationale = rationale;
        this.id = id;
    }
}
