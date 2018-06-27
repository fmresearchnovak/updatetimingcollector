package edu.fandm.enovak.updatetimingcollector;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadPoolExecutor;

import static android.content.pm.PackageManager.MATCH_UNINSTALLED_PACKAGES;


public class ViewChart extends AppCompatActivity {

    private BarChart bc;
    private Context ctx;
    private String names[];
    private String namesLong[];

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

            // In case the file is missing / empty
            if(s == null || s.equals("")){
                return null;
            }


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


            /* This block of code is only necessary to convert the data I have (updateCountDict)
            into the correct format for the stupid BarChart. */

            // Sort the data by the values.
            Set<java.util.Map.Entry<String, Integer>> pairsSet = updateCountDict.entrySet();
            java.util.Map.Entry[] pairs = new java.util.Map.Entry[pairsSet.size()];
            pairsSet.toArray(pairs);
            Arrays.sort(pairs, new Comparator<Map.Entry>() {
                        @Override
                        public int compare(Map.Entry e1, Map.Entry e2) {
                            return Integer.compare((int)e1.getValue(), (int)e2.getValue());
                        }
                    });
            // Now have the data sorted in the array "pairs"
            // Generate the names array and tmp array of values
            // I need to get the names after sorting so that I have them in order!
            // Also I translate them from FQDN / package name is "normal" name
            PackageManager pm = ctx.getPackageManager();
            List<BarEntry> tmp = new ArrayList<BarEntry>(pairs.length);
            names = new String[pairs.length];
            namesLong = new String[pairs.length];
            for (int i = 0; i < pairs.length; i++){
                tmp.add(new BarEntry(i, (int) pairs[i].getValue()));

                // Sometimes the package names have a :UID tacked on to the end
                // e.g., com.google.android.apps.maps:10098   where 10098 is the UID of this app
                // I don't know why, but we need to remove it for the package manager to find
                // the package to get the real name (and icon eventually)
                name = (String) pairs[i].getKey(); // I am re-using this variable from earlier in this method]
                if(name.contains(":")){
                    name = name.split(":")[0];
                }
                namesLong[i] = name;
                try { /// translate from ugly "com.android.google.whatever" to nice "YouTube"
                    ApplicationInfo ai = pm.getApplicationInfo(name, MATCH_UNINSTALLED_PACKAGES);
                    names[i] = (String)pm.getApplicationLabel(ai);
                } catch (PackageManager.NameNotFoundException e1){
                    names[i] = "Unknown Application";
                }
            }

            // But the tmp (raw data values) into BarDataSet to set the color
            BarDataSet bds = new BarDataSet(tmp, "Some Data Label");
            bds.setColor(Color.argb(250, 50, 77, 178));
            BarData bd = new BarData(bds); // for the actual chart
            return bd;

        }

        @Override
        protected void onPostExecute(BarData barData) {
            if(barData == null){
                bc.setNoDataText("Log File Empty!  Install / update some apps first.");
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
                    String n = namesLong[(int)e.getX()];

                    TextView tv = (TextView)findViewById(R.id.view_chart_tv_detail);
                    tv.setText(n + "\n" + (int)e.getY() + " updates");

                    n = names[(int)e.getX()];
                    tv = (TextView) findViewById(R.id.view_chart_tv_main);
                    tv.setText(n);
                }

                @Override
                public void onNothingSelected() {

                }
            });


            // re-draw the chart and animate it!
            // Important to do the animation here because it happens INSTANTLY
            // to it should start when the chart is fully ready to be displayed
            bc.invalidate(); // alters the data on the chart, cause it to be re-rendered
            bc.animateY(1500, Easing.EasingOption.EaseOutBack); // runs an animation

        }
    }
}
