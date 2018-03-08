package edu.fandm.enovak.updatetimingcollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static android.content.ContentValues.TAG;
import static android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE;
import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;

public class LogBcastReceiver extends BroadcastReceiver {

    private static final String TYPE_INSTALL = "install";
    private static final String TYPE_UPDATE = "update";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "Broadcast Received!!  intent: " + intent.toString() + "  action:" + action);

        switch(action){


            case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                Log.d(TAG, "Airplane Mode");
                break;

            case Intent.ACTION_PACKAGE_REPLACED:
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REMOVED:
                if(Lib.isExternalStorageWritable()) {

                    File logFile = Lib.getLogFile(context);
                    if (logFile.exists() && logFile.canWrite()) {
                        int appUID = intent.getIntExtra(Intent.EXTRA_UID, -1);
                        PackageManager pm = context.getPackageManager();
                        String entry = genEntryString(pm, appUID, action);
                        writeFile(logFile, entry);

                    } else {
                        // Not sure what I should do here!
                        Log.d(TAG, "Storage not write-able!");
                    }
                }
                break;

        }

    }

    private String genEntryString(PackageManager pm, int uid, String action_type){

        String pkgName = pm.getNameForUid(uid);
        String entryStr = "";

        int version;
        try {
            // When a package is uninstalled (or the name is otherwise unknown) the system
            // will throw a "name not found exception".
            // In these cases I'll just use -1 as the version number
            PackageInfo pkgInfo = pm.getPackageInfo(pkgName, 0);
            version = pkgInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e1) {
            version = -1;
        }

        entryStr = System.currentTimeMillis() +"," + action_type + "," + uid + "," + pkgName + "," + version + "\n";
        Log.d(TAG, entryStr);
        return entryStr;
    }





}
