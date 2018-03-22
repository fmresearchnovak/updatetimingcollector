package edu.fandm.enovak.updatetimingcollector;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.ContextCompat;
import android.telephony.TelephonyManager;
import android.text.style.TtsSpan;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import static android.content.ContentValues.TAG;

/**
 * Created by enovak on 3/1/18.
 */

public class Lib {

    public static final String[] permissions = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_PHONE_STATE, Manifest.permission.INTERNET};

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
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


    public static String getIMEI(Context ctx){
        TelephonyManager tManager = (TelephonyManager)ctx.getSystemService(Context.TELEPHONY_SERVICE);

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

    public static File getLogFile(Context ctx){
        String fileName = getIMEI(ctx) + ".csv";
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "/" + fileName);
        return f;
    }

    public static boolean writeFile(File f, String entry){
        FileOutputStream fos = null;
        try{
            fos = new FileOutputStream(f, true);
            Log.d(TAG, "Writing:" + entry + "   to file: " + f.getAbsolutePath());
            fos.write(entry.getBytes());
            fos.close();
            return true;

        } catch (IOException e1) {
            // Error writing to file, not sure what I should do, giveup?
            Log.d(TAG, "Cannot write to file! " + e1.getMessage());
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
            FileInputStream fis = null;

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
                Log.d(TAG, "No log (file not found)");
                return null;
            } catch (IOException e2){
                Log.d(TAG, "Error reading log");
                return null;
            }


        } else {
            Log.d(TAG,"Log missing or cannot be read for permissions reasons!");
            return null;
        }
    }

    public static void uploadFile(Context ctx){
        File f = Lib.getLogFile(ctx);
        FilePOSTer fp = new FilePOSTer(f);
        fp.post();
    }

    public static void LogBcastReceiverOnOff(Context ctx, int newState){
        PackageManager pm = ctx.getPackageManager();
        ComponentName cn = new ComponentName(ctx, LogBcastReceiver.class);
        pm.setComponentEnabledSetting(cn, newState, PackageManager.DONT_KILL_APP);
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
        if(needsPermissions){
            return false;
        } else {
            return true;
        }
    }
}
