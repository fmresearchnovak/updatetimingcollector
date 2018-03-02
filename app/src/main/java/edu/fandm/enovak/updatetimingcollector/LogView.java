package edu.fandm.enovak.updatetimingcollector;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogView extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log_view);

    }

    @Override
    protected void onResume(){
        super.onResume();
        String contents = readLogFile();
        TextView v = (TextView)findViewById(R.id.logview_tv_fileview);
        v.setText(contents);
    }


    // Wow what a pain this method is!
    // Reading files in Java is too complicated (there are so many choices!)
    // and some choices (like the one I made below) are too complicated
    private String readLogFile(){
        File f = Lib.getLogFile();
        if(f.exists() && f.canRead()){

            StringBuilder sb = new StringBuilder();
            byte[] buffer = new byte[1024];
            FileInputStream fis = null;

            try{
                fis = new FileInputStream(f);
                int n;
                while( (n = fis.read(buffer)) != -1) {
                    sb.append(new String(buffer));
                }
                return sb.toString();

            } catch (FileNotFoundException e1){
                Toast.makeText(this, "No log (file not found)", Toast.LENGTH_SHORT).show();
            } catch (IOException e2){
                Toast.makeText(this, "Error reading log", Toast.LENGTH_SHORT).show();
            }
            return "Error";

        } else {
            Toast.makeText(this, "Log missing or cannot be read for permissions reasons!", Toast.LENGTH_SHORT).show();
            return "Error";
        }
    }

}
