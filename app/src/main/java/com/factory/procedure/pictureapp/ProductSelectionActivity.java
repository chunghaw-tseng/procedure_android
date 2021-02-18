package com.factory.procedure.pictureapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * This class allows the user to select the product from a list taken from the L-One
 */


//TODO In the future -> Be able to add the and change the categories for the product
public class ProductSelectionActivity extends AppCompatActivity {

    private static final String TAG = "ProductSelectActivity";
    private ListView listView;

//    Products List
    private ArrayList<Object> listing = new ArrayList<>();

    private RelativeLayout title_layout;
    private TextView title_text, path_text;
    private ImageButton back_btn, home_btn, edit_name;

    private int MODE;
    private ListArrayAdapter adapter;


    //    Dialogs for loading the data
    private AlertDialog alertDialog;
    private View dialogView;
    private boolean edit_names = false;

    //    URL use
    private String editName = "/plugins/chn1802/editProductName";
    private String editCategoryName = "/plugins/chn1802/editCategory";
    private String fetchCategoryurl = "/plugins/chn1802/fetchCategoryData";
    private String fetchListProducts = "/plugins/chn1802/fetchListing";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_selection);

        MODE = getIntent().getIntExtra(Constants.Mode_Type, 0);
        title_layout = findViewById(R.id.product_selec_title);
        title_text = findViewById(R.id.titletext);
        path_text = findViewById(R.id.pathtext);

        edit_name = findViewById(R.id.editnamebtn);
        edit_name.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Set edit
//                Add color to the background
                if(edit_names) {
                    edit_names = false;
                    edit_name.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.electric));
                }else{
                    edit_names = true;
                    edit_name.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.poppy));
                }
            }
        });
        home_btn = findViewById(R.id.home_btn);
        home_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                path_text.setText("");
                fetchProductsFromDevice();
            }
        });

        setMode(MODE);

        back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        listView = findViewById(R.id.product_list);

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG, "Long Item Click");
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            boolean isProduct = false;

            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
//                Get the name and pass to the next activity
                if (edit_names){
                    Log.d(TAG, "EDIT NAME");
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(ProductSelectionActivity.this);
                    // Setting Dialog Title
                    alertDialog.setTitle(getText(R.string.edit_title));

                    // Setting Dialog Message
                    alertDialog.setMessage(getText(R.string.edit_title_msg));
                    final EditText input = new EditText(ProductSelectionActivity.this);
                    Object selectedobj = listing.get(position);
                    if (selectedobj instanceof Category){
                        input.setText(((Category) selectedobj).getName());
                        isProduct = false;
                    }else{
                        input.setText(((Product)selectedobj).getProductName());
                        isProduct = true;
                    }
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    input.setLayoutParams(lp);
                    alertDialog.setView(input);

                    // Setting Icon to Dialog
                    alertDialog.setIcon(R.drawable.edit_blck);

                    // Setting Positive "Yes" Button
                    alertDialog.setPositiveButton(getText(R.string.save_btn),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog,int which) {
                                    // Check the type of object
                                    if (isProduct){
                                        // Write your code here to execute after dialog
                                        Log.d(TAG, "Product EDIT");
                                        String url = Constants.HTTPHEADER + loadSettings() + editName;

                                        HashMap<String, String> metadata = new HashMap<>();
                                        Product temp = ((Product) listing.get(position)).updateProductName(input.getText().toString());
                                        Gson gson = new Gson();
                                        String jsondata = gson.toJson(temp);
                                        metadata.put("product", jsondata);
                                        new SendData(ProductSelectionActivity.this, url, metadata, new SendData.SendResponse() {
                                            @Override
                                            public void transferComplete() { }
                                            @Override
                                            public void transferWithData(JSONArray data) {
                                                Log.d(TAG, "Transfer Complete, setting Name");
                                                ((Product)listing.get(position)).setProductName(input.getText().toString());
                                                adapter.notifyDataSetChanged();
                                            }
                                            @Override
                                            public void onTransfer(int status){}
                                            @Override
                                            public void connectionLost(String errors) {
                                                Toast.makeText(getApplicationContext(),getText(R.string.connection_error), Toast.LENGTH_SHORT).show();
                                            }
                                        }).execute();
                                    }else{
                                        Log.d(TAG, "Category EDIT");
                                        // Write your code here to execute after dialog
                                        String url = Constants.HTTPHEADER + loadSettings() + editCategoryName;
                                        HashMap<String, String> metadata = new HashMap<>();
                                        Category temp = ((Category) listing.get(position)).updateCategoryName(input.getText().toString());
                                        Gson gson = new Gson();
                                        String jsondata = gson.toJson(temp);
                                        metadata.put("category", jsondata);
                                        new SendData(ProductSelectionActivity.this, url, metadata, new SendData.SendResponse() {
                                            @Override
                                            public void transferComplete() {}
                                            @Override
                                            public void onTransfer(int status){}
                                            @Override
                                            public void transferWithData(JSONArray data) {
                                                Log.d(TAG, "Transfer Complete, setting Name");
                                                ((Category)listing. get(position)).setCategoryName(input.getText().toString());
                                                adapter.notifyDataSetChanged();
                                            }
                                            @Override
                                            public void connectionLost(String errors) {
                                                Log.e(TAG , errors);
                                                Toast.makeText(getApplicationContext(),getText(R.string.connection_error), Toast.LENGTH_SHORT).show();
                                            }
                                        }).execute();
                                    }
                                }
                            });
                    alertDialog.setNegativeButton(getText(R.string.cancel_btn),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                    // Showing Alert Message
                    alertDialog.show();
                }else {
                    Log.d(TAG, "OPEN PRODUCT OR CATEGORY");
//                    check type
                    if(listing.get(position) instanceof Category){
//                        Folder
                        Category chosencat = (Category)listing.get(position);
                        fetchProductsFromCategory(chosencat);
                    }else{
//                        Product
                        Intent intent = new Intent(getApplicationContext(), OverviewLayout.class);
                        intent.putExtra(Constants.PRODUCT, (Product) listing.get(position));
                        intent.putExtra(Constants.Mode_Type, MODE);
                        startActivity(intent);
                    }
                }
            }
        });
    }

    //  This function loads the Settings from Shared Preferences
    private String loadSettings(){
        SharedPreferences pref = getSharedPreferences(Constants.SHAREDPREF, MODE_PRIVATE);
        String ipaddress = pref.getString(Constants.IP_ADD, Constants.IP_DEFAULT);
        return ipaddress;
    }


//    Show Dialog for when it fetches for data
    private void showAlertDialog(){
        //        Building the Alert Dialog
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        dialogView = inflater.inflate(R.layout.splash_dialog, null);
        dialogBuilder.setView(dialogView);
        dialogBuilder.setCancelable(false);
        alertDialog = dialogBuilder.create();
        alertDialog.setCanceledOnTouchOutside(false);
        Window window = alertDialog.getWindow();
        window.setLayout(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        window.setGravity(Gravity.CENTER);

        Button ok_btn = dialogView.findViewById(R.id.acceptbtn);
        TextView title = dialogView.findViewById(R.id.dialogtitle);
        title.setText(R.string.downloading_products);
        TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
        subtitle.setText(R.string.downloading_products_sub);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setVisibility(View.VISIBLE);
        ok_btn.setVisibility(View.GONE);
        alertDialog.show();
    }



//    Function that fetchs the products from a Category
    private void fetchProductsFromCategory(Category category){
        showAlertDialog();
        path_text.setText(category.getName());
        Log.d(TAG, "CATEGORY CHOSEN");
        String url = Constants.HTTPHEADER + loadSettings() + fetchCategoryurl;
        HashMap<String , String> data = new HashMap<>();
        data.put("catid", category.getId().toString());
        new SendData(ProductSelectionActivity.this, url, data, new SendData.SendResponse() {
            @Override
            public void transferComplete() {
//                No data received
                listing.clear();
                tasksFinished(true, "");
            }
            @Override
            public void onTransfer(int status){}
            @Override
            public void transferWithData(JSONArray data) {
                listing.clear();
                for (int k = 0; k < data.length(); k++) {
//                    Try to read the data, if error skip
                    try {
                        JSONArray productdata = data.getJSONArray(k);
                        Product product = new Product(productdata.getInt(0), productdata.getString(1), productdata.getLong(2), productdata.getLong(3), productdata.getString(4));
                        listing.add(product);
                        continue;
                    }catch (JSONException e){
                        Log.e(TAG, "JSON Error NOT A Product");
                    }
//                       Categories should be here
                    try{
                        JSONArray categorydata = data.getJSONArray(k);
                        Category category = new Category(categorydata.getString(0), categorydata.getInt(1));
                        listing.add(category);
                    }catch (JSONException e){
                        Log.e(TAG, "JSON Error NOT A Category");
                        Toast.makeText(getApplicationContext(), "ERROR LOG: JSON ERROR on parsing data received " , Toast.LENGTH_LONG).show();
                        tasksFinished(false, getApplicationContext().getString(R.string.error_loaddata));
                    }
                }
                adapter = new ListArrayAdapter(getApplicationContext(), listing);
                listView.setAdapter(adapter);
                tasksFinished(true, "");
            }

            @Override
            public void connectionLost(String errors) {
                path_text.setText("");
                tasksFinished(false, errors);
            }
        }).execute();
    }


//    Fetch the Products from the L-One
    private void fetchProductsFromDevice(){
        showAlertDialog();
        Log.d(TAG, "GETTING ALL THINGS");
//        String url = Constants.HTTPHEADER + loadSettings() + fetchProducturl;
        String url = Constants.HTTPHEADER + loadSettings() + fetchListProducts;
//        Requesting the Products
        new RequestData(ProductSelectionActivity.this, url, new RequestData.DataResponse() {
            @Override
            public void processData(JSONArray output) {
//                Processing data and unpackaging
                Log.d(TAG, "Data received");
                listing.clear();
                for (int k = 0; k < output.length(); k++) {
//                    Try to read the data, if error skip
                        try {
                            JSONArray productdata = output.getJSONArray(k);
                            Product product = new Product(productdata.getInt(0), productdata.getString(1), productdata.getLong(2), productdata.getLong(3), productdata.getString(4));
                            listing.add(product);
                            continue;
                        }catch (JSONException e){
                            Log.e(TAG, "JSON Error NOT A Product");
                        }
//                        Try String
                        try{
                            JSONArray categorydata = output.getJSONArray(k);
                            Category category = new Category(categorydata.getString(0), categorydata.getInt(1));
                            listing.add(category);
                        }catch (JSONException e){
                            Log.e(TAG, "JSON Error NOT A Category");
                            Toast.makeText(getApplicationContext(), "ERROR LOG: JSON ERROR on parsing data received " , Toast.LENGTH_LONG).show();
//                            Continue here
                            tasksFinished(false, getApplicationContext().getString(R.string.error_loaddata));
                        }
                    }
                    adapter = new ListArrayAdapter(getApplicationContext(), listing);
                    listView.setAdapter(adapter);
                    tasksFinished(true, "");
            }
            @Override
            public void connectionLost(String error) {
                tasksFinished(false, error);
            }
        }).execute();
    }

    //    Called after the async task finishes
    private void tasksFinished(Boolean success, String error){
        Button ok_btn = dialogView.findViewById(R.id.acceptbtn);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setVisibility(View.GONE);
        TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
        ok_btn.setVisibility(View.VISIBLE);
        if(success) {
            subtitle.setText(R.string.finish_download);
            alertDialog.dismiss();
        }else{
            subtitle.setText(error);
            ok_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                }
            });
        }
    }


//    Setting colors for the mode
    private void setMode(int mode){
        if(mode == Constants.EDIT_MODE){
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                title_layout.setBackgroundColor(getColor(Constants.EDIT_COLOR));
            }else{
                title_layout.setBackgroundColor(getResources().getColor(Constants.EDIT_COLOR));
            }
            title_text.setText(getResources().getText(R.string.edit_package));

        }else{
            if(android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                title_layout.setBackgroundColor(getColor(Constants.VIEW_COLOR));
            }else{
                title_layout.setBackgroundColor(getResources().getColor(Constants.VIEW_COLOR));
            }
            title_text.setText(getResources().getText(R.string.browse_package));
//            addfolder_btn.setVisibility(View.GONE);
            edit_name.setVisibility(View.GONE);
        }
    }

//      List Adapter for the product list received from the Resources received
    class ListArrayAdapter extends ArrayAdapter<Object>{
        private Context context;
        private ArrayList<Object> listing;

        ListArrayAdapter(Context context, ArrayList<Object> listing){
            super(context, -1, listing);
            this.context = context;
            this.listing = listing;
        }

//        Function that gets called when the views are shown on the list view
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            final LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//            Check the type
            if (listing.get(position) instanceof Category) {
                try {
                    Category folder = (Category) listing.get(position);
                    View rowView = inflater.inflate(R.layout.folder_row, parent, false);
                    ViewGroup.LayoutParams params = rowView.getLayoutParams();
                    params.height = 150;
                    TextView textView = rowView.findViewById(R.id.product_title);
                    textView.setText(folder.getName());
                    rowView.setLayoutParams(params);
                    return rowView;
                }catch (Exception e){
                    Log.e(TAG, "Error in Category");
                }
            }else {
                try {
                    Product product = (Product) listing.get(position);
                    View rowView = inflater.inflate(R.layout.product_row, parent, false);
                    ViewGroup.LayoutParams params = rowView.getLayoutParams();
                    params.height = 150;
                    TextView textView = rowView.findViewById(R.id.product_title);
                    textView.setText(product.getProductName());
                    rowView.setLayoutParams(params);
                    return rowView;
                } catch (Exception e) {
                    Log.e(TAG, "Error in Product");
                }
            }
            return null;
        }
    }

    @Override
    public void onBackPressed(){
        if (path_text.getText().toString().equals("")){
            super.onBackPressed();
            finish();
        }else{
            path_text.setText("");
            fetchProductsFromDevice();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
//        Get the products from the L-One
//        Reset the text
        path_text.setText("");
        fetchProductsFromDevice();
    }
}
