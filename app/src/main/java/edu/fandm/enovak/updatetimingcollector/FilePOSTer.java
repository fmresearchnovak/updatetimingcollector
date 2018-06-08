package edu.fandm.enovak.updatetimingcollector;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import static edu.fandm.enovak.updatetimingcollector.Lib.PREF_FILE_NAME;
import static edu.fandm.enovak.updatetimingcollector.Lib.getLogFile;
import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

/**
 * Created by enovak on 3/7/18.
 */

public class FilePOSTer extends AsyncTask<Void, Void, Boolean> {

    private final static String SERVER_ADDR = "http://cs-41.fandm.edu:9000";
    public File f;

    private String attachmentName;
    private String attachmentFileName;
    private final String newLine = "\r\n";
    private final String twoHyphens = "--";
    private final String boundary = "-----";

    private final String contents;
    private Context ctx;
    private boolean toastsOn;
    private boolean networkOn;


    // Each time a file should be posted a new instance of
    // this class is created.  So, I need these to be static so
    // that the threads can read / write single entities (across
    // multiple instances of this class.
    // The volatile makes reading / writing them thread safe
    // Note: it does not protect against read and then write
    // it only protects against reading or writing (as atomic instructions)
    private static volatile long lastUploadScheduledTS = 0;
    private static volatile boolean uploadNecessary = false;
    private final static int uploadWaitTimeMS = 5000; // 5seconds in ms

    public FilePOSTer(File newF, Context context, boolean withToasts) throws IllegalArgumentException{
        ctx = context;
        toastsOn = withToasts;
        networkOn = false;
        contents = Lib.readFile(newF);

        if(contents == null){
            throw new IllegalArgumentException("Unable to read file");
        }

        attachmentFileName = newF.getName();
        attachmentName = Lib.SHA256(newF.getName());
    }

    @Override
    protected void onPreExecute(){
        lastUploadScheduledTS = System.currentTimeMillis();


        networkOn = networkConnectivity();
        if(!networkOn){
            makeToastWithCheck("No network connection!");
        }


        // Race conditions?
        // Case 1, this thread marks this true after a UploadWaitThread marked it false
        //      No problem, that just means that it will be uploaded again (normal case)
        // Case 2, this thread marks this false after an UploadWaitThread marked it true
        //      That literally is not possible because this thread does not make it false
        uploadNecessary = true;

    }

    @Override
    protected Boolean doInBackground(Void... params){
        Boolean success = false;

        // Wait for some time (to avoid rapid uploads) and wait for some network
        // connectivity.
        //   - The uploadTimeWait is only an issue if the user manually uploads rapidly.
        long diff = System.currentTimeMillis() - lastUploadScheduledTS;
        while(diff < uploadWaitTimeMS || !networkConnectivity()) {
            Log.d(TAG, "Waiting to upload logfile.  Waiting on uploadTimeBuffer or Network Connectivity");
            try {

                // Wait a random amount of time so that the threads wake up
                // at different times (assuming there are multiple threads)
                Random r = new Random();
                long sleepTime = r.nextInt(uploadWaitTimeMS);
                Thread.sleep(sleepTime);

            } catch (InterruptedException e1) {
                // Do nothing if interrupted (oh-well, forge ahead!)
            }
            diff = System.currentTimeMillis() - lastUploadScheduledTS;

            // Give-up waiting if upload is no longer necessary
            if(!uploadNecessary){
                return false;
            }
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
            // least 60 seconds after the last time the file was written thanks
            // to the while loop above
            uploadNecessary = false;
            //Log.d(TAG, "Thread in BcastReceiver will now upload.  Upload necessary is now: " + uploadNecessary);

            success = postFile();
            if(!success){
                Log.d(TAG, "UPLOAD FAILED!");
            }
        }

        return success;
    }

    @Override
    protected void onPostExecute(Boolean result){
        String s;
        if(result){
            s = "SUCCESS!  Logfile uploaded!";

            // Save a timestamp, this will be displayed to user in the
            // "status" activity
            long ts = System.currentTimeMillis();
            SharedPreferences sharedPref = ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor e = sharedPref.edit();
            e.putLong(Lib.PREF_SERV_TS_KEY, ts);
            e.commit();

            //ts = sharedPref.getLong(Lib.PREF_SERV_TS_KEY, -1);
            //SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");
            //String dateString = formatter.format(new Date(ts));
            //Log.d(TAG, "Success uploading to server at: " + dateString);

        } else {
            s = "File Not Uploaded!";
        }
        makeToastWithCheck(s);
        Log.d(TAG, s);
    }




    private boolean postFile() {
        // This code heavily inspired by: https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
        HttpURLConnection httpURLConnection = null;
        URL url = null;
        try {

            // Setup request
            url = new URL(SERVER_ADDR);
            httpURLConnection = (HttpURLConnection) url.openConnection();
            httpURLConnection.setUseCaches(false); // Maybe unnecessary
            httpURLConnection.setDoOutput(true); // not sure what this does

            httpURLConnection.setRequestMethod("POST");
            //httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);


            // wrapper for content
            DataOutputStream requestOS = new DataOutputStream(httpURLConnection.getOutputStream());
            requestOS.write((twoHyphens + boundary + newLine).getBytes());
            requestOS.writeBytes("Content-Disposition: form-data; name=\"" + attachmentName + "\";filename=\"" + attachmentFileName + "\"" + newLine);
            requestOS.write(newLine.getBytes());

            // write actual file data!
            requestOS.write(newLine.getBytes());
            requestOS.write(contents.getBytes());
            requestOS.write((twoHyphens + boundary +  twoHyphens + newLine).getBytes());

            // flush
            requestOS.flush();
            requestOS.close();

            // Get response
            InputStream responseIS = new BufferedInputStream(httpURLConnection.getInputStream());
            BufferedReader responseStreamReader = new BufferedReader(new InputStreamReader(responseIS));
            String line = "";
            StringBuilder sb = new StringBuilder();

            while( (line = responseStreamReader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            responseStreamReader.close();

            //String response = sb.toString();
            //if(response != "") {
                //Log.d(TAG, "response: " + response);
            //}

            // Disconnect and return result
            httpURLConnection.disconnect();
            //Log.d(TAG, "File uploaded successfully!");
            return true;

        } catch (MalformedURLException e1){
            e1.printStackTrace();
            return false;
        } catch (IOException e2){
            e2.printStackTrace();
            return false;
        }

    }

    private void makeToastWithCheck(String s){
        if(toastsOn){
            Toast.makeText(ctx, s, Toast.LENGTH_SHORT).show();
        }
    }


    private boolean networkConnectivity(){
        ConnectivityManager conMgr =  (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        return netInfo != null;
    }

    public static void scheduleUpload(Context ctx, boolean withToast){
        FilePOSTer fp = new FilePOSTer(getLogFile(ctx), ctx, withToast);
        fp.execute();
    }
}
