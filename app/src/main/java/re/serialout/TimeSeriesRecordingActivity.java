package re.serialout;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Timer;

public class TimeSeriesRecordingActivity extends ActionBarActivity {
    private long rate;
    private long duration;
    private PrintWriter printOut;
    private File dataFile;
    private boolean phMeasure;
    private boolean glassProbe;
    private boolean tempMeasure;
    final int TEMP = 10;
    final int PH = 11;
    private Calendar cal = new GregorianCalendar();
    private TextView countDownText;
    LineChart lineChart;
    ArrayList<Entry> yVals = new ArrayList<Entry>();
    ArrayList<String> xVals = new ArrayList<String>();
    CountDownTimer time;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_series_recording);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        rate=getIntent().getIntExtra("rate", 1000);
        duration=getIntent().getIntExtra("duration", 60000);
        phMeasure=getIntent().getBooleanExtra("ph", false);
        tempMeasure=getIntent().getBooleanExtra("temp",false);
        glassProbe=getIntent().getBooleanExtra("glassProbe",true);
        String fileName = "data_";
        File folder = new File(Environment.getExternalStorageDirectory() + "/Time_Series");
        if(!folder.exists()) {
            folder.mkdir();
        }
        lineChart=(LineChart)findViewById(R.id.timeRec_chart);
        countDownText=(TextView)findViewById(R.id.timeRes_countDownText);
        dataFile = new File(folder.getPath(), fileName+folder.listFiles().length+".csv");
        startSampling();

    }
    private void updateChart(){
        prepData();
        setUpChart();
        lineChart.invalidate();
    }
    public void timeRecCancel(View view){
        time.cancel();
        finish();
    }
    public double convertToVolts(int val){
        double result;
        double vcm;
        if(glassProbe){
            vcm=1.524;
        }
        else{
            vcm=0.345;
        }
        result=(3.922*val/255.0+3.3*vcm)/4.3-vcm;
        return result;
    }
    public double convertToPh(int val){
        return (-0.056668*val+14.3102);
    }

    private void startSampling(){
        time =new CountDownTimer(duration,rate){
            int phResult=1;
            int i = 0;
            String output = "";
            int tempResult=2;
            public void onTick(long millsTillFinished){
                countDownText.setText(millsTillFinished/1000+"");
                cal=new GregorianCalendar();
                //Take sample
                output+= cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DATE) + "/" + cal.get(Calendar.YEAR) + ",";
                output+=cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ",";
                try {
                    if (phMeasure) {
                        phResult = sendCommand(PH)[0];
                        output += phResult;
                        float volts = (float)convertToPh(phResult);
                        output+=","+volts;
                        yVals.add(new Entry(phResult, i));
                    }
                    if (tempMeasure) {
                        tempResult = sendCommand(TEMP)[0];
                        if (!phMeasure) {
                            yVals.add(new Entry(tempResult, i));
                        }
                        output += "," + tempResult;
                    }
                }
                catch (Exception e){
                    System.out.println("Failed a measurement");
                }
                output+="\n";
                xVals.add((i++)+"");
                updateChart();
            }
            public void onFinish(){
                cal=new GregorianCalendar();
                //Take sample
                output+= cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DATE) + "/" + cal.get(Calendar.YEAR) + ",";
                output+=cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ",";
                try {
                    if (phMeasure) {
                        phResult = sendCommand(PH)[0];
                        float volts = (float)convertToPh(phResult);
                        output+=","+phResult;
                        output+=","+volts;
                        yVals.add(new Entry(phResult, i));
                    }
                    if (tempMeasure) {
                        tempResult = sendCommand(TEMP)[0];
                        if (!phMeasure) {
                            yVals.add(new Entry(tempResult, i));
                        }
                        output += "," + tempResult;
                    }
                }
                catch (Exception e){
                    System.out.println("Failed a measurement");
                }
                output+="\n";
                xVals.add((i++)+"");
                updateChart();
                countDownText.setText("Done!");
                try {
                    printOut = new PrintWriter(new FileWriter(dataFile));
                    printOut.println(output);
                    printOut.flush();
                    printOut.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(dataFile));
                CommandInterface.staticAudio.context.sendBroadcast(intent);
            }
        }.start();
    }

    public int[] sendCommand(int cmd) {
        int tryCount = 0;
        int[] out = {170, cmd, 0, cmd, 238};
        while (tryCount < 2) {
            AudioSerialOutMono.outputFSK(out);
            CommandInterface.getResponse(1.5,true);
            if (CommandInterface.staticAudio.inputDecoder.parseCommmand()) {
                int[] data = CommandInterface.staticAudio.inputDecoder.data;
                System.out.println("Printing everything received in data:");
                return data;
            }
            tryCount++;
        }
        return null;
    }
    private void prepData() {
        System.out.println("chart maker started: "+xVals.size() + ", "+yVals.size());

        LineDataSet set1 = new LineDataSet(yVals, "ppg");
        set1.disableDashedLine();
        set1.setColor(Color.parseColor("#ff0091ff"));
        set1.setCircleColor(Color.parseColor("#ff0091ff"));
        set1.setLineWidth(2f);
        set1.setCircleSize(5f);
        set1.setDrawCircleHole(true);
        set1.setValueTextSize(0);
        set1.setFillAlpha(100);
        set1.setFillColor(Color.parseColor("#ff0091ff"));
        set1.setDrawCubic(false);
        set1.setDrawFilled(true);
//         set1.setShader(new LinearGradient(0, 0, 0, lineChart.getHeight(),
//         Color.BLACK, Color.WHITE, Shader.TileMode.MIRROR));

        ArrayList<LineDataSet> dataSets = new ArrayList<LineDataSet>();
        dataSets.add(set1); // add the datasets

        // create a data object with the datasets
        LineData data = new LineData(xVals, dataSets);

        // set data
        lineChart.setData(data);
    }

    private void setUpChart(){
        lineChart.setDrawGridBackground(false);
        // no description text
        lineChart.setDescription("");
        lineChart.setNoDataTextDescription("You need to provide data for the chart.");

        // enable value highlighting
//        lineChart.setHighlightEnabled(true);

        // enable touch gestures
        lineChart.setTouchEnabled(true);

        // enable scaling and dragging
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(true);
        // lineChart.setScaleXEnabled(true);
        // lineChart.setScaleYEnabled(true);

        // if disabled, scaling can be done on x- and y-axis separately
        lineChart.setPinchZoom(true);
        lineChart.setVisibleXRangeMaximum(xVals.size());

        // set an alternative background color

        // create a custom MarkerView (extend MarkerView) and specify the layout
        // to use for it

        // set the marker to the chart

        // x-axis limit line

        XAxis xAxis = lineChart.getXAxis();
        xAxis.disableGridDashedLine();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextSize(10f);
        xAxis.setDrawGridLines(false);
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setStartAtZero(true);
        //leftAxis.setYOffset(20f);
        leftAxis.disableGridDashedLine();

        // limit lines are drawn behind data (and not on top)
        leftAxis.setDrawLimitLinesBehindData(true);
        lineChart.getAxisRight().setEnabled(false);
//        lineChart.setVisibleXRange(20);
//        lineChart.setVisibleYRange(20f, AxisDependency.LEFT);
//        lineChart.centerViewTo(20, 50, AxisDependency.LEFT);
//        lineChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);
//        lineChart.invalidate();
    }

}
