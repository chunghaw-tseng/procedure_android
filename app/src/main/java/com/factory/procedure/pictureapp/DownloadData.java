package com.factory.procedure.pictureapp;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * This class is used for requesting a download from a URL (Zip File)
 */
public class DownloadData extends AsyncTask<Void, Integer, Void> {

    private String TAG = "DownloadData";
    public static final int DOWNLOAD_STATE = 1;
    public static final int SAVEFILE_STATE = 2;
    public static final int UNZIP_STATE = 3;
    public static final int UNZIP_PHOTO = 4;
    public static final int UNZIP_METADATA = 5;
    public static final int UNZIP_VIDEOS = 6;
    public static final int CLEAN_STATE = 7;

    private Context mContext;
    private String url, workingpath, datapath;
    private static int BUFFER_SIZE = 8192;
    private Integer fileCount;
    private Product product;
    private boolean success= false;
    private String response;

    private int per = 0;

    //    Interface for the actions after the async task is finished
    private DownloadResponse delegate = null;

//  Interface for handling the download complete
    public interface DownloadResponse{
        void donwloadComplete();
        void onDownloadUpdate(Integer[] state);
        void connectionLost(String error);
    }

//  Constructor
    public DownloadData(Context context, String url, Product selected, DownloadResponse delegate){
        this.mContext = context;
        this.url = url;
        this.workingpath =  new File(context.getExternalFilesDir(null), Constants.WORKING_FOLDER).getAbsolutePath();
        this.datapath = new File(context.getExternalFilesDir(null), Constants.DATA_FOLDER).getAbsolutePath();
        this.delegate = delegate;
        this.product = selected;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
//        TODO Check the values
        delegate.onDownloadUpdate(values);
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {
//            Creating the URL

            Log.d(TAG, "The url is " + url);
            Uri uri = Uri.parse(url);
            Uri.Builder builder = new Uri.Builder();
            builder.scheme("http")
                    .authority(uri.getAuthority())
                    .appendPath("plugins")
                    .appendPath("chn1802")
                    .appendPath("getProductData")
                    .appendQueryParameter("productid", String.valueOf(product.getProductid()));

            Log.d(TAG, builder.toString());

            publishProgress(DOWNLOAD_STATE, per);
//            Starting connection
            HttpURLConnection con = (HttpURLConnection) ( new URL(builder.toString())).openConnection();
            con.setRequestProperty("connection", "close");
            System.setProperty ( "http.keepAlive " , "false") ;
            con.setRequestMethod("POST");
            con.setConnectTimeout(50000);
            con.setReadTimeout(50000);
//            Added
            con.setChunkedStreamingMode(1024);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();
            int responseCode = con.getResponseCode();

            switch(responseCode){
                case HttpURLConnection.HTTP_OK:
                    per = per + 20;
                    publishProgress(SAVEFILE_STATE, per);

                    Log.d(TAG, con.getResponseMessage());
//                    Get the number of files from the header field
                    String[] result = con.getHeaderField("Content-Disposition").split("\"");
                    String[] filename = result[1].split("\\.");
                    fileCount = Integer.parseInt(filename[0]);
                    Log.d(TAG, "Files " + fileCount);

                    byte[] buffer = new byte[1024];
                    per = per + 10;
                    publishProgress(UNZIP_STATE, per);
                    int add = 60/ fileCount + 1;
                    File outputFilePath = new File  (datapath + "/");
                    try {
                        ZipInputStream zipInputStream = new ZipInputStream(con.getInputStream());
                        BufferedInputStream in = new BufferedInputStream(zipInputStream);
                        ZipEntry zipEntry = zipInputStream.getNextEntry();
                        while(zipEntry != null){
                            File newFile = new File(outputFilePath, zipEntry.getName());
                            Log.i("zip file path = ", newFile.getPath());
//                            For the status bar
                            if(zipEntry.getName().contains(".jpg")){
                                publishProgress(UNZIP_PHOTO, per);
                            }else if (zipEntry.getName().contains(".json")){
                                publishProgress(UNZIP_METADATA, per);
                            }else{
                                publishProgress(UNZIP_VIDEOS, per);
                            }
                            if (!zipEntry.isDirectory()) {
                                FileOutputStream fos = new FileOutputStream(newFile);
                                BufferedOutputStream bufout = new BufferedOutputStream(fos);
                                int read = 0;
                              while ((read = in.read(buffer)) != -1) {
                                    bufout.write(buffer, 0, read);
                                }
                                bufout.close();
                                fos.close();
                            } else {
                                newFile.mkdirs();
                            }
                            zipInputStream.closeEntry();
                            zipEntry = zipInputStream.getNextEntry();
                            per = per + add;
                        }
                        // Close Stream and disconnect HTTP connection. Move to finally
                        zipInputStream.closeEntry();
                        zipInputStream.close();

                    }catch (IOException e){
                        response = "UNZIP ERROR";
                        e.printStackTrace();
                    }

                    Log.d(TAG, "Finished Stream to File");

                    success = true;
                    publishProgress(CLEAN_STATE, 100);
                    break;
//                    Error message
                case HttpURLConnection.HTTP_INTERNAL_ERROR:
                    Log.e(TAG, "Internal Error");
                    readHTTPMessage(con.getErrorStream());
                    break;
//                    Error message
                case HttpURLConnection.HTTP_BAD_REQUEST:
                    Log.e(TAG, "Response code : " + responseCode);
                    readHTTPMessage(con.getErrorStream());
                    break;
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                    Log.e(TAG, "Response code : " + responseCode);
                    response = mContext.getString(R.string.gateway_error);
                    break;
                case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                    Log.e(TAG, "Client Timeout");
                    response = mContext.getString(R.string.timeout_error);
                default:
                    Log.e(TAG, "Response code : " + responseCode);
                    break;
            }
        }
        catch(IOException e) {
            Log.d(TAG, "IO Exception");
            e.printStackTrace();
            response = mContext.getString(R.string.connection_error);
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        Log.d(TAG, "Finished post");
        if(success) {
//         Remove the files in the working folder
            Log.d(TAG, "Deleting WORKING_FOLDER");
            deleteFolderContents(new File(mContext.getExternalFilesDir(null), Constants.WORKING_FOLDER));
            delegate.donwloadComplete();
        }else{
            delegate.connectionLost(response);
        }
    }

    @Override
    protected void onCancelled(Void aVoid) {
        super.onCancelled(aVoid);
    }

    //    Read the http response and returns as a String
    private void readHTTPMessage(InputStream inputStream){
        try {
//            If different Encoding needed
//            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseStream, "Shift-JIS"));
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
            String line = "";
            StringBuilder stringBuilder = new StringBuilder();
            while ((line = responseStreamReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            responseStreamReader.close();
            response = stringBuilder.toString();

//            This is for the encoding
            OutputStream out = new ByteArrayOutputStream(1024);
            Writer writer = new OutputStreamWriter(out, "UTF-8");
            writer.write(response);
            writer.close();
            Log.d(TAG, "response " + response);
        }catch (UnsupportedEncodingException exception){
            Log.e(TAG, "Encoding Error");
        }catch (IOException ioexception){
            Log.e(TAG, "Reading Data Error");
        }
    }



    //This function will unzip the zip folder
    public static void unzip(String zipFile, String location) throws IOException {
        int size;
        byte[] buffer = new byte[BUFFER_SIZE];

        try {
            if ( !location.endsWith("/") ) {
                location += "/";
            }
            File f = new File(location);
            if(!f.isDirectory()) {
                f.mkdirs();
            }
            ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), BUFFER_SIZE));
            try {
                ZipEntry ze = null;
                while ((ze = zin.getNextEntry()) != null) {
                    String path = location + ze.getName();
                    File unzipFile = new File(path);

                    if (ze.isDirectory()) {
                        if(!unzipFile.isDirectory()) {
                            unzipFile.mkdirs();
                        }
                    } else {
                        // check for and create parent directories if they don't exist
                        File parentDir = unzipFile.getParentFile();
                        if ( null != parentDir ) {
                            if ( !parentDir.isDirectory() ) {
                                parentDir.mkdirs();
                            }
                        }

                        // unzip the file
                        FileOutputStream out = new FileOutputStream(unzipFile, false);
                        BufferedOutputStream fout = new BufferedOutputStream(out, BUFFER_SIZE);
                        try {
                            while ( (size = zin.read(buffer, 0, BUFFER_SIZE)) != -1 ) {
                                fout.write(buffer, 0, size);
                            }

                            zin.closeEntry();
                        }
                        finally {
                            fout.flush();
                            fout.close();
                        }
                    }
                }
            }
            finally {
                zin.close();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.e("ZIP", "Unzip exception or not file exist");
        }
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



