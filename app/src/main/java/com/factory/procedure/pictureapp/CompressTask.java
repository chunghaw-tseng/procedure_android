package com.factory.procedure.pictureapp;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

public class CompressTask extends AsyncTask<Void, Void, Void> {

    private String TAG = "CompressTask";

    private Context context;
    private Uri videouri;
    private File realFileName;
    private boolean success = false;

    //    Interface for the actions after the async task is finished
    private CompressTask.TaskUpdate delegate = null;

    public interface TaskUpdate{
        void onFinish(File compressed_result);
        void onError();
    }



    public CompressTask(Context context, Uri videouri, TaskUpdate update){
        this.delegate = update;
        this.context = context;
        this.videouri = videouri;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }


    @Override
    protected Void doInBackground(Void... voids) {
        try {

            InputStream in = context.getContentResolver().openInputStream(videouri);
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(new Date());
            String tempFileName = "TMP_" + timeStamp + "_";
            File storageDir = new File(context.getExternalFilesDir(null), Constants.DATA_FOLDER);
            realFileName = new File(storageDir, "MP4_" + timeStamp + "_" + String.valueOf(nDigitRandomNo(9))+ ".mp4");
            File video = File.createTempFile(
                    tempFileName,  /* prefix */
                    ".mp4",         /* suffix */
                    storageDir      /* directory */
            );
            OutputStream out = new FileOutputStream(video);
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();

            Log.d(TAG, "TMP Video path " + video.getAbsolutePath());
            String resultpath = SiliCompressor.with(context).compressVideo(video.getAbsolutePath(), storageDir.getAbsolutePath());

            Log.d(TAG, "Compressed Video path " + resultpath);
            File compressed = new File(resultpath);

//          Rename
            if (compressed.renameTo(realFileName)){
                Log.d(TAG, "Rename Video path " + realFileName.getAbsolutePath());
//            Delete tmp file
                video.delete();
                success = true;
            }

        }catch (URISyntaxException urie) {
            urie.printStackTrace();

        }catch (NullPointerException ne) {
            ne.printStackTrace();

        }catch (Exception e) {
            e.printStackTrace();
        }



        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        Log.d(TAG, "Finsihed");
        if (success) {
            delegate.onFinish(realFileName);
        }else{
            delegate.onError();
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    private int nDigitRandomNo(int digits){
        int max = (int) Math.pow(10,(digits)) - 1; //for digits =7, max will be 9999999
        int min = (int) Math.pow(10, digits-1); //for digits = 7, min will be 1000000
        int range = max-min; //This is 8999999
        Random r = new Random();
        int x = r.nextInt(range);// This will generate random integers in range 0 - 8999999
        int nDigitRandomNo = x+min; //Our random rumber will be any random number x + min
        return nDigitRandomNo;
    }

}
