package com.factory.procedure.pictureapp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;


/**
 * Settings class to set the IP Address to connect
 */

public class SettingsActivity extends AppCompatActivity {

    private EditText ip_edit;
    private Button save_ip;
    private ImageButton back_btn;

    private InputFilter[] filters = new InputFilter[1];

    //    Dialogs for loading the data
    private AlertDialog alertDialog;
    private View dialogView;

    //    URL use
    private String checkDevice = "/plugins/chn1802/checkDevice";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_layout);

//        Adding IP Address Filters
        addFilters();
        ip_edit = findViewById(R.id.ip_edit);
        ip_edit.setFilters(filters);
        back_btn = findViewById(R.id.back_btn);
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        save_ip = findViewById(R.id.save_settings);
        save_ip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String new_ip = ip_edit.getText().toString();
                checkIPDevice(new_ip);
            }
        });
//        Loading settings from the Shared Prefs
        loadSettings();
    }

    /**
     * Adding filters for the IP address in the edit text so only IP addresses are allowed
     */
    private void addFilters(){
        filters[0] = new InputFilter() {
            @Override
            public CharSequence filter(CharSequence source, int start, int end,
                                       android.text.Spanned dest, int dstart, int dend) {
                if (end > start) {
                    String destTxt = dest.toString();
                    String resultingTxt = destTxt.substring(0, dstart)
                            + source.subSequence(start, end)
                            + destTxt.substring(dend);
                    if (!resultingTxt
                            .matches("^\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3}(\\.(\\d{1,3})?)?)?)?)?)?")) {
                        return "";
                    } else {
                        String[] splits = resultingTxt.split("\\.");
                        for (int i = 0; i < splits.length; i++) {
                            if (Integer.valueOf(splits[i]) > 255) {
                                return "";
                            }
                        }
                    }
                }
                return null;
            }

        };
    }

//  This function loads the Settings from Shared Preferences
    private void loadSettings(){
        SharedPreferences pref = getSharedPreferences(Constants.SHAREDPREF, MODE_PRIVATE);
        String savedprefs = pref.getString(Constants.IP_ADD, Constants.IP_DEFAULT);
        ip_edit.setText(savedprefs);
    }


//    This function saves the Settings from the Shared Preferences
    private void saveSettings(String new_ip){
        SharedPreferences pref = getSharedPreferences(Constants.SHAREDPREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(Constants.IP_ADD, new_ip);
        editor.commit();

        Toast.makeText(SettingsActivity.this, getText(R.string.settings_saved), Toast.LENGTH_SHORT).show();

    }


    //    async task that checks server
    private void checkIPDevice(final String new_ip){
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
        title.setText(R.string.check_connection_title);
        TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
        subtitle.setText(R.string.wait_msg);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setVisibility(View.VISIBLE);
        ok_btn.setVisibility(View.GONE);
        alertDialog.show();

//        Downloading the data for the product
        String url = Constants.HTTPHEADER + new_ip + checkDevice;

        new RequestData(SettingsActivity.this, url, new RequestData.DataResponse() {
            @Override
            public void processData(JSONArray output) {
                tasksFinished(true, new_ip);
            }

            @Override
            public void connectionLost(String error) {

                tasksFinished(false, new_ip);
            }
        }).execute();
    }

    //    Called after the async task finishes
    private void tasksFinished(Boolean success, final String ip){

        Button ok_btn = dialogView.findViewById(R.id.acceptbtn);
        ProgressBar pg = dialogView.findViewById(R.id.progressbar);
        pg.setVisibility(View.GONE);
        TextView subtitle = dialogView.findViewById(R.id.customdialogtext);
        ok_btn.setVisibility(View.VISIBLE);
        if(success) {
            subtitle.setText(R.string.device_found);
            ok_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    saveSettings(ip);
                    alertDialog.dismiss();
                }
            });
        }else{
            subtitle.setText(R.string.device_not_found);
            ok_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                }
            });
        }
    }

}
