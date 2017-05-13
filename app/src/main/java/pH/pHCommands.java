package pH;

import android.content.Intent;
import android.net.Uri;
import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;

import re.serialout.AudioSerialOutMono;
import re.serialout.CommandInterface;
import re.serialout.InputDecoder;

/**
 * Created by Tom Phelps on 4/28/2015.
 */

public  class pHCommands {
    private boolean debug = false;
    private File dataFile;
    private PrintWriter printOut;
    private String filename;
    final int ID = 15;
    final int SERIAL = 13;
    final int TEMP = 10;
    final int PH = 11;
    private boolean inUse = false;
    public void createFile(String patientID){
        String fileName = "pid_";
        File folder = new File(Environment.getExternalStorageDirectory() + "/pH_data");
        if(!folder.exists()) {
            folder.mkdir();
        }
        dataFile = new File(folder.getPath(), fileName+patientID+".csv");
        try {
            //HEADER GOES HERE
            if(!dataFile.exists()){
                printOut = new PrintWriter(new FileWriter(dataFile));
                printOut.print("DATA FORMAT: QuizResults,Date, Time, ID, Serial Number, pH1,pH2, temp1, temp2\r\n");
                printOut.flush();
                printOut.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        filename = dataFile.toString();
    }
    public String getFilename(){
        System.out.println(filename);
        return filename;
    }

    public void openFile(){
        try {
            printOut = new PrintWriter(new FileWriter(dataFile,true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void phTest() {
        //For testing purposes, not used in final application
        int size = 50;
        int phVals[] = new int[size];
        for (int i = 0; i < size; i++) {
            System.out.println("==SAMPLE #: " + i + "==");
            int[] store = sendCommand(PH);
            try {
                phVals[i] = store[0];
            } catch (Exception e) {
                store = sendCommand(PH);
            }
        }
        int max = 0;
        int min = 100000;
        double mean = 0;
        double stdev = 0;
        for (int val : phVals) {
            if (val < 10) {
                size = size - 1;
                continue;
            }
            max = max > val ? max : val;
            min = (min < val) ? min : val;
            mean += val;
        }
        mean = mean / size;
        for (int val : phVals) {
            if (val < 10) {
                continue;
            }
            stdev = stdev + (val - mean) * (val - mean);
        }
        stdev = Math.sqrt(stdev / size);

        System.out.println("===Results===");
        System.out.println("Mean: " + mean);
        System.out.println("Min: " + min + "\nMax: " + max);
        System.out.println("Standard Dev: " + stdev);
    }
    public PrintWriter getPrintOut(){
        return printOut;
    }
    public boolean recordSamples(){
        openFile();
        if(printOut==null)
        System.out.println("PRINTOUT NULL");
        Calendar cal = new GregorianCalendar();
        printOut.print(","+cal.get(Calendar.MONTH) + "/" + cal.get(Calendar.DATE) + "/" + cal.get(Calendar.YEAR) + ",");
        printOut.print(cal.get(Calendar.HOUR) + ":" + cal.get(Calendar.MINUTE) + ":" + cal.get(Calendar.SECOND) + ",");
        String output="";
        try {
        int[] store = sendCommand(ID);
        if (store == null) {
            return false;
        }
        output += store[0] + ",";
//        store = sendCommand(SERIAL);
        if(store==null){
            return false;
        }
        output+=store[0]+",";
//        InputDecoder.textView.setText("ID:10\n Serial Number:" + store[0]);
//            PH FETCH
            store = sendCommand(PH);
            if (store == null) {
                return false;
            }
            output += store[0] + ",";

//            InputDecoder.textView.setText("\n Ph measurement 1:" + store[0]);

//            store = sendCommand(PH);
            if (store == null) {
                return false;
            }
            output += store[0] + ",";
            InputDecoder.textView.append("\n Ph measurement 2:" + store[0]);

//            store = sendCommand(TEMP);
            if (store == null) {
                return false;
            }
            output += store[0] + ",";
            InputDecoder.textView.append("\n Temp measurement 1:" + store[0]);

//            store = sendCommand(TEMP);
            if (store == null) {
                return false;
            }
            output += store[0] + "\r\n";
            InputDecoder.textView.append("\n Temp measurement 2:" + store[0]);
            printOut.write(output + "\n");
            donePrinting();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public void donePrinting(){
        if(printOut!=null) {
            printOut.flush();
            printOut.close();
        }
//        try {
//            BufferedReader reader = new BufferedReader(new FileReader(filename));
//            String s;
//            while((s=reader.readLine())!=null){
//                System.out.println(s);
//            }
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(dataFile));
        CommandInterface.staticAudio.context.sendBroadcast(intent);
    }

    public int[] sendCommand(int cmd) {
        if(debug){
            CommandInterface.staticAudio.recordData();
        }
        int tryCount = 0;
        if(inUse){
            return null;
        }
        inUse=true;
        int[] out = {170, cmd, 0, cmd, 238};
        while (tryCount < 2) {
            AudioSerialOutMono.outputFSK(out);
            CommandInterface.getResponse(1.5,true);
            if (CommandInterface.staticAudio.inputDecoder.parseCommmand()) {
                int[] data = CommandInterface.staticAudio.inputDecoder.data;
                System.out.println("Printing everything received in data:");
                for (int part : data) {
                    System.out.println(part);
                }
                inUse = false;
                return data;
            }
            tryCount++;
        }
        inUse=false;
        if (debug) {
            CommandInterface.staticAudio.stopData();
        }
        return null;
    }

}
