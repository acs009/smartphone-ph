package potStat;

import android.widget.Toast;

import re.serialout.AudioSerialOutMono;
import re.serialout.CommandInterface;

/**
 * Created by Tom Phelps on 7/29/2015.
 */
public class PotStatCommands {
    int testLength = 0;
    boolean inUse=false;

    public boolean setTest(int test, int rate) {
        int[] output = {170, 254, 2, test, rate, (test + rate + 2 + 254) % 256, 238};
        AudioSerialOutMono.output(output);
        testLength = getTestLength(test, rate);
        CommandInterface.getResponse(2,false);
        if (CommandInterface.staticAudio.inputDecoder.parseCommmand()) {
            boolean response = CommandInterface.staticAudio.inputDecoder.data[0] == test + 10 && CommandInterface.staticAudio.inputDecoder.data[1] == rate;
            return response;
        } else
            return false;
    }

    public int getTestLength(int test, int len) {
        int[][] lengths = {{125, 65, 35}, {125, 65, 35}, {173, 89, 47}, {125, 65, 35}};
        return lengths[test - 1][len - 1];
    }

    public boolean readyCheck() {
        try {
            int tryCount = 0;
            while (tryCount < 3) {
                sendCommand(13);
                if (CommandInterface.staticAudio.inputDecoder.data[0] == 1)
                    return true;
                tryCount++;
            }
            return false;
        }
        catch (Exception e){
            return false;
        }
    }

    public void recordRawData() {
        //DONT FORGET TO CHANGE RESPONSE TIME BACK TO TEST LENGTH
        CommandInterface.staticAudio.recordData();
        System.out.println("Recording started, length: " + testLength);
        CommandInterface.getResponse(testLength, false);
        System.out.println("Data Colletion has Stopped");
        CommandInterface.staticAudio.stopData();
    }
    public int[] sendCommand(int cmd) {
        int tryCount = 0;
        if(inUse){
            return null;
        }
        inUse=true;
        int[] out = {170, cmd, 0, cmd, 238};
        while (tryCount < 1) {
            AudioSerialOutMono.output(out);
            CommandInterface.getResponse(1.5, false);
//            recordRawData();
            if (CommandInterface.staticAudio.inputDecoder.parseCommmand()) {
                int[] data = CommandInterface.staticAudio.inputDecoder.data;
                System.out.println("Printing everything received in data:");
                for (int part : data) {
                    System.out.println(part);
                }
                inUse = false;
                return data;
            }
//            CommandInterface.staticAudio.printFailed();
            tryCount++;
        }
        inUse=false;
        return null;
    }

}
