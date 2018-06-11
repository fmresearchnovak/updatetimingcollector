package edu.fandm.enovak.updatetimingcollector;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;

import static android.content.ContentValues.TAG;
import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;

public class LogBcastReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context ctx, Intent intent) {

        String action = intent.getAction();
        Log.d(TAG, "Broadcast Received!!  intent: " + intent.toString() + "  action:" + action);

        switch(action){


            case Intent.ACTION_AIRPLANE_MODE_CHANGED:
                //Log.d(TAG, "Airplane Mode");
                break;

            case Intent.ACTION_PACKAGE_REPLACED:
            case Intent.ACTION_PACKAGE_ADDED:
            case Intent.ACTION_PACKAGE_REMOVED:
                if(Lib.isExternalStorageWritable()) {

                    File logFile = Lib.getLogFile(ctx);
                    if (logFile != null && logFile.exists() && logFile.canWrite()) {
                        int appUID = intent.getIntExtra(Intent.EXTRA_UID, -1);

                        PackageManager pm = ctx.getPackageManager();
                        String entry = genEntryString(pm, appUID, action);
                        writeFile(logFile, entry);
                        FilePOSTer.scheduleUpload(ctx, false, 300000); // 5 min in ms

                    } else {
                        // Not sure what I should do here!
                        //Log.d(TAG, "Storage not write-able!");
                    }
                }
                break;

        }

    }

    private String genEntryString(PackageManager pm, int uid, String action_type){
        // This function uses the PackageManager to read a few values
        //      The package name (reverse fqdn app name)
        //      The version number (version code)
        // These values may be null / void / -1 if the app was just removed
        // However, the UID came from the Intent itself and is
        // definitely always right.  When parsing the log file it should
        // be trivial to figure out what app it was.  Just find another line with
        // the same UID (maybe when this app was install or updated
        // I would try to fix this so -1 and "null" never occure in the log
        // file, but I think by the nature of the broadcast receiver it is
        // unavoidable.
        String pkgName = pm.getNameForUid(uid);
        String entryStr;

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
        //Log.d(TAG, entryStr);
        return entryStr;
    }


    public static void setEnabled(Context ctx, int enabled){
        PackageManager pm = ctx.getPackageManager();
        ComponentName cn = new ComponentName(ctx, LogBcastReceiver.class);
        pm.setComponentEnabledSetting(cn, enabled, PackageManager.DONT_KILL_APP);
    }


    public static boolean isEnabled(Context ctx){
        PackageManager pm = ctx.getPackageManager();
        ComponentName cn = new ComponentName(ctx, LogBcastReceiver.class);
        return pm.getComponentEnabledSetting(cn) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED;
    }

}
