package re.serialout;

import android.content.Context;
import android.widget.TextView;

/**
 * Created by Tom Phelps on 2/22/2015.
 */
//This class is designed to make it easy to send and receive commands.
//It should send a command and then wait for a limited amount of time for a response.


public class CommandInterface {
    int buadRate = 2400;
    static boolean isTesting = false;
    private static int failed = 0;
    static int x1, x2;
    int i = 0;
    public boolean setUp = false;
    //The audio manager controls the mic port and comes with an InputDecoded built into it.
    //This will allow this class to get all the information it needs.
    public static MicManager staticAudio;

    public CommandInterface(int buadRate, Context context, TextView phOutput) {
        this.buadRate = buadRate;
        staticAudio = new MicManager(buadRate, context, phOutput);
    }

    public void releaseAll() {
        staticAudio.releaseAll();
    }

    public static String queryID(final int timeOut) {
        //Sends an int array containing the bytes for the command get query,
        //and waits for response for xxx amount of seconds. Variable timeOut should have units
        //of seconds.
        //int[] output = {170,254,2,0,1,1,238};
        int[] output = {170, 15, 0, 15, 238};
        AudioSerialOutMono.outputFSK(output);
        getResponse(2,true);
        System.out.println("Sent Command");
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                getResponse(timeOut);
//            }
//        }).start();

        if (staticAudio.inputDecoder.parseCommmand() && staticAudio.inputDecoder.len > 0) {
            return "" + staticAudio.inputDecoder.data[0];
        } else return null;

    }

    public void startUpLoop() {
        int winCount = 0;
        int tryCount = 0;
        while (winCount < 2&&tryCount<6) {
            tryCount++;
            int[] output = {170, 15, 0, 15, 238};
            AudioSerialOutMono.outputFSK(output);
            getResponse(1,true);
            if (staticAudio.inputDecoder.parseCommmand()) {
                winCount++;
                System.out.println("Success! " + winCount);
            } else {
                winCount = 0;
            }
        }
        setUp = true;
        System.out.println("Set-up complete.");
    }
    public void testPacket() {
        //===TEST FUNCTION===
        //This part is used to send a packet one time for testing.

        final int[] output = {170, 13, 0, 13, 238};
//        final int[] output = {10,15,24,26,44,52,55,63,72,74,79,91,93};
//        final int[] output={1,5,6,253,255,0};
//        final int[] output = {170, 73, 1, 1, ( 2 +73) % 256, 238};

        //int[] send = {output[i++]};
//        int[] send = {170};
        AudioSerialOutMono.outputFSK(output);
        getResponse(2,true);
        i = i % output.length;
        if (true || !staticAudio.inputDecoder.parseCommmand()) {
            staticAudio.printFailed();
        }

        //==THIS PART IS TO SEND A PACKET OVER AND OVER==

//        run = !run;
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (run) {
//                    int[] output={170};
//                    AudioSerialOutMono.outputFSK(output);
//                    try {
//                        Thread.sleep(500, 0);
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }).start();
    }
    public void sendCommand(int[] command, int timeOut) {
        //General fucntion to be used to sensor activites to send commands.
        AudioSerialOutMono.outputFSK(command);
        getResponse(timeOut,true);
        return;
    }

    public void runTest() {
        //The code for the stress test, can be run for any values of X1 and X1, automaticaly computs check sum and counts failures.
        //Will print failed waveforms to file.
        //See botton of getResponse for the packet check and print functions.
        x2 = 0;
        x1 = 0;
        failed = 0;
        isTesting = true;
        for (x1 = 20; x1 < 120; x1++) {
            System.out.println("Running:" + x1);
            //int[] output = {x1};
            int[] output = {170, x1, 1, 1, (1 + 1 + x1) % 256, 238};
            AudioSerialOutMono.outputFSK(output);
            getResponse(1,true);
            try {
                Thread.sleep(200, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Failed " + failed + " out of " + (x1 - 20) + "!===");
        isTesting = false;
    }

    public static void getResponse(double timeOut,boolean fsk) {
        /*
        This starts and stops recording for the given time period.
         */
        int timeWaited = 0;
        staticAudio.startRecording(fsk);
        while (staticAudio.isRecording) {
            try {
                Thread.sleep(10, 0);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            timeWaited += 10;
            if (timeWaited > timeOut * 1000) {
                System.out.println("Timed Out");
                staticAudio.stopRecording();
                break;
            }
        }
        //Code for the stress test
        if (isTesting) {

            if (staticAudio.inputDecoder.parseCommmand()) {
                System.out.println("Passed parse command");
                if (!(staticAudio.inputDecoder.data[0] == x1)) {
                    failed += 1;
                    System.out.println("====Failed at " + x1+"====");
                    staticAudio.printWave();
                }
            } else {
                failed += 1;
                System.out.println("====Failed at " + x1 + "====");
                staticAudio.printWave();

            }

        }
    }
}
