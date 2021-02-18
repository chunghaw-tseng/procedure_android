package com.factory.procedure.pictureapp;

import android.util.Log;

import java.io.Serializable;

/**
 * Procedure class to store the details for a procedure
 */

public class Procedure implements Serializable {

    private static final String TAG = "Procedure Class";
//    Variables to save
    private final int objectId;
    private String image;
    private String video;
    private String recording_sound;
    private String procedure_details;
    private String procedure_title;
    private String product_name;
    private static int idNo = 0;

//    These are used for the DiffUtil on the RecyclerView
//    Keys to know what has been changed on the list
    public static String IMAGE_KEY = "Image";
    public static String NAME_KEY = "Name";
    public static String CONTENTS_KEY = "Contents";

//    Constructors
    Procedure(String title, String explanation, String product_name){
        this.procedure_details = explanation;
        this.procedure_title = title;
        this.product_name = product_name;
        this.objectId = idNo ++;
        Log.d(TAG, "Adding new procedure with id " + this.objectId);
    }
    Procedure(String title, String explanation, String image, String product_name) {
        this.procedure_details = explanation;
        this.procedure_title = title;
        this.product_name = product_name;
        this.image = image;
        this.objectId = idNo ++;
    }

//    Getter and Setter functions here
    public static void setIdNo(int idNo) {
        Procedure.idNo = idNo;
    }
    public int getObjectId() {
        return objectId;
    }
    public String getProcedure_details() {
        return procedure_details;
    }
    public void setProcedure_details(String procedure_details) {
        this.procedure_details = procedure_details;
    }
    public String getVideo(){return video;}
    public void setVideo(String video){this.video = video;}
    public String getImage() {
        return image;
    }
    public void setImage(String image) {
        this.image = image;
    }
    public String getProduct_name() {
        return product_name;
    }
    public String getProcedure_title() {
        return procedure_title;
    }
    public void setProcedure_title(String procedure_title) {
        this.procedure_title = procedure_title;
    }



    //    NOT USED IN THIS VERSION
    public String getRecording_sound() {
        return recording_sound;
    }

    public void setRecording_sound(String recording_sound) {
        this.recording_sound = recording_sound;
    }
}
