package edu.fandm.enovak.updatetimingcollector;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;




public class Main extends AppCompatActivity {
    public static final String TAG = "enovak.TAG";

    public static final int ISSUE_NONE = 0;
    public static final int ISSUE_PERMS = 2;
    public static final int ISSUE_UNKNOWN = 100;

    private TextView tv;
    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = getApplicationContext();
        tv = (TextView)findViewById(R.id.main_tv_explain);

        boolean hasPerms = Lib.hasPermissions(ctx);
        if(!hasPerms) {
            // I  request all (even those I already have)
            // The system ignores requests for permissions that
            // the app already has!
            ActivityCompat.requestPermissions(this, Lib.permissions, 0);

        } else {
            File logFile = Lib.getLogFile(ctx); // To initialize log file
            Log.d(TAG, logFile.getAbsolutePath());
            Lib.LoggingOnOff(ctx, true);
        }


    }


    @Override
    public void onResume() {
        super.onResume();


        int issue = getIssue();
        switch(issue){

            case ISSUE_PERMS:
                tv.setText("Please accept all permissions");
                tv.setTextColor(Color.parseColor("#800000"));
                break;

            case ISSUE_UNKNOWN:
                tv.setText("Oops!  Something went wrong!");
                tv.setTextColor(Color.parseColor("#800000"));
                break;

            case ISSUE_NONE:
                tv.setText("That's it!  You're done!");
                tv.setTextColor(Color.parseColor("#111111"));
                break;
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
            File logFile = Lib.getLogFile(ctx); // To initialize log file
            Lib.LoggingOnOff(ctx, true);
        }
    }



    public int getIssue(){

        if(!Lib.hasPermissions(ctx)){
            return ISSUE_PERMS;
        }

        return ISSUE_NONE;
    }
}
