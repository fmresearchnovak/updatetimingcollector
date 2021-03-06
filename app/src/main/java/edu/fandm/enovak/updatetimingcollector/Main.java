package edu.fandm.enovak.updatetimingcollector;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import static edu.fandm.enovak.updatetimingcollector.Lib.PREF_FILE_NAME;


public class Main extends AppCompatActivity {
    public static final String TAG = "enovak.TAG";

    private TextView tv;
    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = getApplicationContext();
        tv = findViewById(R.id.main_tv_explain);

        boolean hasPerms = Lib.hasPermissions(ctx);
        if(!hasPerms) {
            // I  request all (even those I already have)
            // The system ignores requests for permissions that
            // the app already has!
            ActivityCompat.requestPermissions(this, Lib.permissions, 0);

        } else {
            File logFile = Lib.getLogFile(ctx); // To initialize log file
            //Log.d(TAG, logFile.getAbsolutePath());
            Lib.LoggingOnOff(ctx, true);
        }


    }


    @Override
    public void onResume() {
        super.onResume();


        if(!Lib.hasPermissions(ctx)){
            tv.setText("Please accept all permissions");
            tv.setTextColor(Color.parseColor("#800000"));
        } else {

            // Upload on first run
            SharedPreferences sharedPref = ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            long ts = sharedPref.getLong(Lib.PREF_SERV_TS_KEY, -1);
            if(ts == -1) {
                FilePOSTer.scheduleUpload(ctx, false, 500); // 500ms
            }


            tv.setText("That's it!  You're done!");
            tv.setTextColor(Color.parseColor("#111111"));
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu m){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, m);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        Intent i;
        switch(menuItem.getItemId()) {
            case R.id.view_log:
                i = new Intent(ctx, LogView.class);
                startActivity(i);
                break;

            case R.id.view_chart:
                i = new Intent(ctx, ViewChart.class);
                startActivity(i);
                break;

            case R.id.permissions_reprompt:
                boolean hasPerms = Lib.hasPermissions(ctx);
                if(hasPerms){
                    Toast.makeText(ctx, "Permissions already granted", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(this, Lib.permissions, 0);
                }
                break;

            case R.id.permissions_details:
                i = new Intent(ctx, PermissionDetails.class);
                startActivity(i);
                break;

            case R.id.view_status:
                i = new Intent(ctx, StatusActivity.class);
                startActivity(i);
                break;
        }

        return true;
    }


    public void onRequestPermissionsResult(int code, String permissions[], int[] grantResults){
        boolean missingOne = false;
        for(int i = 0; i < grantResults.length; i++){
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                missingOne = true;
            }
        }

        if(missingOne){
            Toast.makeText(this, "Background logging turned off", Toast.LENGTH_SHORT).show();
            Lib.LoggingOnOff(ctx, false);

        } else {
            File logFile = Lib.getLogFile(ctx); // To initialize log file (just to be safe!)
            Lib.LoggingOnOff(ctx, true);
        }
    }
}
