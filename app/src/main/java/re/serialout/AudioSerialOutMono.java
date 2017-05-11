package re.serialout;


import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;

/*
    This class generates FSK waveforms and sends them to the device
 */
public class AudioSerialOutMono {
    // idea from http://marblemice.blogspot.com/2010/04/generate-and-play-tone-in-android.html as modified by Steve Pomeroy <steve@staticfree.info>

    private static Thread audiothread = null;
    private static AudioTrack audiotrk = null;
    private static byte generatedSnd[] = null;
    public static Context context;
    public static boolean playing = false;

    // set that can be edited externally
    public static int max_sampleRate = 48000;
    public static int min_sampleRate = 4000;
    public static int new_baudRate = 1200; // assumes N,8,1 right now
    public static int new_sampleRate = 48000; // min 4000 max 48000
    public static int new_characterdelay = 0; // in audio frames, so depends on the sample rate. Useful to work with some microcontrollers.
    public static boolean new_levelflip = false;
    public static String prefix = "";
    public static String postfix = "";

    // set that is actually used: this is so they get upadted all in one go (safer)
    private static int baudRate = 1200;
    private static int sampleRate = 48000;
    private static int characterdelay = 0;
    private static boolean levelflip = false;

    //Used to manage the timing to avoid echos.
    int waitTime = 0;

    public static LinkedList<byte[]> playque = new LinkedList<byte[]>();
    public static byte[] playBytes;
    public static boolean active = false;



    public static void UpdateParameters(boolean AutoSampleRate) {
        baudRate = new_baudRate; // we're not forcing standard baud rates here specifically because we want to allow odd ones
        if (AutoSampleRate == true) {
            new_sampleRate = new_baudRate;
            while (new_sampleRate <= (max_sampleRate)) {
                new_sampleRate *= 2;//+= new_baudRate;
            }
            new_sampleRate /= 2;
        }
        if (new_sampleRate > max_sampleRate)
            new_sampleRate = max_sampleRate;
        if (new_sampleRate < min_sampleRate)
            new_sampleRate = min_sampleRate;

        sampleRate = new_sampleRate; // min 4000 max 48000
        if (new_characterdelay < 0)
            new_characterdelay = 0;
        characterdelay = new_characterdelay;
        levelflip = new_levelflip;
        minbufsize = AudioTrack.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

    }

    public static void output(int[] input) {
        if (input == null)
            return;
        playque.clear();
        if(audiotrk!=null) {
            audiotrk.release();
            audiotrk = null;
        }
        playing=true;

        playque.add(SerialIntsFullDAC(input));

        synchronized (audiothread) {
            audiothread.notify();
        }
        int playRate = 48000;
        System.out.println("PlayRate: " + playRate);
        double waitTime = 100 + ((double) length / playRate) / 4.0 * 1000.0;
        System.out.println("wait time: " + waitTime);
        try {
            Thread.sleep((int) 0, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public static void outputFSK(int[] input) {
        if (input == null)
            return;
        playque.clear();
        if(audiotrk!=null) {
            audiotrk.release();
            audiotrk = null;
        }
        playing=true;
//        playBytes=FSKSerialIntsFullDAC(input);


        playque.add(FSKSerialIntsFullDAC(input));
//        playSound();

        synchronized (audiothread) {
            audiothread.notify();
        }
        //Gives the audio thread enough time to update Length.
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        int playRate = 48000;
        //**DEBUG
        double waitTime = 105 + ((double) length / playRate) / 4.0 * 1000.0;
//        double waitTime=0;
        System.out.println("wait time: " + waitTime);
//        Waits untill the message has sent to start recording. prevents echos.
        try {
            Thread.sleep((int) waitTime, 0);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    public static void output(String sendthis) {
        if (sendthis == null)
            return;
        playque.add(SerialDAC((prefix + sendthis + postfix).getBytes()));
        synchronized (audiothread) {
            audiothread.notify();
        }
    }

    public static void output(byte[] sendthis) {
        if (sendthis == null)
            return;
        playque.add(SerialDAC(sendthis));
        synchronized (audiothread) {
            audiothread.notify();
        }
    }

    private static byte jitter = (byte) (4);
    private static byte logichigh = (byte) (-128);
    private static byte logiclow = (byte) (16);

    private static int bytesinframe = 10 + characterdelay;
    private static int i = 0; // counter
    private static int j = 0; // counter
    private static int k = 0; // counter
    private static int m = 0; // counter
    private static int n = sampleRate / baudRate;
    private static byte l = jitter; // intentional jitter used to prevent the DAC from flattening the waveform prematurely

    public static byte[] SerialDAC(byte[] sendme) {
        if (levelflip) {
            logiclow = (byte) (-128);
            logichigh = (byte) (16);
            jitter = (byte) (4);
        } else {
            logichigh = (byte) (-128);
            logiclow = (byte) (16);
            jitter = (byte) (4);
        }

        bytesinframe = 10 + characterdelay;
        i = 0; // counter
        j = 0; // counter
        k = 0; // counter
        m = 0; // counter
        n = sampleRate / baudRate;
        boolean[] bits = new boolean[sendme.length * bytesinframe];
        byte[] waveform = new byte[(sendme.length * bytesinframe * sampleRate / baudRate)]; // 8 bit, no parity, 1 stop
        //Arrays.fill(waveform, (byte) 0);
        Arrays.fill(bits, true); // slight opti to decide what to do with stop bits

        // generate bit array first: makes it easier to understand what's going on
        for (i = 0; i < sendme.length; ++i) {
            m = i * bytesinframe;
            bits[m] = false;
            bits[++m] = ((sendme[i] & 1) == 1);//?false:true;
            bits[++m] = ((sendme[i] & 2) == 2);//?false:true;
            bits[++m] = ((sendme[i] & 4) == 4);//?false:true;
            bits[++m] = ((sendme[i] & 8) == 8);//?false:true;
            bits[++m] = ((sendme[i] & 16) == 16);//?false:true;
            bits[++m] = ((sendme[i] & 32) == 32);//?false:true;
            bits[++m] = ((sendme[i] & 64) == 64);//?false:true;
            bits[++m] = ((sendme[i] & 128) == 128);//?false:true;
            // cheaper to prefill to true
            // now we need a stop bit, BUT we want to be able to add more (character delay) to play-nice with some microcontrollers such as the Picaxe or BS1 that need it in order to do decimal conversion natively.
            //			for(k=0;k<bytesinframe-9;k++)
            //				bits[++m]=true;
        }
        bits=padWithOnes(bits);

        // now generate the actual waveform using l to wiggle the DAC and prevent it from zeroing out
        for (i = 0; i < bits.length; i++) {
            for (k = 0; k < n; k++) {
                waveform[j++] = (bits[i]) ? ((byte) (logichigh + l)) : ((byte) (logiclow - l));
                l = (l == (byte) 0) ? jitter : (byte) 0;
            }
        }
        return waveform;
    }
    private static boolean[] padWithOnes(boolean input[]){
        boolean ones[]={true,true,true,true,true,true,true,true,true,true};
        boolean output[]=new boolean[input.length+ones.length];
        System.arraycopy(ones,0,output,0,ones.length);
        System.arraycopy(input,0,output,ones.length,input.length);
        return output;
    }
    public static byte[] FSKSerialIntsFullDAC(int [] input){
        double freqs[] = {600,1100};
        int window = sampleRate/150;//300 is the FSK baudRate
        //8 bits plus start and stop bit.
        boolean[] bits = new boolean[input.length*10];
        //make room for padding in signal. large pad of ones at start, 2 ones between each packet
        for (int i = 0; i < input.length; i++) {
            //Adds start bit and end bit
            bits[10 * i] = false;
            bits[10 * i + 9] = true;
            int tempValue = input[i];
            int modValue = 128;
            //Builts a bit array out of the 8 bits of data starting with the largest number and working down.
            for (int x = 8 + i * bytesinframe; x > i * bytesinframe ; x--) {
                bits[x] = ((tempValue % modValue) != tempValue);
                tempValue -= bits[x] ? modValue : 0;
                modValue /= 2;
            }
        }
        bits = padWithOnes(bits);
        //extend out for padding between backets

        ArrayList<Double> samp = new ArrayList<>();
        for(int x =0;x<bits.length;x++){
            if(x%10==0&&x>0)
                System.out.println();
            System.out.print(bits[x]?"1":"0");
        }
        System.out.println();
        // fill out the array
        int i = 0;
        double phase = 0;
        double currentFreq=freqs[0];
        for(int j=0;j<bits.length;j++){
            phase = (2*Math.PI*i*(currentFreq-freqs[bits[j]?1:0])/sampleRate+phase)%(2*Math.PI);//fixes phase errors
            currentFreq=freqs[bits[j]?1:0];

            int h =0;
            while (h<window) {
                samp.add((double)((Math.sin((2.0 * Math.PI) * (double)(i) / (sampleRate /currentFreq)+phase))>0?1:-1));
                i++;
                h++;
            }
            if(j%10==9&&j!=0){
                h=0;
                while (h<3*window) {
                    phase = (2*Math.PI*i*(currentFreq-freqs[1])/sampleRate+phase)%(2*Math.PI);//fixes phase errors
                    currentFreq=freqs[1];
                    samp.add((double)((Math.sin((2.0 * Math.PI) * (double)(i) / (sampleRate /currentFreq)+phase))>0?1:-1));
                    i++;
                    h++;
                }
            }

        }
        byte[] waveform=new byte[(samp.size())*2];
        int idx = 0;
        //Turn the doubles into bytes for 16 bit PCM
        for (final double dVal : samp) {
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            waveform[idx++] = (byte) (val & 0x00ff);
            waveform[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        return waveform;
    }
    public static byte[] SerialIntsFullDAC(int[] input) {
        if (levelflip) {
            logiclow = (byte) (-128);
            logichigh = (byte) (32);
            jitter = (byte) (4);
        } else {
            logichigh = (byte) (-128);
            logiclow = (byte) (32);
            jitter = (byte) (4);
        }
        System.out.println("Buad: " +2*baudRate);
        bytesinframe = 10 + characterdelay;
        i = 0; // counter
        j = 0; // counter
        k = 0; // counter
        m = 0; // counter
        n = sampleRate / baudRate;
        boolean[] bits = new boolean[input.length * bytesinframe];
        byte[] waveform = new byte[((input.length+15+64) * 2*(bytesinframe+1) * sampleRate / baudRate)]; // 16 bit, no parity, 1 stop, random numbers added to make room for buffers start or end of the packet

        for (int i = 0; i < input.length; i++) {
            //Adds start bit and end bit
            bits[10 * i] = false;
            bits[10 * i + 9] = true;
            int tempValue = input[i];
            int modValue = 128;
            //Builts a bit array out of the 8 bits of data starting with the largest number and working down.
            for (int x = 8 + i * bytesinframe; x > i * bytesinframe ; x--) {
                bits[x] = ((tempValue % modValue) != tempValue);
                tempValue -= bits[x] ? modValue : 0;
                modValue /= 2;
            }
        }
        for(int x =0;x<bits.length;x++){
            if(x%10==0&&x>0)
                System.out.println();
            System.out.print(bits[x]?"1":"0");

        }
        System.out.println();
        bits=padWithOnes(bits);
        // now generate the actual waveform using l to wiggle the DAC and prevent it from zeroing out
        for(int x=0;x<48*n;x++){
            waveform[j++]=((byte) (-32767 & 0x00ff));
            waveform[j++]=((byte) ((-32767 & 0xff00) >>> 8));
        }
        //actually creates the square waves
        for (i = 0; i < bits.length; i++) {
            if(i%10==0){
                for(int x=0;x<n;x++){
                    waveform[j++]=((byte) (32767 & 0x00ff));
                    waveform[j++]=((byte) ((32767 & 0xff00) >>> 8));
                }
            }
            //====FLIPPED THE BITS HERE FOR THE POT STAT====
            for (k = 0; k < n; k++) {
                waveform[j++] = (bits[i]) ? ((byte) (32767 & 0x00ff)) : ((byte) (-32767 & 0x00ff));
                waveform[j++]= bits[i]?((byte) ((32767 & 0xff00) >>> 8)):((byte) ((-32767 & 0xff00) >>> 8));
            }

        }
//        HUGE AMOUNT OF ZEROS TO TRY AND FIX THE BOARD
//        TODO
        for(int x=0;x<64*n;x++){
            waveform[j++]=((byte) (-32767 & 0x00ff));
            waveform[j++]=((byte) ((-32767 & 0xff00) >>> 8));
        }
        bits = null;
        return waveform;

    }
    // essentially a constructor, but i prefer to do a manual call.
    public static void activate() {
        UpdateParameters(true);

        // Use a new tread as this can take a while
        audiothread = new Thread() {
            public void run() {
                active = true;
                synchronized (audiothread) {
                    Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
                    while (active) {
                        try {
                            audiothread.wait(Long.MAX_VALUE);
                            while (playque.isEmpty() == false)
                                playSound();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        };
        audiothread.start();
        while (active == false) // wait for the thread to actually turn on
        {
            SystemClock.sleep(50);
        }
    }

    public static boolean isPlaying() {
        try {
            return audiotrk.getPlaybackHeadPosition() < (generatedSnd.length);
        } catch (Exception e) {
            return false;
        }
    }


    private static int minbufsize;
    private static int length;

    private static void playSound() {
        if (audiotrk != null) {
            if (generatedSnd != null) {
                while (audiotrk.getPlaybackHeadPosition() < (generatedSnd.length))
                    SystemClock.sleep(50);  // let existing sample finish first: this can probably be set to a smarter number using the information above
            }
            audiotrk.release();
        }
//        UpdateParameters(false); // might as well do it at every iteration, it's cheap
        generatedSnd = playque.poll();
        length = generatedSnd.length;
        if (minbufsize < length)
            minbufsize = length;
        audiotrk = new AudioTrack(AudioManager.STREAM_MUSIC,
                sampleRate, AudioFormat.CHANNEL_OUT_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, generatedSnd.length,
                AudioTrack.MODE_STATIC);
        //THIS FUNCTION USES (Left,Right). RIGHT FOR POT STAT, LEFT FOR ALEX's BOARD.
        audiotrk.setStereoVolume(0, 1);

        audiotrk.write(generatedSnd, 0, length);
        audiotrk.play();
//        int playRate=audiotrk.getPlaybackRate();
//        System.out.println("PlayRate: "+playRate);
//        double waitTime=175+((double)length/playRate)/4.0*1000.0;
//        System.out.println("wait time: "+waitTime);
//        try {
//            Thread.sleep((int)waitTime,0);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

    }
}

