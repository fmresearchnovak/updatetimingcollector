package edu.fandm.enovak.updatetimingcollector;

import android.util.Log;

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

import static edu.fandm.enovak.updatetimingcollector.Main.TAG;

/**
 * Created by enovak on 3/7/18.
 */

public class FilePOSTer {

    public File f;

    private String attachmentName;
    private String attachmentFileName;
    private final String newLine = "\r\n";
    private final String twoHyphens = "--";
    private final String boundary = "-----";

    private final String contents;

    public FilePOSTer(File newF) throws IllegalArgumentException{
        contents = Lib.readFile(newF);
        if(contents == null){
            throw new IllegalArgumentException("Unable to read file");
        }

        attachmentFileName = newF.getName();
        attachmentName = newF.getName();
    }

    // Asynchronous call that does the actual network communication
    public void post(){
        Thread t = new Thread(new FilePOSTer.filePOSTerRunnable());
        t.start();
    }



    private class filePOSTerRunnable implements Runnable {
        @Override
        public void run() {
            Log.d(TAG, "Posting File Now!");
            postFile();
        }

        private void postFile() {
            // This code heavily inspired by: https://stackoverflow.com/questions/11766878/sending-files-using-post-with-httpurlconnection
            HttpURLConnection httpURLConnection = null;
            URL url = null;
            try {

                // Setup request
                url = new URL("http://155.68.60.102:9000");
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
                Log.d(TAG, "response: " + response);

                httpURLConnection.disconnect();

            } catch (MalformedURLException e1){
                e1.printStackTrace();
                return;
            } catch (IOException e2){
                e2.printStackTrace();
                return;
            }

        }
    }


}
