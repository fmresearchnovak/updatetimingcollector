package edu.fandm.enovak.updatetimingcollector;

import android.os.Environment;
import android.telephony.TelephonyManager;
import android.text.style.TtsSpan;
import android.util.Log;

import java.io.File;
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
}
