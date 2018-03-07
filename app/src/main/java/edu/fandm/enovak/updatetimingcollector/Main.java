package edu.fandm.enovak.updatetimingcollector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;


public class Main extends AppCompatActivity {

    public static final String TAG = "updatetimingcollector";

    private static final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.INTERNET};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Get / Check all permissions first
        boolean needsPermissions = false;
        String curPerm;
        for(int i = 0; i < permissions.length; i++){
            curPerm = permissions[i];
            int permissionCheck = ContextCompat.checkSelfPermission(this, curPerm);
            if(permissionCheck == PackageManager.PERMISSION_DENIED){
                LogBcastReceiverOnOff(PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
                needsPermissions = true;
            }
        }

        if(needsPermissions){
            // I  request all (even those I already have)
            // I hope it doesn't cause any problems.  Hopefully
            // the system ignores requests for permissions that
            // the app already has!
            ActivityCompat.requestPermissions(this, permissions, 0);
            return;
        }

        // Create the log file, write the IMEI number, write the header row
        initializeLogFile();
        LogBcastReceiverOnOff(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
    }


    public void launchLogViewActivity(View v){
        Intent i = new Intent(this, LogView.class);
        startActivity(i);
    }


    private void LogBcastReceiverOnOff(int newState){
        PackageManager pm = getPackageManager();
        ComponentName cn = new ComponentName(this, LogBcastReceiver.class);
        pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP);
    }


    public void onRequestPermissionsResult(int code, String permissions[], int[] grantResults){
        boolean missingOne = false;
        for(int i = 0; i < grantResults.length; i++){
            if(grantResults[i] != PackageManager.PERMISSION_GRANTED){
                missingOne = true;
            }
        }

        if(missingOne){
            Toast.makeText(this, "This application cannot function without all permissions.  Please restart the app and accept all requested permissions.", Toast.LENGTH_LONG).show();
        } else {
            initializeLogFile();
            LogBcastReceiverOnOff(PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        }
    }



    private void initializeLogFile(){
        File logFile = Lib.getLogFile();
        if(!logFile.exists()){
            try{
                logFile.createNewFile();
                String IMEI = getIMEI() + "\n";
                Log.d(TAG, "Writing to file   IMEI: " + IMEI);
                writeFile(logFile, getIMEI() + "\n");
                writeFile(logFile,"timestamp,event,uid,name,version\n");

            } catch (IOException e1){
                Log.d(TAG, "Hand trouble intializing log file!");
            }
        }
    }


    private String getIMEI(){
        TelephonyManager tManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

        try {
            if (Build.VERSION.SDK_INT < 26) {
                return tManager.getDeviceId();
            } else if (Build.VERSION.SDK_INT >= 26) {
                return tManager.getImei();
            }
        } catch (SecurityException e1){
            Log.d(TAG, "Cannot obtain unique identify for permission / security reasons on this device");
        }
        return "0";
    }
}
