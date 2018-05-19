package edu.fandm.enovak.updatetimingcollector;

import android.annotation.TargetApi;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;
import android.util.Log;

import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

/**
 * Created by enovak on 4/20/18.
 */

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class LoggingJobSchedulerService extends JobService {

    public boolean onStartJob(JobParameters params){
        Log.d(TAG, "JOB SERVICE WORKING!  ONSTARTJOB CALLED!!!");
        Lib.LoggingOnOff(getApplicationContext(), true);
        return false;
    }

    public boolean onStopJob(JobParameters params){
        Log.d(TAG, "onSopJob() called!");
        return false;
    }


}
