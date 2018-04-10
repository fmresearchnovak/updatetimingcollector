package edu.fandm.enovak.updatetimingcollector;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.AsyncTask;
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

    public static final String TAG = "enovak.TAG";


    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = getApplicationContext();

        boolean hasPerms = Lib.hasPermissions(ctx);
        if(!hasPerms) {
            // I  request all (even those I already have)
            // The system ignores requests for permissions that
            // the app already has!
            ActivityCompat.requestPermissions(this, Lib.permissions, 0);

        } else {

            initializeLogFile();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

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

            Lib.LogBcastReceiverOnOff(ctx, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);

        } else {

            initializeLogFile();
            Lib.LogBcastReceiverOnOff(ctx, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
        }
    }






    private void initializeLogFile(){
        File logFile = Lib.getLogFile(ctx);
        if(!logFile.exists()){
            try{
                logFile.createNewFile();
                String IMEI = Lib.getIMEI(ctx) + "\n";
                //Log.d(TAG, "Writing to file   IMEI: " + IMEI);
                writeFile(logFile, IMEI + "\n");
                writeFile(logFile,"timestamp,event,uid,name,version\n");

            } catch (IOException e1){
                //Log.d(TAG, "Hand trouble intializing log file!");
            }
        }
    }

}
