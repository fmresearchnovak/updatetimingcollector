package edu.fandm.enovak.updatetimingcollector;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

public class BootBcastReceiver extends BroadcastReceiver {
    // The purpose of this class is to turn on the other broadcast receiver
    // at boot time.  This is necessary if the user installs our app
    // and later turns their phone off.  After they turn it back on, we will
    // have to re-enable our logging broadcast receiver again.

    private Context ctx;

    @Override
    public void onReceive(Context context, Intent intent) {
        ctx = context;

        String action = intent.getAction();
        Log.d(ContentValues.TAG, "BOOT Broadcast Received!!  intent: " + intent.toString() + "  action:" + action);

        if(action == Intent.ACTION_BOOT_COMPLETED){

            // Check for necessary permissions and enable the BcastReceiver to log updates
            boolean hasPerms = Lib.hasPermissions(ctx);
            Lib.LoggingOnOff(ctx, hasPerms); // If we have permissions, turn it on!
        }
    }
}
