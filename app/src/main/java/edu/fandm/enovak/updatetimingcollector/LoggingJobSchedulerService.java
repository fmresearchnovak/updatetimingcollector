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
import android.os.Environment;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static edu.fandm.enovak.updatetimingcollector.Lib.writeFile;
import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

/**
 * Created by enovak on 4/20/18.
 */


@RequiresApi(Build.VERSION_CODES.O)
public class LoggingJobSchedulerService extends JobService implements Serializable{

    private static HashMap<String,Entry> lastScanResults = new HashMap<String,Entry>();
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

                HashMap<String,Entry> newScanResults = new HashMap<String, Entry>(lastScanResults.size());
                for(PackageInfo pkg : packages){
                    int uid;
                    try {
                        uid = pm.getPackageUid(pkg.packageName, 0);
                    } catch (PackageManager.NameNotFoundException e1){
                        uid = -1;
                    }

                    Entry e = new Entry(pkg.lastUpdateTime, uid, pkg.packageName, pkg.versionCode);
                    newScanResults.put(pkg.packageName, e);
                }

                // instantiate lastScanResults if necessary.  This is messy
                // but it is necessary for right after the device just booted
                // In this case, there is no "most recent scan results" so we
                // have to artificially create them.
                // There is a chance here that the system will miss a package event
                // For example if an event happens, very soon after boot (before this
                // code runs), then the system will miss that event.

                // Also, if the system is rebooted before this code runs
                // a second time that event will not be captured at all.
                // However, this will not introduce any "false positive" events
                if(lastScanResults.size() == 0){
                    Log.d(TAG, "Last scan results are blank!  Attempting to read from file.");
                    lastScanResults = loadScanResults();

                    // Even the file was blank (probably a fresh install)
                    if(lastScanResults == null){
                        Log.d(TAG, "File also blank!  Using these scan results");
                        lastScanResults = newScanResults;
                    }
                }

                ArrayList<String> newLogEntries = compareScans(lastScanResults, newScanResults);
                if(newLogEntries.size() > 0) {
                    Log.d(TAG, "Some changes spotted.  Collecting, logging, uploading...");

                    for (int i = 0; i < newLogEntries.size(); i++) {
                        writeFile(logFile, newLogEntries.get(i) + "\n");
                    }
                    FilePOSTer.scheduleUpload(ctx, false, 2000); // two seconds in ms
                }

                lastScanResults = newScanResults;
                storeScanResults(lastScanResults);
            }
        }
        // The entire check is done, we should schedule the next check
        scheduleNextCheck(ctx);
        return false;
    }


    private File getDataFile(){
        String fileName = "lastScanResults.data";
        File envDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        if(!envDir.exists()){
            envDir.mkdir();
        }
        File f = new File(envDir, "/" + fileName);
        if(!f.exists()){
            try{
                f.createNewFile();
            } catch (IOException e){
                // This should not ever happen!
                e.printStackTrace();
                return null;
            }
        }

        return f;
    }

    private void storeScanResults(HashMap<String, Entry> scan){

        File f = getDataFile();
        try {
            FileOutputStream fos = new FileOutputStream(f, false);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            //Log.d(TAG, "storing scan: " + scan);
            oos.writeObject(scan);
            oos.close();
            fos.close();

        } catch (FileNotFoundException e1){
            e1.printStackTrace();
            return;
        } catch (IOException e2){
            e2.printStackTrace();
        }
    }

    private HashMap<String, Entry> loadScanResults(){
        File f = getDataFile();
        HashMap<String, Entry> res;
        try{
            FileInputStream fis = new FileInputStream(f);
            ObjectInputStream ois = new ObjectInputStream(fis);
            res = (HashMap<String, Entry>)ois.readObject();
            ois.close();
            fis.close();
        } catch (FileNotFoundException e1){
            e1.printStackTrace();
            return null;
        } catch (IOException e2){
            e2.printStackTrace();
            return null;
        } catch (ClassNotFoundException e3){
            e3.printStackTrace();
            return null;
        }

        return res;
    }

    private ArrayList<String> compareScans(HashMap<String, Entry> oldScan, HashMap<String, Entry> newScan){
        // this method compares two scans, for any differences it finds, it outputs
        // array of strings that should be written to the log file.  These strings
        // should match exactly the entries generated for the log file for previous
        // versions of android (this method / code is only for Android 8.0+)

        final long ts = System.currentTimeMillis();


        ArrayList<String> output = new ArrayList<>();
        String tmp;

        // Check for removals and updates (look forward)
        Iterator it = oldScan.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry entryPair = (Map.Entry)it.next();
            Entry old_e = (Entry)entryPair.getValue();
            Entry new_e = newScan.get(entryPair.getKey());

            if(new_e == null) {
                tmp = ts + "," + "android.intent.action.PACKAGE_REMOVED" + "," + old_e.UID + "," + old_e.pkg + "," + old_e.versionCode;
                output.add(tmp);
            } else if (new_e.isNewerThen(old_e)) {
                tmp = new_e.time + "," + "android.intent.action.PACKAGE_REPLACED" + "," + new_e.UID + "," + new_e.pkg + "," + new_e.versionCode;
                output.add(tmp);
            }
        }

        // Check for installs (look backward)
        it = newScan.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry entryPair = (Map.Entry)it.next();
            Entry new_e = (Entry)entryPair.getValue();
            Entry old_e = oldScan.get(entryPair.getKey());
            if(old_e == null){
                tmp = new_e.time + "," + "android.intent.action.PACKAGE_INSTALLED" + "," + new_e.UID + "," + new_e.pkg + "," + new_e.versionCode;
                output.add(tmp);
            }
        }

        return output;

    }


    public boolean onStopJob(JobParameters params){
        Log.d(TAG, "onSopJob() called!");
        return false;
    }


    public static void scheduleNextCheck(Context ctx){
        // For debugging
        // 1 hr = 60 minutes = 60 * 60 seconds = 60 * 60 * 1000 ms = 3600000ms
        // 30 min = 30 * 60 seconds = 30 * 60 * 1000 ms = 1800000;
        // 15 min = 15 * 60 seconds = 15 * 60 * 1000 ms = 900000;

        // 10 min = 10 * 60 seconds = 10 * 60 * 1000 ms = 600000;
        final int TEN_MIN = 600000;

        // 5 min = 5 * 60 = 5 * 60 * 1000 = 300000;
        final int FIVE_MIN = 300000;

        // 10 seconds = 10000;

        final int TEN_SEC = 10000;
        // 2 seconds = 2000;

        scheduleNextCheck(ctx, TEN_MIN);
    }


    public static void scheduleNextCheck(Context ctx, int msDelay){
        JobScheduler js = getJS(ctx);
        ComponentName cn = new ComponentName(ctx, LoggingJobSchedulerService.class);
        JobInfo.Builder b = new JobInfo.Builder(JOB_ID, cn);

        //////////////////////////////////////////////////////////
        // Will edit later maybe to be periodic and not ad-hoc
        b.setMinimumLatency(msDelay);
        b.setOverrideDeadline(msDelay + 60000); // wait at most 1 minute longer + 1 min


        int res = js.schedule(b.build());
        //Log.d(TAG,    Result: " + res);
        if (res != 0) {
            Log.d(TAG, "Scheduled next job service job.  Waiting at least: " + msDelay + "ms");
        } else {
            throw new IllegalStateException("Unable to schedule next job!");

        }

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

    private class Entry implements Serializable{
        private static final long serializableVerID = 1;

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
            //Log.d(TAG, "Comparing this: " + this.toString() +  "  with  " + other.toString());
            if(!this.matchesPackage(other)){
                throw new IllegalArgumentException("Packages must match!");
            }

            //Log.d(TAG, "this.versionCode: " + this.versionCode + " > " + other.versionCode + "??");
            return this.versionCode > other.versionCode;
        }

        public String toString(){
            return "Entry  t:" + this.time + "  pkg:" + this.pkg + "  ver:" + this.versionCode;
        }
    }
}
