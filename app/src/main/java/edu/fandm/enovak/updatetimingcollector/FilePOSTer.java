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


import static edu.fandm.enovak.updatetimingcollector.Lib.PREF_FILE_NAME;
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
        ConnectivityManager conMgr =  (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = conMgr.getActiveNetworkInfo();
        if (netInfo == null){
            makeToastWithCheck("No network connection");
        } else {
            networkOn = true;
        }
    }

    @Override
    protected Boolean doInBackground(Void... params){
        Boolean success = false;
        if(networkOn) {
            success = postFile();
        }
        return success;
    }

    @Override
    protected void onPostExecute(Boolean result){
        String s = "";
        if(result){
            s = "Success!";

            // Save a timestamp, this will be displayed to user in the
            // "server information" activity
            // Which is not yet implemented!
            long ts = System.currentTimeMillis();
            SharedPreferences sharedPref = ctx.getSharedPreferences(PREF_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor e = sharedPref.edit();
            e.putLong(Lib.PREF_SERV_TS_KEY, ts);
            e.commit();

            ts = sharedPref.getLong(Lib.PREF_SERV_TS_KEY, -1);

            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss a");
            String dateString = formatter.format(new Date(ts));

            Log.d(TAG, "Success uploading to server at: " + dateString);

        } else {
            s = "Upload Failed!";
        }
        makeToastWithCheck(s);
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

            String response = sb.toString();
            if(response != "") {
                Log.d(TAG, "response: " + response);
            }

            // Disconnect and return result
            httpURLConnection.disconnect();
            Log.d(TAG, "File uploaded successfully!");
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

}
