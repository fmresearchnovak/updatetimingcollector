package edu.fandm.enovak.updatetimingcollector;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;

public class LogView extends AppCompatActivity {

    private TextView mainTV;
    protected Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);

        ctx = this;

    }

    @Override
    protected void onResume(){
        super.onResume();

        mainTV = (TextView)findViewById(R.id.logview_tv_fileview);
        new LogFileLoader().execute();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu m){
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_logview, m);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem menuItem) {
        if (menuItem.getItemId() == R.id.action_refresh) {
            new LogFileLoader().execute();

        } else if (menuItem.getItemId() == R.id.action_upload) {
            FilePOSTer.scheduleUpload(this, true, 1000); // 1 second in ms
        }

        return true;
    }



    private class LogFileLoader extends AsyncTask<Void, Void, String> {

        protected void onPreExecute(){
            mainTV.setText("Loading...");
        }

        protected String doInBackground(Void... params){
            //Log.d(Main.TAG, "Loading file background...");
            File f = Lib.getLogFile(ctx);
            String contents = Lib.readFile(f);

            return contents;
        }

        protected void onPostExecute(String contents) {
            //Log.d(Main.TAG, "Done loading file.  Displaying now!");
            if (contents == null) {
                mainTV.setText("Error reading log file.");
            } else if (contents.equals("")) {
                mainTV.setText("Log File Empty!  Install / update some apps.");
            } else {
                mainTV.setText(contents);
            }
        }
    }







}
