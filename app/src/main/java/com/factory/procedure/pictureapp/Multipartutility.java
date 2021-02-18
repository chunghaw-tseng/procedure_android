package com.factory.procedure.pictureapp;


import android.util.Log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;


// Class created to make a MultipartUtility POST

public class Multipartutility {

    private final String TAG = "MultipartUtility";
    private final String boundary;
    private static final String LINE_FEED = "\r\n";
    private HttpURLConnection httpConn;
    private String charset;
    private OutputStream outputStream;
    private PrintWriter writer;


    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     * @param requestURL
     * @param charset
     * @throws IOException
     */
    public Multipartutility(String requestURL, String charset)
            throws IOException {
        this.charset = charset;

        // creates a unique boundary based on time stamp
        boundary = "---" + System.currentTimeMillis() + "---";

//        Creating URL
        URL url = new URL(requestURL);
        httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + boundary);
        httpConn.setRequestProperty("User-Agent", "CodeJava Agent");
        httpConn.setRequestProperty("Procedure App", "Save");
        outputStream = httpConn.getOutputStream();
        writer = new PrintWriter(new OutputStreamWriter(outputStream, charset),
                true);
    }

    /**
     * Adds a form field to the request
     * @param name field name
     * @param value field value
     */
    public void addFormField(String name, String value) {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
                .append(LINE_FEED);
        writer.append("Content-Type: text/plain; charset=" + charset).append(
                LINE_FEED);
        writer.append(LINE_FEED);
        writer.append(value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a upload file section to the request
     * @param fieldName name attribute in <input type="file" name="..." />
     * @param uploadFile a File to be uploaded
     * @throws IOException
     */
    public void addFilePart(String fieldName, File uploadFile)
            throws IOException {
        String fileName = uploadFile.getName();
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append(
                "Content-Disposition: form-data; name=\"" + fieldName
                        + "\"; filename=\"" + fileName + "\"")
                .append(LINE_FEED);
        writer.append(
                "Content-Type: "
                        + URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(uploadFile);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();
    }

//    New way by using bytestream
    public void addFilePart(String fieldName, String fileName, ByteArrayOutputStream uploadFile)
            throws IOException {
        writer.append("--" + boundary).append(LINE_FEED);
        writer.append(
                "Content-Disposition: form-data; name=\"" + fieldName
                        + "\"; filename=\"" + fileName + "\"")
                .append(LINE_FEED);
        writer.append(
                "Content-Type: "
                        + URLConnection.guessContentTypeFromName(fileName))
                .append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        uploadFile.writeTo(outputStream);
        outputStream.flush();

        writer.append(LINE_FEED);
        writer.flush();
    }

    /**
     * Adds a header field to the request.
     * @param name - name of the header field
     * @param value - value of the header field
     */
    public void addHeaderField(String name, String value) {
        writer.append(name + ": " + value).append(LINE_FEED);
        writer.flush();
    }

    /**
     * Completes the request and receives response from the server.
     * @return a list of Strings as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    public List<String> finish() throws IOException {
        List<String> response = new ArrayList<String>();

        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("Server returned non-OK status: " + status);
        }

        return response;
    }

//    Same as above but returns a String instead a ArrayList
    public HTTPAnswer finishwithString() throws IOException {
        String ans;
        HTTPAnswer httpresult;

        writer.append(LINE_FEED).flush();
        writer.append("--" + boundary + "--").append(LINE_FEED);
        writer.close();

        // checks server's status code first
        int status = httpConn.getResponseCode();
        switch (status){
            case HttpURLConnection.HTTP_OK:
                 ans = buildAnswer(httpConn.getInputStream());
                 httpresult = new HTTPAnswer(HttpURLConnection.HTTP_OK, ans);
                break;
            case HttpURLConnection.HTTP_BAD_REQUEST:
                ans = buildAnswer(httpConn.getErrorStream());
                httpresult = new HTTPAnswer(HttpURLConnection.HTTP_BAD_REQUEST, ans);
                break;

            case HttpURLConnection.HTTP_BAD_GATEWAY:
                ans = "Connection ERROR";
                httpresult = new HTTPAnswer(HttpURLConnection.HTTP_BAD_GATEWAY, ans);
                break;
            case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
                ans = "";
                httpresult = new HTTPAnswer(HttpURLConnection.HTTP_CLIENT_TIMEOUT, ans);
                break;
            default:
                ans = "Connection ERROR";
                httpresult = new HTTPAnswer(999, ans);
                break;
        }
        httpConn.disconnect();
        return httpresult;
    }

//  Writes the HTTP Answer into a String to send it back
    private String buildAnswer(InputStream stream){
        StringBuilder response = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    stream));
            String line = null;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
        }catch (IOException exception){
            Log.e(TAG, "IO Exception");
            exception.printStackTrace();
            response.append("Error in reading the HTTP Response");
        }
        return response.toString();
    }

}


// Class for the HTTP Answer to get the different Messages
final class HTTPAnswer{
    private final int connetioncode;
    private final String message;

    public HTTPAnswer(int connetioncode, String message){
        this.connetioncode = connetioncode;
        this.message = message;
    }

    public int getConnetioncode() {
        return connetioncode;
    }

    public String getMessage() {
        return message;
    }
}