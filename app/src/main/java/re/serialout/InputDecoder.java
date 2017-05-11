package re.serialout;

import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Tom Phelps on 2/14/2015.
 */
public class InputDecoder {

    //This class handles turning the raw audio input into usable bytes and then getting the command
    //and data from those bytes.

    private float sampleRate;
    private int baudRate;
    private int stepSize = 18;
    private ArrayList<Double> last10 = new ArrayList();
    private ArrayList<Integer> bits = new ArrayList();
    public ArrayList<Integer> bytes = new ArrayList();
    public boolean startBit = false;
    private boolean currentBit = false;
    private int j = 0;
    private int bitCount = 0;
    private int nextChar = 0;
    public int currentValue = 0;
    private int twos = 1;
    private double lastValue = 0;
    private int highBit = -1;

    private double lastHigh = 0;
    private boolean inverted = false;
    private boolean invertNextChange = false;
    private double largeDiff = 10;
    private double prevDiff = 10;

    //These are for FSK decoding
    private int zeroCross = 0;
    private double valSum;
    private int valCount;
    private ArrayList<Double> last5 = new ArrayList<>();
    private boolean zeroPending = false;

    public int command;
    public int len;
    public int[] data;
    public static TextView textView;
    private String debugOut = "";

    public boolean screenWrite = false;

    public InputDecoder(float sampleRate, int baudRate, TextView textView) {

        this.sampleRate = sampleRate;
        this.baudRate = baudRate;
        stepSize = (int) sampleRate / baudRate;
        System.out.println("INPUT DECODER STARTED, step size" + stepSize);
        this.textView=textView;
    }

    public void clearVars() {
        last10.clear();
        bits.clear();
        bytes.clear();
        startBit = false;
        currentBit = false;
        highBit = -1;
        debugOut = "";
    }

    private int attempts = 0;

    public boolean parseCommmand() {
        int crc = 0;
        //This turns the array of bits collected from audio into useable info, gets the command and any data,
        //and checks for a valid packet
        try {
            if (bytes.size() < 4) {
                throw new Exception();
            }
            int i = 0;
            while (bytes.get(i) != 170) {
                i++;
                if (bytes.size() == i) {
                    throw new Exception();
                }
            }
            if (!(bytes.get(i++) == 170)) {
                throw new Exception();
            }
            crc += (command = bytes.get(i++));
            crc += (len = bytes.get(i++));
            data = new int[len];
            for (int j = 0; j < len; j++) {
                crc = crc + bytes.get(i);
                data[j] = bytes.get(i++);
            }
            if (!(bytes.get(i++) == (crc % 256))) {
                throw new Exception();
            }
            if (bytes.get(i++) != 238) {
                throw new Exception();
            } else
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    //Gets the input one voltage value at a time.
    public void toggleScreen(boolean bool) {
        screenWrite = bool;
    }

    public void refreshText() {
        if(textView!=null) {
            textView.setText("timed out\n" + debugOut);
            System.out.println("refreshed text");
        }
        debugOut = "";
    }

    double prevVal = 0;

    private boolean crossed(double value, double val) {
        boolean result;
        double signValue = value / val;
        if (prevVal * signValue < 0 && val > 0.1 || (prevVal * signValue > 0 && Math.abs(prevVal) < 0.1 && val > 0.1))
            result = true;
        else
            result = false;
        prevVal = value;
        return result;
    }
    public void parseInputFSK(double value) {
        double val = Math.abs(value);
        largeDiff = val > largeDiff ? val : largeDiff;
        if (crossed(value, val)) {
            zeroCross++;
            if (!startBit) {
                if (j > 12 && j < 26 && Math.abs(largeDiff) > 0.2) {
                    bitCount = 1;
                    zeroCross = 1;
                    currentValue = 0;
                    startBit = true;
                }
                largeDiff = 0;
                j = 0;
            }

        }
        if (startBit) {
            //Check to see if we are near the end of a window. 5 is there as a little bit of lee-way.
            if ((j - 145) > 0) {
                //if we are, use the number of times zero was crossed to decide if it was a one or a zero.
                int bit = zeroCross < 13 ? 0 : 1;
                if (bitCount == 1 && (bit == 1 || zeroCross < 6)) {
                    //Check the startbit! make sure its a zero.
                    bitCount = 0;
                    startBit = false;
                    largeDiff = 0;
                } else {

                    System.out.print(bit);
                    debugOut += bit + "";

                    if (bitCount > 1 && bitCount < 10) {
                        currentValue += twos * (bit == 1 ? 1 : 0);
                        twos *= 2;
                    }
                    bits.add(bit);
                    zeroCross = 0;
                    bitCount++;
                    j = 0;
                }

            }
            if (bitCount == 11) {
                //This is the end of the byte, reset everything and get ready for the next byte.
                System.out.print(" 0x" + Integer.toHexString(currentValue) + " " + currentValue + "\n");
                debugOut += " 0x" + Integer.toHexString(currentValue) + " " + currentValue + "\n";
                startBit = false;
                bytes.add(currentValue);
                twos = 1;
                zeroCross = 0;
                bitCount = 0;
                j = 0;
                largeDiff = 0;
            }
        }
        last5.add(value);
        if (last5.size() > 5)
            last5.remove(0);
        j++;
        lastValue = value;
    }
    double peak = -1;

    public void parseInput(double value) {
        //This parse is specific to the Note2 (possibly only the one in the lab)
        //RS232 PARSER! not in use but left in case we switch back.
        boolean printed = false;
        last10.add(value);
        if (last10.size() > 3) {
            double diff = last10.get(last10.size() - 1)
                    - last10.get(last10.size() - 3);
            if(Math.abs(diff)>0.3){
                if(peak/value<=0){{
                    peak=value;
                }
                }
            }
            if (startBit || bitCount > 0) {
                if (startBit) {
                    startBit = false;
                    lastValue = value;
                    bits.add(0);
                    System.out.print(0);
                    debugOut += 0 + "";
                    bitCount++;
                    nextChar = stepSize * 1+5 ;
                    // values.add(value);
                    printed = true;
                } else if (nextChar == j) {
                    // values.add(value);

                    printed = true;
                    if (peak < 0) {
                        currentBit = false;
                    }
                    if (peak > 0) {
                        currentBit = true;
                    }
                    // lastHigh=value>0?value:lastHigh;
                    // lastValue = value;
                    bits.add(currentBit ? 1 : 0);
                    debugOut= debugOut+(currentBit?1:0);
//					System.out.println("!!!"+(currentBit ? 1 : 0)+" "+peak);
                    System.out.print(currentBit ? 1 : 0);
                    nextChar += stepSize;
                    bitCount++;
                    // Translate to number Value
                    if (bitCount != 10) {
                        currentValue += twos * (currentBit ? 1 : 0);
                        twos *= 2;
                    }
                    if (bitCount == 10) {
                        bitCount = 0;
                        j = 0;
                        twos = 1;
                        currentBit = false;
                        bytes.add(currentValue);
                        debugOut= debugOut+" "+currentValue+"\n";
                        System.out.println(" " + currentValue);
                        currentValue = 0;
                    }
                }
                j++;
            } else if (Math.abs(diff) > 0.3 && diff < 0&&value<-0.1) {
                startBit = true;
            }
            if (last10.size() > 9) {
                last10.remove((int) 0);
            }
            if (!printed) {
                // values.add(0.0);
            }
        }
    }

    public void parseInputNexus(double value) {
        //This parse is specific to the nexus 4 (possibly only the one in the lab)
        boolean printed = false;
        last10.add(value);
        if (last10.size() > 3) {
            double diff = last10.get(last10.size() - 1) - last10.get(last10.size() - 3);
            if (last10.size() > 6) {
                largeDiff = last10.get(last10.size() - 1) - last10.get(last10.size() - 7);
            }
            //Check to see if the signal inverts
            if (startBit || bitCount > 0) {
                if (startBit) {
                    startBit = false;
                    lastValue = value;
                    bits.add(0);
                    System.out.print(0);
                    bitCount++;
                    nextChar = stepSize * 3 / 2;
                    printed = true;
                } else if (nextChar == j) {
                    printed = true;
                    if (lastHigh > 0 && value < 0 && (lastHigh - value) < 0.3) {
                        invertNextChange = true;
                    }
                    if (Math.abs(lastValue - value) > 0.3 && invertNextChange) {
                        inverted = !inverted;
                        invertNextChange = false;
                        System.out.print(" flipped ");
                    }
                    if (Math.abs(largeDiff) > Math.abs(2 * prevDiff)) {
                        currentBit = !(bits.get(bits.size() - 1) == 1);
                    } else if (Math.abs(value) < 0.2) {
                        currentBit = diff > 0 ? inverted : !inverted;
                    } else {
                        currentBit = value > 0 ? !inverted : inverted;
                    }
                    lastHigh = value > 0 ? value : lastHigh;
                    lastValue = value;
                    bits.add(currentBit ? 1 : 0);
                    System.out.print(currentBit ? 1 : 0);
                    nextChar += stepSize;
                    bitCount++;
                    prevDiff = largeDiff;

                    //Translate to number Value
                    if (bitCount != 10) {
                        currentValue += twos * (currentBit ? 1 : 0);
                        twos *= 2;
                    }
                    if (bitCount == 10) {
                        bitCount = 0;
                        j = 0;
                        twos = 1;
                        currentBit = false;
                        bytes.add(currentValue);
                        System.out.println(" " + currentValue);
                        currentValue = 0;
                    }
                }
                j++;
            } else if (Math.abs(diff) > 0.3 && 0 > (inverted ? -diff : diff)) {

                startBit = true;
            }
            if (last10.size() > 9) {
                last10.remove((int) 0);
            }
        }
    }
}

