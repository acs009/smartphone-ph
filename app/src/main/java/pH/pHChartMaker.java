package pH;

import android.content.Intent;
import android.graphics.Color;

import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.text.DecimalFormat;
import java.util.ArrayList;

/**
 * Created by Tom Phelps on 9/9/2015.
 */
public class pHChartMaker {
    private LineChart lineChart;
    String filename;
    double calSlope;
    double calIntercept;
    double tempCal;
    private float lastVal;
    ArrayList<Entry> yVals = new ArrayList<Entry>();
    ArrayList<String> xVals = new ArrayList<String>();
    public pHChartMaker(String filename){
        this.filename=filename;
        getHistory();
    }
    public LineChart getLineChart(LineChart lineChart){
        this.lineChart = lineChart;
        prepData();
        setUpChart();
        return this.lineChart;
    }
    private void prepData() {
        System.out.println("chart maker started: "+xVals.size() + ", "+yVals.size());

        LineDataSet set1 = new LineDataSet(yVals, "pH Values");
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
        lineChart.animateX(2500, Easing.EasingOption.EaseInOutQuart);
//        lineChart.invalidate();
    }
    boolean valuesSet = false;
    private void getHistory(){
        int ph1,ph2,temp1,temp2,i=0;
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String s=reader.readLine();
            while ((s=reader.readLine())!=null){
                String[] data = s.split("[,]+");
                if(data.length==1){
                    continue;
                }
                System.out.println("STRING"+s);
                if(!valuesSet){
                    valuesSet=true;
                    System.out.println("serial:" +data[data.length-5]);
                    setCalValues(Integer.parseInt(data[data.length - 5]));
                }
                System.out.println(s);
                ph1=Integer.parseInt(data[data.length - 3]);
                ph2=Integer.parseInt(data[data.length-4]);
                temp1=Integer.parseInt(data[data.length - 1]);
                temp2=Integer.parseInt(data[data.length - 2]);
                int temp= (temp1+temp2)/2;
                int phAvg=(ph1+ph2)/2;
                float tempCorrect = (float)(calSlope/tempCal*(temp-tempCal));
                System.out.println(tempCorrect);
                lastVal = (float)convertToPh(phAvg);
                yVals.add(new Entry(lastVal,i));
                xVals.add((i++)+"");
            }
        }catch (Exception e){
            e.printStackTrace();
            System.out.println("Data fetch failed");
        }
    }
    public double convertToPh(int val){
        return (-0.056668*val+14.3102);
    }
    public String getPhVal(){
        DecimalFormat df = new DecimalFormat("##0.0");
        return df.format((double)lastVal)+"";
    }
    private void setCalValues(int serial){
        switch (serial){
            case 1:
                calIntercept=10.68452708;
                tempCal = 108;
                calSlope=-0.027772817;
                break;
            case 2:
                calIntercept=10.43949849;
                tempCal = 102;
                calSlope=-0.027515988;
                break;
            case 3:
                calIntercept=10.90596276;
                tempCal = 102;
                calSlope=-0.027899734;
                break;
            default:
                break;
        }

    }
}
