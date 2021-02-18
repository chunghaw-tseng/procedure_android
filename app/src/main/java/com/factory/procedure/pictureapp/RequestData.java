package com.factory.procedure.pictureapp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * This async task will request data from the server (JSON not files)
 */

public class RequestData extends AsyncTask<Void, Void, Void> {

    private final String TAG = "Requesting Data";
    private Context context;
    private String url;
    private String response;
    private Boolean success = false;

    //    Interface for the actions after the async task is finished
    private DataResponse delegate = null;


    public interface DataResponse{
        void processData(JSONArray output);
        void connectionLost(String error);
    }

//    Constructor
    public RequestData(Context mContext, String url, DataResponse delegate){
        super();
        this.context = mContext;
        this.url = url;
        this.delegate = delegate;
    }

    @Override
    protected Void doInBackground(Void... voids) {
//        Send post here
        try {
//            Establishing connection
            HttpURLConnection con = (HttpURLConnection) ( new URL(url)).openConnection();
            con.setRequestProperty("connection", "close");
            System.setProperty ( "http.keepAlive " , "false") ;
            con.setRequestProperty("Authorization", "Basic " + Base64.encodeToString("user:".getBytes(), Base64.NO_WRAP));
            con.setRequestMethod("POST");
            con.setConnectTimeout(1000);
            con.setReadTimeout(3000);
            //            Added
            con.setChunkedStreamingMode(1024);
            con.setDoInput(true);
            con.setDoOutput(true);
            con.connect();
            int responseCode = con.getResponseCode();
            InputStream responseStream;
            switch(responseCode){
                case HttpURLConnection.HTTP_OK:
                    Log.d(TAG, "the response code is " + responseCode);
//                    Parse the message here
                    responseStream = new BufferedInputStream(con.getInputStream());
                    readHTTPMessage(responseStream);
                    success = true;
                    break;
                case HttpURLConnection.HTTP_BAD_REQUEST:
                    Log.e(TAG, "Bad Request");
//                    Parse the message here
                    responseStream = con.getErrorStream();
                    readHTTPMessage(responseStream);
                    Log.d(TAG , "Bad request was parsed");
                    break;
                case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                    Log.e(TAG, "Client Timeout");
                    response = context.getString(R.string.timeout_error);
                    break;
                case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
                    Log.e(TAG, "Gateway Timeout");
                    response = context.getString(R.string.gateway_error);
                    break;
                default:
                    Log.e(TAG, "Response code : " + responseCode);
                    response = "Server Sent " + responseCode;
                    break;
            }

//            Get the message
            Log.d(TAG, "response " + response);
            con.disconnect();
//            Technically this shouldn't hit
        }  catch(MalformedURLException e) {
            Log.d(TAG, "Incorrect URL");
            e.printStackTrace();
            response = context.getString(R.string.connection_error);
        } catch (IOException ioexception){
            Log.d(TAG, "IO Exception");
            ioexception.printStackTrace();
            response = context.getString(R.string.gateway_error);
        }
        return null;
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    @Override
    protected void onCancelled(Void aVoid) {
        super.onCancelled(aVoid);
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
//        Need to check if it's a zip file
        Log.d(TAG, "Data was received");
        if(success) {
            try {
                JSONArray jArray = new JSONArray(response);
//                Data is passed to the activity
                delegate.processData(jArray);
            } catch (JSONException e) {
                Log.e(TAG, "No Data Received");
                e.printStackTrace();
                response = context.getString(R.string.corrupted_error);
                delegate.connectionLost(response);
            }
        }else{
            delegate.connectionLost(response);
        }
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
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



}
