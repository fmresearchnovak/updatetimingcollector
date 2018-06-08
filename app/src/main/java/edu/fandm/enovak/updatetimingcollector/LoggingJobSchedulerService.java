package edu.fandm.enovak.updatetimingcollector;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;
import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

/**
 * Created by enovak on 4/20/18.
 */


@RequiresApi(Build.VERSION_CODES.O)
public class LoggingJobSchedulerService extends JobService {

    private static ArrayList<Entry> lastScanResults = new ArrayList<Entry>();
    private static final int JOB_ID = 1;
    private static JobScheduler js;


    public boolean onStartJob(JobParameters params){
        //Log.d(TAG, "JOB SERVICE WORKING!  ONSTARTJOB CALLED!!!");
        Log.d(TAG, "Job Service Running A Check");
        Context ctx = getApplicationContext();


        if(Lib.isExternalStorageWritable()){
            File logFile = Lib.getLogFile(ctx);
            if(logFile != null && logFile.exists() && logFile.canWrite()) {

                PackageManager pm = ctx.getPackageManager();
                List<PackageInfo> packages = pm.getInstalledPackages(0);

                ArrayList<Entry> newScanResults = new ArrayList<>(lastScanResults.size());
                for(PackageInfo pkg : packages){
                    int uid;
                    try {
                        uid = pm.getPackageUid(pkg.packageName, 0);
                    } catch (PackageManager.NameNotFoundException e1){
                        uid = -1;
                    }

                    Entry e = new Entry(pkg.lastUpdateTime, uid, pkg.packageName, pkg.versionCode);
                    newScanResults.add(e);

                }

                // instantiate lastScanResults if necessary.  This is messy
                // but it is necessary for right after the device just booted
                // In this case, there is no "most recent scan results" so we
                // have to artificially create them.
                // There is a chance here that the system will miss a package event
                // For example if an event happens, then (in less than one hour) the
                // system is rebooted that event will not be captured by this system
                // However, this will not introduce any "false" events
                if(lastScanResults.size() == 0){
                    lastScanResults = newScanResults;
                }

                ArrayList<String> newLogEntries = compareScans(lastScanResults, newScanResults);
                if(newLogEntries.size() > 0) {

                    for (int i = 0; i < newLogEntries.size(); i++) {
                        writeFile(logFile, newLogEntries.get(i) + "\n");
                    }
                    FilePOSTer.scheduleUpload(ctx, false);
                }

                lastScanResults = newScanResults;
            }
        }
        // The entire check is done, we should schedule the next check
        scheduleNextCheck(ctx);
        return false;
    }

    private ArrayList<String> compareScans(ArrayList<Entry> oldScan, ArrayList<Entry> newScan){
        // this method compares two scans, for any differences it finds, it outputs
        // array of strings that should be written to the log file.  These strings
        // should match exactly the entries generated for the log file for previous
        // versions of android (this method / code is only for Android 8.0+)

        final long ts = System.currentTimeMillis();

        // Check for removals and updates (look forward)
        ArrayList<String> output = new ArrayList<>();
        String tmp;
        for (Entry old_e : oldScan){
            Entry new_e = findMatching(old_e, newScan);

            if(new_e == null){
                 tmp = ts + "," + "android.intent.action.PACKAGE_REMOVED" + "," + old_e.UID + "," + old_e.pkg + "," + old_e.versionCode;
                 output.add(tmp);
            } else if (new_e.isNewerThen(old_e)) {
                tmp = new_e.time + "," + "android.intent.action.PACKAGE_REPLACED" + "," + new_e.UID + "," + new_e.pkg + "," + new_e.versionCode;
                output.add(tmp);
            }
        }

        // Check for installs (look backward)
        for(Entry new_e: newScan){
            Entry old_e = findMatching(new_e, oldScan);
            if(old_e == null){
                tmp = new_e.time + "," + "android.intent.action.PACKAGE_INSTALLED" + "," + new_e.UID + "," + new_e.pkg + "," + new_e.versionCode;
                output.add(tmp);
            }
        }

        return output;

    }


    private Entry findMatching(Entry target_e, ArrayList<Entry> scan){
        Entry output = null;
        for (Entry e : scan){
            if(target_e.matchesPackage(e)){
                output = e;
            }
        }
        return output;
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
        //Log.d(TAG, "Scheduled next jobservice job!   Result: " + res);
        //if (res < 0) {
        //    Log.d(TAG, "Something went wrong!!!");
        //}

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


    private class Entry{

        long time;
        int UID;
        String pkg;
        int versionCode;


        public Entry(long newTime, int newUID, String newPkg, int newVersionCode){
            time = newTime;
            UID = newUID;
            pkg = newPkg;
            versionCode = newVersionCode;
        }

        public boolean matchesPackage(Entry other){
            return this.pkg.equals(other.pkg);
        }

        public boolean isNewerThen(Entry other){
            if(!this.matchesPackage(other)){
                throw new IllegalArgumentException("Packages must match!");
            }

            return this.versionCode > other.versionCode;
        }

        public String toString(){
            return "Entry  t:" + this.time + "  pkg:" + this.pkg + "  ver:" + this.versionCode;
        }
    }




}
