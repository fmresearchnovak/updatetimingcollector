package edu.fandm.enovak.updatetimingcollector;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
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

import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;


public class Main extends AppCompatActivity {

    public static final String TAG = "updatetimingcollector";


    private static final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.INTERNET};
    private Context ctx;
    private TextView statusTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = getApplicationContext();
        statusTV = (TextView)findViewById(R.id.main_tv_status);

        boolean hasPerms = hasPermissions();
        if(!hasPerms) {
            // I  request all (even those I already have)
            // The system ignores requests for permissions that
            // the app already has!
            ActivityCompat.requestPermissions(this, permissions, 0);

        } else {

            initializeLogFile();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean hasPerms = hasPermissions();
        if (hasPerms) {
            // Create the log file, write the IMEI number, write the header row
            LogBcastReceiverOnOff(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            setStatusText(true);

        } else {
            LogBcastReceiverOnOff(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            setStatusText(false);
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
                boolean hasPerms = hasPermissions();
                if(hasPerms){
                    Toast.makeText(ctx, "Permissions already granted", Toast.LENGTH_SHORT).show();
                } else {
                    ActivityCompat.requestPermissions(this, permissions, 0);
                }
                break;

            case R.id.permissions_details:
                i = new Intent(ctx, PermissionDetails.class);
                startActivity(i);
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

            LogBcastReceiverOnOff(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            setStatusText(false);

        } else {

            initializeLogFile();
            LogBcastReceiverOnOff(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            setStatusText(true);
        }
    }


    private boolean hasPermissions(){
        // Get / Check all permissions first
        boolean needsPermissions = false;
        String curPerm;
        for(int i = 0; i < permissions.length; i++){
            curPerm = permissions[i];
            int permissionCheck = ContextCompat.checkSelfPermission(ctx, curPerm);
            if(permissionCheck == PackageManager.PERMISSION_DENIED){
                needsPermissions = true;
                break;
            }
        }
        if(needsPermissions){
            return false;
        } else {
            return true;
        }
    }

    private void LogBcastReceiverOnOff(int newState){
        PackageManager pm = getPackageManager();
        ComponentName cn = new ComponentName(this, LogBcastReceiver.class);
        pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP);
    }



    private void initializeLogFile(){
        File logFile = Lib.getLogFile(ctx);
        if(!logFile.exists()){
            try{
                logFile.createNewFile();
                String IMEI = Lib.getIMEI(ctx) + "\n";
                Log.d(TAG, "Writing to file   IMEI: " + IMEI);
                writeFile(logFile, IMEI + "\n");
                writeFile(logFile,"timestamp,event,uid,name,version\n");

            } catch (IOException e1){
                Log.d(TAG, "Hand trouble intializing log file!");
            }
        }
    }


    private void setStatusText(boolean isON){
        if(isON){
            statusTV.setText("Status: On");
            statusTV.setTextColor(Color.parseColor("#147e00"));
        } else {
            statusTV.setText("Status: Off");
            statusTV.setTextColor(Color.parseColor("#000000"));
        }
    }



}
