package edu.fandm.enovak.updatetimingcollector;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.File;

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

        } else if (menuItem.getItemId() == R.id.action_upload) {
            FilePOSTer.scheduleUpload(this, true, 2000); // 2 seconds in ms
        }

        return true;
    }


    private void loadAndDisplayFile(){
        File f = Lib.getLogFile(this);
        String contents = Lib.readFile(f);
        if(contents == null){
            mainTV.setText("Error reading log file.");
        } else if(contents == ""){
            mainTV.setText("Log File Empty!  Install / update some apps.");
        } else {
            mainTV.setText(contents);
        }
    }

}
