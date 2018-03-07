package edu.fandm.enovak.updatetimingcollector;

import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.style.TtsSpan;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.ContentValues.TAG;

/**
 * Created by enovak on 3/1/18.
 */

public class Lib {

    public static boolean isExternalStorageWritable(){
        String state = Environment.getExternalStorageState();
        if(Environment.MEDIA_MOUNTED.equals(state)){
            return true;
        }
        return false;
    }

    public static File getLogFile(){
        File f = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "/update_log.csv");
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
                    sb.append(new String(buffer));
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
}
