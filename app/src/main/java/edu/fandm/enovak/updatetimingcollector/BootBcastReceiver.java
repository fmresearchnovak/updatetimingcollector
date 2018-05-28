package edu.fandm.enovak.updatetimingcollector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootBcastReceiver extends BroadcastReceiver {
    // The purpose of this class is to turn on the other broadcast receiver
    // at boot time.  This is necessary if the user installs our app
    // and later turns their phone off.  After they turn it back on, we will
    // have to re-enable our logging broadcast receiver again.



    @Override
    public void onReceive(Context ctx, Intent intent) {

        String action = intent.getAction();
        //Log.d(ContentValues.TAG, "BOOT Broadcast Received!!  intent: " + intent.toString() + "  action:" + action);

        if(action == Intent.ACTION_BOOT_COMPLETED){

            // Check for necessary permissions and enable the BcastReceiver to log updates
            boolean hasPerms = Lib.hasPermissions(ctx);
            Lib.LoggingOnOff(ctx, hasPerms); // If we have permissions, turn it on!
        }
    }
}
