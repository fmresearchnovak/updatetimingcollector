package edu.fandm.enovak.updatetimingcollector;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

import static edu.fandm.enovak.updatetimingcollector.Lib.PREF_FILE_NAME;

public class StatusActivity extends AppCompatActivity {

    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_status);

        ctx = this;


    }

    protected void onResume(){
        super.onResume();

        // Logging status
        boolean hasPerms = Lib.hasPermissions(ctx);
        if (hasPerms) {
            // Create the log file, write the IMEI number, write the header row
            Lib.LogBcastReceiverOnOff(ctx, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            setStatusText(true);

        } else {
            Lib.LogBcastReceiverOnOff(ctx, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            setStatusText(false);
        }


        // Last upload to server
        // collect server ts from sharedPrefs
        SharedPreferences sharedPref = ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        long ts = sharedPref.getLong(Lib.PREF_SERV_TS_KEY, -1);
        if(ts != -1) {
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
            Log.d(Main.TAG, "ts: " + ts);
            String dateString = formatter.format(new Date(ts));
            TextView tsTV = (TextView) findViewById(R.id.status_tv_server_ts_val);
            tsTV.setText(dateString);
            tsTV.setTextColor(Color.parseColor("#147e00"));
        }


        // Number of Bytes
        File logF = Lib.getLogFile(ctx);
        long bytes = logF.length();
        Log.d(Main.TAG, "name: "+logF.getAbsoluteFile());
        Log.d(Main.TAG, "bytes: "+bytes);
        if(bytes != 0L){
            TextView sizeTV = (TextView)findViewById(R.id.status_tv_bytes_contrib_val);
            sizeTV.setText(bytes + " Bytes of data");
            sizeTV.setTextColor(Color.parseColor("#147e00"));
        }



    }


    private void setStatusText(boolean isON){


        TextView statusTV = (TextView)findViewById(R.id.status_tv_status_val);
        if(isON){
            statusTV.setText("Active");
            statusTV.setTextColor(Color.parseColor("#147e00"));
        } else {
            statusTV.setText("Inactive");
            statusTV.setTextColor(Color.parseColor("#bbbbbb"));
        }
    }
}
