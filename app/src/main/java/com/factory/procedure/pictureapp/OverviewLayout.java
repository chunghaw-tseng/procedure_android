package com.factory.procedure.pictureapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.factory.procedure.pictureapp.touch_helpers.ItemTouchHelperAdapter;
import com.factory.procedure.pictureapp.touch_helpers.ItemTouchHelperCallback;
import com.factory.procedure.pictureapp.touch_helpers.ItemTouchHelperViewHolder;
import com.factory.procedure.pictureapp.touch_helpers.OnStartDragListener;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.factory.procedure.pictureapp.Constants.METADATA;


/**
 * This class shows the overview of the packaging procedure
 */
public class OverviewLayout extends AppCompatActivity implements OnStartDragListener {

//    Dialogs for loading the data
    private AlertDialog alertDialog;
    private View dialogView;

    public static final int PROGRESS_BAR = 0;
    public static final int LOADER = 1;

    private ImageButton addProcedurebtn, saveLayoutButton, back_btn;
    private final static String TAG = "OverviewLayout";

//    RecyclerView Use
    private RecyclerView gridView;
    private RecyclerView.LayoutManager mLayoutManager;
    private RecycleGridAdapter mAdapter;

//  This list is needed
    private ArrayList<Procedure> procedureArrayList = new ArrayList<>();

    //     Variables used for the OverviewLayout for the Activity on Result
    final static int EDIT_PROCEDURE = 0;
    final static int NEW_PROCEDURE = 1;

    private String product_name;
    private RelativeLayout title_layout;
    private TextView title_txt;
    private int Mode_type;
    private Product selectedProduct;

//    URLs for POST
    private File productFolder;
    private String getProducturl = "/plugins/chn1802/getProductData";
    private String reorderProcedure = "/plugins/chn1802/reorderProcedure";

    private ItemTouchHelper mItemTouchHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.procedure_overview);

        title_txt = findViewById(R.id.overview_title);
        title_layout = findViewById(R.id.titlelayout);

//        Add New Procedure
        addProcedurebtn = findViewById(R.id.add_procedure);
        addProcedurebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Intent moving to the Procedure Activity
                Intent intent = new Intent(getApplicationContext(), ProcedureActivity.class);
                Bundle bundle = new Bundle();
                Procedure emptyProcedure = new Procedure("", "", product_name);
                bundle.putSerializable(Constants.PROCEDURE, emptyProcedure);
                bundle.putSerializable(Constants.PRODUCT, selectedProduct);
                intent.putExtra(Constants.PROCEDURE, bundle);
                intent.putExtra(Constants.Mode_Type, Mode_type);
                startActivityForResult(intent, NEW_PROCEDURE);
            }
        });

//        Save Layout
        saveLayoutButton = findViewById(R.id.save_layout);
        saveLayoutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Changing background color
                saveLayoutButton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.electric));
//                Save in metadata
                Log.d(TAG, "Saving layout");
                String jsondata = updateMetadata();
//                Send the metadata
//      Inform the Server of change
                if (jsondata != null) {
                    String url = Constants.HTTPHEADER + loadSettings() + reorderProcedure;
                    HashMap<String, String> metadata = new HashMap<>();
                    metadata.put("metadata", jsondata);
                    showLoader(R.string.procedure_send, R.string.procedure_change, LOADER);
                    new SendData(OverviewLayout.this, url, metadata, new SendData.SendResponse() {
                        @Override
                        public void transferComplete() {
                            tasksFinished(true, getApplicationContext().getString(R.string.reorder_success));
                        }
                        @Override
                        public void transferWithData(JSONArray data) {}
                        @Override
                        public void onTransfer(int status){}
                        @Override
                        public void connectionLost(String errors) {
//                            Log.e(TAG , errors);
                            tasksFinished(false, errors);
                        }
                    }).execute();
//                    Error on writing to file
                }else{
                    Toast.makeText(getApplicationContext(), "DEBUG : ERROR ON WRITING FILE", Toast.LENGTH_LONG ).show();
                }
            }
        });

        back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

//        Getting the of the products with the view type
        if(getIntent().getSerializableExtra(Constants.PRODUCT) != null){
            selectedProduct = (Product) getIntent().getSerializableExtra(Constants.PRODUCT);
            product_name = selectedProduct.getProductName();
            title_txt.setText(product_name);
            Mode_type = getIntent().getIntExtra(Constants.Mode_Type, 0);
            setMode(Mode_type);
        }else{
//            If not product then shouldn't be here
            Log.e(TAG, "No product Received");
            finish();
        }

//        Use for recycler View
        gridView = findViewById(R.id.procedure_list);
        mLayoutManager = new GridLayoutManager(this, 2);
//        mLayoutManager = new LinearLayoutManager(this);
        gridView.setLayoutManager(mLayoutManager);
        mAdapter = new RecycleGridAdapter(this, procedureArrayList, this);
        gridView.setAdapter(mAdapter);
//        Adds smoothing to the grid view
        gridView.setItemViewCacheSize(20);
        gridView.setDrawingCacheEnabled(true);
        gridView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

//        Creating the touches for the recylcle view
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mAdapter);
        mItemTouchHelper  = new ItemTouchHelper(callback);
        mItemTouchHelper .attachToRecyclerView(gridView);

        productFolder = new File(getExternalFilesDir(null), Constants.DATA_FOLDER);

        Log.d(TAG, "Deleting DATA_FOLDER");
        deleteFolderContents(new File(getExternalFilesDir(null), Constants.DATA_FOLDER));

        showLoader(R.string.downloading, R.string.downloading_subtitle, PROGRESS_BAR);

//        Getting data from the server
        String url = Constants.HTTPHEADER + loadSettings() + getProducturl;
//        Downloading the data for the product
        new DownloadData(OverviewLayout.this, url, selectedProduct, new DownloadData.DownloadResponse() {
            @Override
            public void donwloadComplete() {
//        This needs to run after the Download Data
                if(loadDataSaved()){
                    //      And update the index
                    Log.d(TAG, "Data Loaded");
                    updateViews();
                    alertDialog.dismiss();
                }else{
//                    Toast.makeText(getApplicationContext(), "DEBUG: ERROR JSON EXCEPTION", Toast.LENGTH_LONG).show();
                    tasksFinished(false, getApplicationContext().getString(R.string.error_loaddata));
                }
            }

            @Override
            public void onDownloadUpdate(Integer[] state){
                int msg = 0;
                switch (state[0]){
                    case DownloadData.DOWNLOAD_STATE:
                        msg = R.string.downloading_subtitle;
                        break;
                    case DownloadData.SAVEFILE_STATE:
                        msg = R.string.creating_file;
                        break;
                    case DownloadData.UNZIP_STATE:
                        msg = R.string.unzipping_file;
                        break;
                    case DownloadData.UNZIP_PHOTO:
                        msg = R.string.unzipping_img;
                        break;
                    case DownloadData.UNZIP_METADATA:
                        msg = R.string.unzipping_meta;
                        break;
                    case DownloadData.UNZIP_VIDEOS:
                        msg = R.string.unzipping_videos;
                        break;
                    case DownloadData.CLEAN_STATE:
                        msg = R.string.clean_up;
                        break;
                }
                if(msg != 0) {
                    updateDialog(msg, state[1]);
                }
            }
            @Override
            public void connectionLost(String error) {
                Log.e(TAG, "Connection was lost");
                tasksFinished(false, error);
            }
        }).execute();
    }

    //  This function loads the Settings from Shared Preferences
    private String loadSettings(){
        SharedPreferences pref = getSharedPreferences(Constants.SHAREDPREF, MODE_PRIVATE);
        return pref.getString(Constants.IP_ADD, Constants.IP_DEFAULT);
    }

    private void showLoader(int titletxt, int subtitletext, int type){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogView = inflater.inflate(R.layout.loader_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);
        alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        Window window = alertDialog.getWindow();
        window.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);

        TextView title = dialogView.findViewById(R.id.dialogtitle);
        title.setText(titletxt);
        LinearLayout pb_layout = dialogView.findViewById(R.id.progressFrame);
        LinearLayout loader_layout = dialogView.findViewById(R.id.loadingFrame);
        if(type == PROGRESS_BAR) {
            loader_layout.setVisibility(View.GONE);
            pb_layout.setVisibility(View.VISIBLE);
            ProgressBar pg = dialogView.findViewById(R.id.progressbar);
            pg.setVisibility(View.VISIBLE);
            TextView subtitle = dialogView.findViewById(R.id.progresstext);
            subtitle.setText(subtitletext);
            TextView pg_per = dialogView.findViewById(R.id.loader_percent);
            pg_per.setText("0%");
        }else {
            loader_layout.setVisibility(View.VISIBLE);
            pb_layout.setVisibility(View.GONE);
            TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
            subtitle.setText(subtitletext);
            ProgressBar pg = dialogView.findViewById(R.id.loader);
            pg.setVisibility(View.VISIBLE);
        }
        alertDialog.show();
    }


    private void updateDialog(int subtitletext, int value){
        TextView subtitle = dialogView.findViewById(R.id.progresstext);
        subtitle.setText(subtitletext);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setProgress(value);
        TextView pg_val = dialogView.findViewById(R.id.loader_percent);
        pg_val.setText(String.format(Locale.getDefault(), "%d %%", value));
    }

    //    Called after the async task finishes
    private void tasksFinished(Boolean success, String text){

        LinearLayout pb_layout = dialogView.findViewById(R.id.progressFrame);
        LinearLayout loader_layout = dialogView.findViewById(R.id.loadingFrame);
        pb_layout.setVisibility(View.GONE);
        loader_layout.setVisibility(View.VISIBLE);
        Button ok_btn = dialogView.findViewById(R.id.acceptbtn);
        ProgressBar loader = dialogView.findViewById(R.id.loader);
        loader.setVisibility(View.GONE);
        TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
        ok_btn.setVisibility(View.VISIBLE);
        if(success) {
            subtitle.setText(text);
            ok_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                }
            });
        }else{
            subtitle.setText(text);
            ok_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                    finish();
                }
            });
        }
    }

    //    Updates the Items on the RecyclerView
    private void updateViews(){
//        For the recycler view
        mAdapter.notifyItemRangeInserted(0, procedureArrayList.size());
        mAdapter.notifyDataSetChanged();
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

//  Loading data from internal memory
    private boolean loadDataSaved(){
//        Resetting the arrays in case the data is loaded twice
        procedureArrayList.clear();
//        Searching for the product folder
        File datasaved = new File(getExternalFilesDir(null), Constants.DATA_FOLDER);
        File[] list = datasaved.listFiles();
//        Parsing files from the folder
        if(list != null && list.length > 0) {
            for (File f : list) {
//            Is a data folder
                String extension = f.getName().substring(f.getName().lastIndexOf("."));
//                            Check the metadata
                if(extension.equals(".json")){
                    String decodeFile =  decodeJSONFile(f);
                    try {
                        JSONObject metadatajson = new JSONObject(decodeFile);
                        Log.d(TAG, "Decoded : " + metadatajson.toString());
//                        Update the products data
                        if ( metadatajson.get("procedure") != (JSONObject.NULL)) {
                            Gson gson = new Gson();
                            Type listType = new TypeToken<List<Procedure>>() {}.getType();
                            List<Procedure> procedureList = gson.fromJson(metadatajson.getString("procedure"), listType);

//                            Checking removing duplicates
                            List<Procedure> parsedList = removeDuplicates(procedureList);
                            procedureArrayList.addAll(parsedList);
                            selectedProduct.setProcedureList(procedureArrayList);
                        }
                    }catch (JSONException jsone){
                        Log.e(TAG, "JSON Error");
                        jsone.printStackTrace();
                        return false;
                    }
                }
            }
        }
//        For the procedure
//        Setting the correct ID Number
        Integer id =0;
        for (int i=0 ; i < procedureArrayList.size(); i++){
            if (procedureArrayList.get(i).getObjectId() > id){
                id = procedureArrayList.get(i).getObjectId();
            }
        }
        Log.d(TAG, "The id set is " + (id + 1));
        Procedure.setIdNo(id + 1 );
        return true;
    }

//  Function that checks duplicates from the saved list (In case)
    private List<Procedure> removeDuplicates(List<Procedure> procedureList){
        Log.d(TAG, "Duplicates check");
//        This hash map contains all the non duplicates
        HashMap<Integer,Procedure> noduplicates = new HashMap<>();
        List<Procedure> resultList = new ArrayList<>();

//        Get the non duplicates -> Only take the first found
        for (int i=0; i < procedureList.size(); i++){
            Procedure current = procedureList.get(i);
            if (! noduplicates.containsKey(current.getObjectId())){
                noduplicates.put(current.getObjectId(), current);
                resultList.add(current);
            }
        }
//        Check that the images exist
        return imageChecker(resultList);
    }


//    Checks that the images are there if not delete procedure
    private List<Procedure> imageChecker(List<Procedure> list){
        List<Procedure> result = new ArrayList<>();
//        Getting the file list
        File datasaved = new File(getExternalFilesDir(null), Constants.DATA_FOLDER);
        File[] filelist = datasaved.listFiles();

        for (Procedure p : list){
            File currentImage = new File(datasaved, p.getImage());
            if (!Arrays.asList(filelist).contains(currentImage)){
                Log.d(TAG, "ERROR image is not existant");
            }else{
                result.add(p);
            }
        }
        return result;
    }

    //    Updates the metadata.json file and writes to it
    private String updateMetadata(){
//        Update time
        Long tsLong = System.currentTimeMillis();
        selectedProduct.setModifiedTime(tsLong);
        Log.d(TAG, "Setting time " + tsLong);
        selectedProduct.setProcedureList(procedureArrayList);
        File filepath = new File(getExternalFilesDir(null), Constants.DATA_FOLDER);
        File metadata = new File(filepath, METADATA);
//        Delete the metadatafile
//        Update json file
        Gson gson = new Gson();
        String jsondata = gson.toJson(selectedProduct);
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

//    Decodes the json File
    private String decodeJSONFile(File data){
        try {
            FileInputStream fileInputStream = new FileInputStream(data);
            BufferedReader bf = new BufferedReader(new InputStreamReader(fileInputStream));
            StringBuilder sb = new StringBuilder();
            String filedata = null;
            while ((filedata = bf.readLine()) != null) {
                sb.append(filedata).append("\n");
            }
            bf.close();
            return sb.toString();
        }catch (FileNotFoundException fe){
            Log.e(TAG, "File not existant");
            fe.printStackTrace();
        }catch (IOException ioe){
            Log.e(TAG, "IO Exception");
            ioe.printStackTrace();
        }
        return "";
    }

    //    Setting colors and the buttons for the mode
    private void setMode(int mode){
        if(mode == Constants.EDIT_MODE){
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                title_layout.setBackgroundColor(getColor(Constants.EDIT_COLOR));
            }else{
                title_layout.setBackgroundColor(getResources().getColor(Constants.EDIT_COLOR));
            }
            addProcedurebtn.setVisibility(View.VISIBLE);
            saveLayoutButton.setVisibility(View.VISIBLE);
        }else{
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                title_layout.setBackgroundColor(getColor(Constants.VIEW_COLOR));
            }else{
                title_layout.setBackgroundColor(getResources().getColor(Constants.VIEW_COLOR));
            }
            addProcedurebtn.setVisibility(View.INVISIBLE);
            saveLayoutButton.setVisibility(View.INVISIBLE);
        }
    }

//    Adapter for RecycleView
//    Implementing drag and drop
    class RecycleGridAdapter extends RecyclerView.Adapter<RecycleGridAdapter.ViewHolder> implements ItemTouchHelperAdapter {

            private List<Procedure> procedureList;
            private Context context;
            private final OnStartDragListener mDragStartListener;
            private static final int TYPE_ITEM = 0;

            final Handler handler = new Handler();

            // Provide a suitable constructor (depends on the kind of dataset)
            public RecycleGridAdapter(Context ctx, List<Procedure> procedureList, OnStartDragListener dragStartListener) {
                this.procedureList = procedureList;
                this.context = ctx;
                this.mDragStartListener = dragStartListener;
            }

            // Create new views (invoked by the layout manager)
            @Override
            public RecycleGridAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                           int viewType) {
                // create a new view
                if(viewType == TYPE_ITEM) {
                    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagecell_layout, null);
//                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.imagerow_layout, null);
                    view.setOnClickListener(clickListener);
                    return new ViewHolder(view);
                }
                throw new RuntimeException("there is no type that matches the type " + viewType + " + make sure your using types correctly");
            }

//          Replace contents of a view
            @Override
            public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
                Procedure p = procedureList.get(position);
                holder.title_txt.setText(p.getProcedure_title());
//                holder.index_txt.setText(String.valueOf(p.getIndex()));
                holder.explanation_txt.setText(p.getProcedure_details());
                if (!p.getImage().equals("")) {
                    File imagepath = new File(productFolder, p.getImage());
                    holder.imageView.setImageBitmap(scalePicture(imagepath.getAbsolutePath()));
                }else{
                    holder.imageView.setImageResource(0);
                }
            }

    // Replace the contents of a view (invoked by the layout manager)
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void onBindViewHolder(final ViewHolder holder, int position, List<Object> payloads) {
                // - get element from your dataset at this position
                // - replace the contents of the view with that element
                if (payloads.isEmpty()) {
                    onBindViewHolder(holder, position);
                }else{
                    Bundle o = (Bundle) payloads.get(0);
                    for (String key : o.keySet()){
                        if (key.equals(Procedure.IMAGE_KEY)){
                            if (o.get(Procedure.IMAGE_KEY) != null) {
                                File imagepath = new File(productFolder, (String) o.get(Procedure.IMAGE_KEY));
                                holder.imageView.setImageBitmap(scalePicture(imagepath.getAbsolutePath()));
                            }
                        }
                        if(key.equals(Procedure.NAME_KEY)){
                            holder.title_txt.setText((String) o.get(Procedure.NAME_KEY));
                        }
                        if(key.equals(Procedure.CONTENTS_KEY)){
                            holder.explanation_txt.setText((String) o.get(Procedure.CONTENTS_KEY));
                        }
                    }
                }

                //                Set the drag button
                Picasso.get().load(R.drawable.move).resize(75,75).centerCrop().into(holder.drag_btn);

//                Listener for the drag and drop event
                holder.drag_btn.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {

                        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
//                            Start the handler here
                            Log.d(TAG, "START");
                            mDragStartListener.onStartDrag(holder);
                        }
                        if(event.getActionMasked() == MotionEvent.ACTION_UP){
                            v.performClick();
                        }
                        return false;
                    }
                });

            }
            @Override
            public int getItemViewType(int position) {
                return TYPE_ITEM;
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public int getItemCount() {
                return procedureList.size();
            }

//            Replaces the Adapter data for new data
            public void onNewData(List<Procedure> newData){
                DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new ProcedureListDiffCallback(procedureList, newData));
                diffResult.dispatchUpdatesTo(this);
                this.procedureList.clear();
                this.procedureList.addAll(newData);
            }

//            Scales the Picture in the RecyclerView Items
            Bitmap scalePicture(String picpath){
                // Get the dimensions of the View
                // Dimensions need to be fixed
                int targetW = 300;
                int targetH = 300;
                // Get the dimensions of the bitmap
                BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                bmOptions.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(picpath, bmOptions);
                int photoW = bmOptions.outWidth;
                int photoH = bmOptions.outHeight;

                // Determine how much to scale down the image
                int scaleFactor = Math.min(photoW/targetW, photoH/targetH);

                // Decode the image file into a Bitmap sized to fill the View
                bmOptions.inJustDecodeBounds = false;
                bmOptions.inSampleSize = scaleFactor;
                bmOptions.inPreferredConfig = Bitmap.Config.RGB_565;
//                TODO Check this
                bmOptions.inPurgeable = true;
                Bitmap bitmap = BitmapFactory.decodeFile(picpath, bmOptions);
                return bitmap;
            }

//            ClickListener for the RecyclerView Items
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                Log.d(TAG, "Clicked on item");
//                Intent moving to the Procedure Activity
                int itemPosition = gridView.getChildAdapterPosition(v);
                Intent intent = new Intent(getApplicationContext(), ProcedureActivity.class);
                Bundle bundle = new Bundle();
                bundle.putSerializable(Constants.PROCEDURE, procedureArrayList.get(itemPosition));
                bundle.putSerializable(Constants.PRODUCT, selectedProduct);
                intent.putExtra(Constants.Mode_Type, Mode_type);
                intent.putExtra(Constants.PROCEDURE, bundle);
//                intent.putExtra(Constants.FILE_PATH, filepath);
                startActivityForResult(intent, EDIT_PROCEDURE);
                }
            };


//          Functions for the drag and drop
//            This function is not used but needs to be here
            @Override
            public void onItemDismiss(int position) {}

//            Called on the item move
            @Override
            public boolean onItemMove(int fromPosition, int toPosition) {
                Log.d(TAG, "Moving Item");
                if (fromPosition < toPosition) {
                    for (int i = fromPosition; i < toPosition; i++) {
                        Collections.swap(procedureList, i, i + 1);
                    }
                } else {
                    for (int i = fromPosition; i > toPosition; i--) {
                        Collections.swap(procedureList, i, i - 1);
                    }
                }
//                          Changing Color (Force the user to save)
                saveLayoutButton.setBackgroundColor(ContextCompat.getColor(context, R.color.poppy));
                notifyItemMoved(fromPosition, toPosition);
                return true;
            }


        // Provide a reference to the views for each data item
        // Complex data items may need more than one view per item, and
        // you provide access to all the views for a data item in a view holder
       class ViewHolder extends RecyclerView.ViewHolder implements ItemTouchHelperViewHolder {
            // each data item is just a string in this case
            TextView title_txt,explanation_txt;
            ImageView imageView;
            ImageButton drag_btn;

            ViewHolder(View itemView) {
                super(itemView);
                title_txt = itemView.findViewById(R.id.subtitle);
//                index_txt = itemView.findViewById(R.id.index_label);
                explanation_txt = itemView.findViewById(R.id.explanation);
                imageView = itemView.findViewById(R.id.image_procedure);
                drag_btn = itemView.findViewById(R.id.drag_btn);
                if(Mode_type == Constants.EDIT_MODE){
                    drag_btn.setVisibility(View.VISIBLE);
                }else{
                    drag_btn.setVisibility(View.INVISIBLE);
                }
            }

//            Functions for when the element is selected for drag and drop
            @Override
            public void onItemSelected() {
                CardView cardView = (CardView) itemView;
                cardView.setCardBackgroundColor(Color.LTGRAY);
            }

            @Override
            public void onItemClear() {
                CardView cardView = (CardView) itemView;
                cardView.setCardBackgroundColor(0);
            }
        }

}

//  Calling Interface to start dragging
    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }

    //    Callback for the DiffUtil used to check the differences in the RecyclerView
    public class ProcedureListDiffCallback extends DiffUtil.Callback {
        private List<Procedure> mOldList;
        private List<Procedure> mNewList;

        public ProcedureListDiffCallback(List<Procedure> oldList, List<Procedure> newList) {
            this.mOldList = oldList;
            this.mNewList = newList;
        }

//        Comparing sizes
        @Override
        public int getOldListSize() {
            return mOldList != null ? mOldList.size() : 0;
        }

        @Override
        public int getNewListSize() {
            return mNewList != null ? mNewList.size() : 0;
        }

//        Comparing if items are the same
        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return mNewList.get(newItemPosition).getObjectId() == mOldList.get(oldItemPosition).getObjectId();
        }

//        Comparing if contents are the same
        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            final Procedure oldP = mOldList.get(oldItemPosition);
            final Procedure newP = mNewList.get(newItemPosition);
//            Checking Image
            if ( ! newP.getImage().equals(oldP.getImage())) {
                return false;
            }
//            Checking title
            if ( ! newP.getProcedure_title().equals(oldP.getProcedure_title())) {
                return false;
            }
//            Checking Explanation
            if (! newP.getProcedure_details().equals(oldP.getProcedure_details())) {
                return false;
            }
            Log.d(TAG, "Checking contents the same is true");
            return true;
        }

        //        This function is called if the areItemsTheSame is true and areContentsTheSame is false
//        This compares and returns the differences on the items
        @Nullable
        @Override
        public Object getChangePayload(int oldItemPosition, int newItemPosition) {
            Procedure newProcedure = mNewList.get(newItemPosition);
            Procedure oldProcedure = mOldList.get(oldItemPosition);
            Bundle diffBundle = new Bundle();
//            Checking Image
            if ( ! newProcedure.getImage().equals(oldProcedure.getImage())) {
                diffBundle.putString(Procedure.IMAGE_KEY, newProcedure.getImage());
            }
//            Checking title
            if ( ! newProcedure.getProcedure_title().equals(oldProcedure.getProcedure_title())) {
                diffBundle.putString(Procedure.NAME_KEY, newProcedure.getProcedure_title());
            }
//            Checking Explanation
            if (! newProcedure.getProcedure_details().equals(oldProcedure.getProcedure_details())) {
                diffBundle.putString(Procedure.CONTENTS_KEY, newProcedure.getProcedure_details());
            }
            if (diffBundle.size() == 0) return null;
            return diffBundle;
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    //    Received on edit or view the Procedure
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
//        If it was a new procedure
        if (resultCode == RESULT_OK) {
            if (requestCode == NEW_PROCEDURE || requestCode == EDIT_PROCEDURE) {
                // Make sure the request was successful
                Log.d(TAG, "Procedure saved");
                Bundle bundleddata = data.getBundleExtra(Constants.PROCEDURE_LIST);
                List<Procedure> procedureList = (List<Procedure>) bundleddata.getSerializable(Constants.PROCEDURE_LIST);
//                Here needs to add the value procedure added to the array
//                Here it also updates the other arrays
                mAdapter.onNewData(procedureList);
                selectedProduct.setProcedureList((ArrayList<Procedure>) procedureList);
            }
        }
    }

}
