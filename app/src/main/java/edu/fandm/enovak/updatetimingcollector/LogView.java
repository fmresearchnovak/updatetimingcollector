package edu.fandm.enovak.updatetimingcollector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogView extends AppCompatActivity {

    private TextView mainTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);

    }

    @Override
    protected void onResume(){
        super.onResume();

        mainTV = (TextView)findViewById(R.id.logview_tv_fileview);

        loadAndDisplayFile();
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
            loadAndDisplayFile();
            Log.d(Main.TAG, "Reloading...");
        } else if (menuItem.getItemId() == R.id.action_upload) {
            uploadFile();
            Log.d(Main.TAG, "Uploading...");
            Toast.makeText(this, "Uploading...", Toast.LENGTH_SHORT).show();
        }


        return true;
    }


    private void uploadFile(){
        File f = Lib.getLogFile(this);
        FilePOSTer fp = new FilePOSTer(f);
        fp.post();
    }


    private void loadAndDisplayFile(){
        File f = Lib.getLogFile(this);
        String contents = Lib.readFile(f);
        if(contents != null) {
            mainTV.setText("loading...");

            // Put in a phony delay for user satisfaction
            try{
                Thread.currentThread().sleep(1000); // one second
            } catch (InterruptedException e1) {

            }

            mainTV.setText(contents);
        } else {
            mainTV.setText("Error reading log file!");
        }


    }

}
