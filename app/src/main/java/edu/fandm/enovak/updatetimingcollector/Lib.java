package edu.fandm.enovak.updatetimingcollector;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

/**
 * Created by enovak on 3/1/18.
 */

public class Lib {

    public static final String PREF_FILE_NAME = "edu.fandm.enovak.updatetimingcollector.MAIN_PREF_FILE";
    public static final String PREF_SERV_TS_KEY = "edu.fandm.enovak.updatetimingcollector.SERV_TS";
    public static final String PREF_UUID_KEY = "edu.fandm.enovak.updatetimingcollector.UUID_KEY";

    public static final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.INTERNET};


    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }


    public static String SHA256(String data) {
        try { // The exception will never be thrown because I hard-coded the algorithm
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.reset();
            byte[] hash = md.digest(data.getBytes("UTF-8"));

            StringBuilder sb = new StringBuilder();
            for(int i = 0; i < hash.length; i++){
                sb.append(String.format("%02x", hash[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e2){
            e2.printStackTrace();
        }

        // Should never happen!
        return null;
    }

    public static boolean isNewerAndroid(){
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }


    public static String getID(Context ctx){


        SharedPreferences sharedPref = ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
        long uniqueID = sharedPref.getLong(Lib.PREF_UUID_KEY, -1);

        // Generate a new UUID if necessary, this should only happen on install / reinstall
        if(uniqueID == -1){
            // .getMostSignificantBits gives me a purely numeric result
            uniqueID = Math.abs(UUID.randomUUID().getMostSignificantBits());

            SharedPreferences.Editor e = sharedPref.edit();
            e.putLong(Lib.PREF_UUID_KEY, uniqueID);
            e.commit();
        }

        //Log.d(TAG, "uniqueID: "+ uniqueID);
        return String.valueOf(uniqueID);


    }

    public static File getLogFile(Context ctx){
        String fileName = getID(ctx) + ".csv";

        File envDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if(!envDir.exists()){
            envDir.mkdir();
        }
        File f = new File(envDir, "/" + fileName);
        if(!f.exists()){
            try{
                f.createNewFile();
            } catch (IOException e){
                // This should not ever happen!
                e.printStackTrace();
                return null;
            }
        }
        return f;
    }



    public static boolean writeFile(File f, String entry){
        FileOutputStream fos;
        try{
            fos = new FileOutputStream(f, true);
            //Log.d(TAG, "Writing:" + entry + "   to file: " + f.getAbsolutePath());
            fos.write(entry.getBytes());
            fos.close();
            return true;

        } catch (IOException e1) {
            // Error writing to file, not sure what I should do, giveup?
            //Log.d(TAG, "Cannot write to file! " + e1.getMessage());
            e1.printStackTrace();
        }

        return false;
    }


    // Wow what a pain this method is!
    // Reading files in Java is too complicated (there are so many choices!)
    // and some choices (like the one I made below) are too complicated
    public static String readFile(File f){
        if(f.exists() && f.canRead()){

            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            FileInputStream fis;

            try{
                fis = new FileInputStream(f);
                int n;
                while( (n = fis.read(buffer)) != -1) {

                    for(int i = 0; i < n; i++){
                        sb.append((char)buffer[i]);
                    }
                }
                return sb.toString();

            } catch (FileNotFoundException e1){
                //Log.d(TAG, "No log (file not found)");
                return null;
            } catch (IOException e2){
                //Log.d(TAG, "Error reading log");
                return null;
            }


        } else {
            //Log.d(TAG,"Log missing or cannot be read for permissions reasons!");
            return null;
        }
    }



    public static void LoggingOnOff(Context ctx, boolean newState){

            // In the new version of Android we periodically poll the package manager
            // to look for changes
        if(isNewerAndroid()){ // VERSION_CODES.O = OREO = API 26
            if (newState) {
                LoggingJobSchedulerService.scheduleNextCheck(ctx, 0); // Schedule for now
            } else {
                LoggingJobSchedulerService.cancel(ctx);
            }
        }


        // For older versions of Android we can use the broadcast receiver
        // to capture install / update / uninstall events when they happen
        else {
            if(newState) {
                LogBcastReceiver.setEnabled(ctx, PackageManager.COMPONENT_ENABLED_STATE_ENABLED);
            } else {
                LogBcastReceiver.setEnabled(ctx, PackageManager.COMPONENT_ENABLED_STATE_DISABLED);
            }

        }
    }



    public static boolean hasPermissions(Context ctx){
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

        return !needsPermissions;
    }
}
