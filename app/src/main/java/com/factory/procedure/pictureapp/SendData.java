package com.factory.procedure.pictureapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Async Task that will send the data to the server
 */

public class SendData extends AsyncTask<Void, Integer, Void> {

    private String TAG = "Send Data";
    private Context context;
    private String url;
    private HashMap<String, String> dataPacket;
    private File[] file;

    public static final int SET_RESOURCES = 0;
    public static final int ZIP_RESOURCES = 1;
    public static final int POST_RESOURCES = 2;


    //For the post command
    private String charset = "UTF-8";
    private String response;
    private HTTPAnswer httpAnswer;
    private Boolean success = false;


    //    Interface for the actions after the async task is finished
    private SendResponse delegate = null;

    public interface SendResponse{
        void onTransfer(int status);
        void transferComplete();
        void transferWithData(JSONArray data);
        void connectionLost(String errors);
    }


//    Constructors
//  File and text to send
    public SendData(Context mContext, String url , File[] filedata, HashMap<String ,String> data, SendResponse delegate){
        this.context = mContext;
        this.url = url;
        this.file = filedata;
        this.dataPacket = data;
        this.delegate = delegate;
    }

    public SendData(Context mContext, String url , HashMap<String ,String> data, SendResponse delegate){
        this.context = mContext;
        this.url = url;
        this.dataPacket = data;
        this.delegate = delegate;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected Void doInBackground(Void... voids) {

//        Check if the data is files or not
        publishProgress(SET_RESOURCES);
        try {
//            Creating multipart POST
            Multipartutility multipart = new Multipartutility(url, charset);
            multipart.addHeaderField("User-Agent", "KukiApp");
//            multipart.addHeaderField("Test-Header", "Header-Value");
            Log.d(TAG, "Adding data");
            if (dataPacket != null) {
                Iterator it = dataPacket.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    multipart.addFormField(String.valueOf(pair.getKey()), String.valueOf(pair.getValue()));
                }
            }
//            Send the output
//            Send outputstream
            if(file != null) {
                publishProgress(ZIP_RESOURCES);
//                Figure the size of the video
//                If video is too large, it has to be turned into a file
                if(file.length == 3){
                    Log.i(TAG, "Size " + String.valueOf(file[2].length()/1024));
                    if((file[2].length()/1024) > 100000){
                        Log.i(TAG, "File too large must be an error");
                        File destfile = new File(context.getExternalFilesDir(null), Constants.WORKING_FOLDER + File.separator + dataPacket.get("id") + ".zip");
                        if(createZipFile(file, destfile)) {
                            Log.i(TAG, "Create from file");
                            multipart.addFilePart("file", destfile);
                        }
                    }else{
                        Log.i(TAG, "Create from stream");
                        multipart.addFilePart("file", dataPacket.get("id"), createZipStream(file));
                    }
                }else {
                    Log.i(TAG, "Create from stream");
                    multipart.addFilePart("file", dataPacket.get("id"), createZipStream(file));
                }
            }

            Log.d(TAG, "Posting");
//            Send the POST
            publishProgress(POST_RESOURCES);
            httpAnswer = multipart.finishwithString();
            switch (httpAnswer.getConnetioncode()){
                case HttpURLConnection.HTTP_OK:
                    success = true;
                    response = httpAnswer.getMessage();
                    break;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                    response = httpAnswer.getMessage();
                    break;
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                    response = context.getString(R.string.gateway_error);
                    break;
                case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                    response = context.getString(R.string.timeout_error);
                    break;
                default:
                    break;
            }

            Log.d(TAG, "Sent Complete");
//          The response will be True if correct and False if not
//           Check if the response is correct on the PostExecute
            Log.d(TAG, "The response is " + response);

        } catch (IOException ex) {
            Log.e(TAG, "Error has happened");
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        delegate.onTransfer(values[0]);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(success){
            try{
                Log.d(TAG, response);
                JSONArray jsonArray = new JSONArray(response);
                delegate.transferWithData(jsonArray);
                return;
            }catch (JSONException e){
                Log.e(TAG, "Error Data cannot be parsed, might be empty (Success)");
            }
            deleteFolderContents(new File(context.getExternalFilesDir(null), Constants.WORKING_FOLDER));
            delegate.transferComplete();
        }else{
            delegate.connectionLost(response);
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    private boolean createZipFile(File[] filessource, File toLocation){
        final int BUFFER = 1024;
        BufferedInputStream origin;
        try{
            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream zos = new ZipOutputStream(dest);

            for (File f : filessource) {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(f);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(f.getName());
                zos.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    zos.write(data, 0, count);
                }
                Log.i("zip file path = ", f.getPath());
            }
            zos.close();
        }catch (FileNotFoundException fe){
            fe.printStackTrace();
            return false;
        }catch (IOException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }


    // IMPORTANT This throws an out of memory error if the size is too large
    //  Creates the zipfile to send to the server
    private ByteArrayOutputStream createZipStream(File[] filessource){
        final int BUFFER = 1024;
        BufferedInputStream origin;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        try{
//            FileOutputStream dest = new FileOutputStream(toLocation);
            ZipOutputStream zos = new ZipOutputStream(byteArrayOutputStream);

            for (File f : filessource) {
                byte data[] = new byte[BUFFER];
                FileInputStream fi = new FileInputStream(f);
                origin = new BufferedInputStream(fi, BUFFER);
                ZipEntry entry = new ZipEntry(f.getName());
                zos.putNextEntry(entry);
                int count;
                while ((count = origin.read(data, 0, BUFFER)) != -1) {
                    zos.write(data, 0, count);
                }
                Log.i("zip file path = ", f.getPath());
            }
            zos.close();
        }catch (FileNotFoundException fe){
            fe.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        return byteArrayOutputStream;
    }



    //    Delete Folder Contents After everything is finished
    private void deleteFolderContents(File folderdir){
        if (folderdir.isDirectory()){
            String[] foldercontents = folderdir.list();
            for(int i=0; i <foldercontents.length; i++){
                if( new File(folderdir, foldercontents[i]).delete()){
                    Log.d(TAG, "File deleted" );
                }else{
                    Log.d(TAG, "File not deleted");
                }
            }
        }
    }

}
