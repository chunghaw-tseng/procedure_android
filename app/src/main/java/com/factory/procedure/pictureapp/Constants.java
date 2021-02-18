package com.factory.procedure.pictureapp;

/**
 * Constants class that can be used for the whole project
 * These constants cannot be changed
 */

public final class Constants {

//    Strings TAGs used for bundles
     final static String Mode_Type = "Mode";
     final static String PROCEDURE = "Procedure";
     final static String PROCEDURE_LIST = "List_Procedure";
     final static String PRODUCT = "Product";

//     TAGs to show the current viewing mode
     final static int EDIT_MODE = 0;
     final static int VIEW_MODE = 1;


//     Folder Path Name
     final static String DATA_FOLDER = "Products";
     final static String WORKING_FOLDER = "Working";
     final static String METADATA = "metadata.json";

    //    Colors so it's easier to know what mode you are in
     final static int EDIT_COLOR = R.color.tangerine;
     final static int VIEW_COLOR = R.color.electric;

//     Color for folders


//     For the shared prefs
     final static String IP_DEFAULT = "192.168.1.151";
     final static String SHAREDPREF = "Settings";
     final static String IP_ADD = "IPAdd";
     final static String HTTPHEADER = "http://";

//　Not used constructor
    private Constants(){}
}
