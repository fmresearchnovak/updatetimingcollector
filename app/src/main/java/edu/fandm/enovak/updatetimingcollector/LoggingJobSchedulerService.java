package edu.fandm.enovak.updatetimingcollector;

import android.annotation.TargetApi;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ChangedPackages;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.util.List;
import java.util.Random;

import static android.app.job.JobInfo.NETWORK_TYPE_ANY;
import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;
import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

/**
 * Created by enovak on 4/20/18.
 */


public class LoggingJobSchedulerService extends JobService {

    private static String lastScanResults = "";
    private static final int JOB_ID = 1;
    private static JobScheduler js;

    private Context ctx;

    public boolean onStartJob(JobParameters params){
        //Log.d(TAG, "JOB SERVICE WORKING!  ONSTARTJOB CALLED!!!");
        Log.d(TAG, "Job Service Running A Check");
        ctx = getApplicationContext();


        if(Lib.isExternalStorageWritable()){
            File logFile = Lib.getLogFile(ctx);
            if(logFile != null && logFile.exists() && logFile.canWrite()) {
                PackageManager pm = ctx.getPackageManager();

                List<PackageInfo> packages = pm.getInstalledPackages(0);

                StringBuilder sb = new StringBuilder();
                for(PackageInfo pkg : packages){
                    int uid;
                    try {
                        uid = pm.getPackageUid(pkg.packageName, 0);
                    } catch (PackageManager.NameNotFoundException e1){
                        uid = -1;
                    }

                    String entry = pkg.lastUpdateTime + "," + pkg.packageName + "," + uid + "," + pkg.versionCode + "\n";
                    sb.append(entry);

                }

                String scanResults = sb.toString();
                if(!scanResults.equals(lastScanResults)) {
                    lastScanResults = scanResults;
                    // An entry is now (the time) and then a block of packages
                    writeFile(logFile, System.currentTimeMillis() + "\n");
                    writeFile(logFile, scanResults);
                    FilePOSTer.scheduleUpload(ctx, false);
                }
            }

        }
        // The entire check is done, we should schedule the next check
        scheduleNextCheck(ctx);
        return false;
    }

    public boolean onStopJob(JobParameters params){
        Log.d(TAG, "onSopJob() called!");
        return false;
    }


    public static boolean scheduleNextCheck(Context ctx){
        JobScheduler js = getJS(ctx);
        ComponentName cn = new ComponentName(ctx, LoggingJobSchedulerService.class);
        JobInfo.Builder b = new JobInfo.Builder(JOB_ID, cn);

        //////////////////////////////////////////////////////////
        // Will edit later maybe to be periodic and not ad-hoc
        // 1 hr = 60 minutes = 60 * 60 seconds = 60 * 60 * 1000 ms = 3600000ms
        b.setMinimumLatency(3600000); // 1hr
        b.setOverrideDeadline(3660000); // 1hr + 1 min

        // For debugging
        //b.setMinimumLatency(1000);
        //b.setMinimumLatency(10000);
        //b.setOverrideDeadline(20000);
        //////////////////////////////////////////////////////////

        int res = js.schedule(b.build());
        Log.d(TAG, "scheduled next jobservice job!   Result: " + res);
        if (res < 0) {
            Log.d(TAG, "Something went wrong!!!");
        }

        return res == 1;
    }

    public static void cancel(Context ctx){
        JobScheduler js = getJS(ctx);
        js.cancel(JOB_ID);
    }

    public static boolean isScheduled(Context ctx){
        JobScheduler js = getJS(ctx);
        return js.getPendingJob(JOB_ID) != null;
    }


    public static JobScheduler getJS(Context ctx){
        if(js == null) {
            js = (JobScheduler) ctx.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        }
        return js;
    }




}
