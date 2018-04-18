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
import java.util.Random;

import static android.content.ContentValues.TAG;
import static android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE;
import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;

public class LogBcastReceiver extends BroadcastReceiver {

    // Each time onReceive is called there is a new instance of
    // the broadcast receiver.  So I need these to be static so
    // that the threads can read / write single entities (across
    // multiple instances of LogBcastReceiver
    // The volatile makes reading / writing them thread safe
    // Note: it does not protect against read and then write
    // it only protects against reading or writing (as atomic instructions)
    private static volatile long lastUploadScheduledTS = 0;
    private static volatile boolean uploadNecessary = false;

    private Context ctx;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;

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

                    File logFile = Lib.getLogFile(ctx);
                    if (logFile != null && logFile.exists() && logFile.canWrite()) {
                        int appUID = intent.getIntExtra(Intent.EXTRA_UID, -1);

                        PackageManager pm = ctx.getPackageManager();
                        String entry = genEntryString(pm, appUID, action);
                        writeFile(logFile, entry);
                        scheduleUpload();

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
        //Log.d(TAG, entryStr);
        return entryStr;
    }

    private void scheduleUpload(){

        lastUploadScheduledTS = System.currentTimeMillis();

        // Race conditions?
        // Case 1, this thread marks this true after a UploadWaitThread marked it false
        //      No problem, that just means that it will be uploaded again (normal case)
        // Case 2, this thread marks this false after an UploadWaitThread marked it true
        //      That literally is not possible because this thread does not make it false
        uploadNecessary = true;

        // Schedule a thread to wait and do the upload
        Thread t = new Thread(new UploadWaitThread());
        t.start();
    }


    private class UploadWaitThread implements Runnable {
        @Override
        public void run() {

            // Die immediately if an upload isn't necessary
            if(!uploadNecessary){
                return;
            }

            long diff = System.currentTimeMillis() - lastUploadScheduledTS;
            while(diff < 10000) { // ten seconds in milliseconds
                try {

                    // Wait a random amount of time so that the threads wake up
                    // at different times
                    Random r = new Random();
                    long sleepTime = r.nextInt(10000);
                    Thread.currentThread().sleep(sleepTime);

                } catch (InterruptedException e1) {
                    // Do nothing if interrupted (oh-well, forge ahead!)
                }
                diff = System.currentTimeMillis() - lastUploadScheduledTS;
            }

            // Again, check if we should just die since we just waited a bit
            //Log.d(TAG, "Checking if upload necessary: " + uploadNecessary);
            if(uploadNecessary){
                // There is a race condition here
                // But I don't care for now.  The worst thing that happens is that the file is
                // uploaded twice.
                //
                // This happens if one Thread checks the if statement above just before
                // some other thread switched it to false (below).
                //
                // Even in this case, the file will be uploaded twice (not so bad)
                // I don't think any entries from the actual log contents will be
                // lost because the thread reads the entire file.  And this is at
                // least 10 seconds after the last time the file was written thanks
                // to the while loop above
                uploadNecessary = false;
                //Log.d(TAG, "Thread in BcastReceiver will now upload.  Upload necessary is now: " + uploadNecessary);


                FilePOSTer fp = new FilePOSTer(Lib.getLogFile(ctx), ctx, false);
                fp.execute();

            }
        }
    }
}
