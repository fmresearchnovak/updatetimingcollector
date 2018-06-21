package edu.fandm.enovak.updatetimingcollector;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.github.mikephil.charting.utils.MPPointD;
import com.github.mikephil.charting.utils.MPPointF;

import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


public class ViewChart extends AppCompatActivity {

    private BarChart bc;
    private Context ctx;
    private String names[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_chart);
        ctx = this;

        bc = (BarChart)findViewById(R.id.view_chart_barchart);

        // The legend displays some text displayed below the chart
        // Because of the way may data is, there is only one "set" so the legend
        // only has one item in it which is pointless)
        bc.getLegend().setEnabled(false);

        // The X-axis is the names of the apps, by default it shows at the top
        // disable it for now to look better.  I plan on implementing "click to see"
        // style so the user can click on a bar to see what app that is
        bc.getXAxis().setEnabled(false);

        // The description is text that is overlayed on top of the chart
        // by default it appears in the bottom right corner.  I am turning it off
        // because it is difficult to place elsewhere (using absolute pixel values on screen)
        // and it's small
        bc.getDescription().setEnabled(false);

        // By default the chart draws the values in little text right
        // above each bar unnecessary and (I think) ugly.  This
        // only draws them on the bar (instead of just above the bar)
        // I think it looks slightly better!
        bc.setDrawValueAboveBar(false);


        bc.animateY(3000, Easing.EasingOption.EaseOutBack);

    }


    @Override
    public void onResume(){
        super.onResume();

        new ChartLoader().execute();

    }


    private class ChartLoader extends AsyncTask<Void, Void, BarData>{

        private long earliestTS = 0;
        protected void onPreExecute(){
            bc.setNoDataText("Loading...");
        }

        protected BarData doInBackground(Void... params){

            File log = Lib.getLogFile(ctx);
            String s = Lib.readFile(log);


            String[] strList = s.split("\n");


            // The initial capacity (length / 2) is a VERY loose approximation.
            // The user probably will NOT have every entry as a unique app.
            HashMap<String, Integer> updateCountDict = new HashMap<>(strList.length/2);

            String[] parts = new String[5];
            String name;
            Integer val;

            for(String line : strList){
                parts = line.split(",");

                if(parts[1].equals("android.intent.action.PACKAGE_REPLACED")) {

                    // Find the TS of the earliest "PACKAGE_REPLACED
                    // event.  This gives a lower-bound on the time-frame of
                    // this log / set of updates
                    if(earliestTS == 0){
                        earliestTS = Long.valueOf(parts[0]);
                    }


                    name = parts[3];

                    if(updateCountDict.containsKey(name)){
                        val = updateCountDict.get(name);
                        updateCountDict.put(name, val+1);
                    } else {
                        updateCountDict.put(name, 1);
                    }
                }

            }

            // This means there are no "update" events in the log
            // The safest thing to do I think is just stop (to avoid
            // triggering a stupid bug in the rest of this method)
            if(updateCountDict.size() == 0){
                return null;
            }

            names = new String[updateCountDict.size()];
            List<BarEntry> blah = new ArrayList<>(updateCountDict.size());

            Iterator it = updateCountDict.entrySet().iterator();
            int idx = 0;
            while(it.hasNext()) {
                Map.Entry entryPair = (Map.Entry) it.next();
                names[idx] = (String)entryPair.getKey();
                blah.add(new BarEntry(idx++, (int) entryPair.getValue()));
            }

            BarDataSet bds = new BarDataSet(blah, "Some Data Label");
            bds.setColor(Color.argb(250, 50, 77, 178));
            BarData bd = new BarData(bds);
            return bd;

        }

        @Override
        protected void onPostExecute(BarData barData) {
            if(barData == null){
                return;
            }

            // Update title
            SimpleDateFormat formatter = new SimpleDateFormat("MM/dd/yyyy   h:mm:ss a");
            String dateString = formatter.format(new Date(earliestTS));
            TextView tv = (TextView)findViewById(R.id.view_chart_tv_title);
            tv.setText("Number of Updates Since\n" + dateString);

            // Update chart
            bc.setData(barData);
            bc.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
                @Override
                public void onValueSelected(Entry e, Highlight h) {
                    String n = names[(int)e.getX()];
                    TextView tv = (TextView)findViewById(R.id.view_chart_tv_detail);
                    tv.setText(n + "\n" + (int)e.getY() + " updates");
                }

                @Override
                public void onNothingSelected() {

                }
            });


            // re-draw the chart
            bc.invalidate();

        }
    }
}
