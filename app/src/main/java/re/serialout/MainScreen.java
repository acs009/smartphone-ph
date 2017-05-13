/* LICENSE: You can do whatever you want with this, on four conditions.
 * 1) Share and share alike. This means source, too.
 * 2) Acknowledge attribution to spiritplumber@gmail.com in your code.
 * 3) Email me to tell me what you're doing with this code! I love to know people are doing cool stuff!
 * 4) You may NOT use this code in any sort of weapon.
 */

package re.serialout;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;

import databasemanager.PatientDbHelper;
import databasemanager.Settings;
import pH.pHActivity;
import pH.pHCommands;
import potStat.PotStatActivity;
import survey.DemographicActivity;
import survey.QuestionnaireActivity;
import survey.ShortQuizActivity;

//This class I recycled to use as the testing frame for the app. All the code is pretty much left the way it was
//with the execption of the buadRate being changed to 2400 and a few buttons added.
public class MainScreen extends Activity {

    //MAKE SURE TO UPDATE THIS IF YOUR POSTING A NEW VERSION
    double currentAppVersion = 1.1;


    int buadRate = 2400;
    static public final char cr = (char) 13; // because i don't want to type that in every time
    static public final char lf = (char) 10; // because i don't want to type that in every time
    private PlaySine playSine = new PlaySine();
    //Holds all the command classes
    CommandInterface commandInterface;
    pHCommands phCommands = new pHCommands();
    Settings settings;
    PatientDbHelper dbhelper;
    //for debug button
    int timesPressed = 0;
    long initTime = 0;
    boolean debugMode = false;
    boolean idSet = false;
    public boolean deviceConnected = false;
    public String idText;
    private boolean clincMode = false;
    private boolean glassProbe = true;
    AudioManager am;


    //for headphone check
    private BroadcastReceiver AudioBroadcastReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            MainScreen.this.receivedBroadcast(intent);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //Set up patient tracking
        dbhelper = new PatientDbHelper(this);
        //Set up communications
        AudioSerialOutMono.activate();
        AudioSerialOutMono.context = this.getApplicationContext();
        for (int rate : new int[]{8000, 11025, 16000, 22050, 44100}) {
            //Just here to make sure that the buffer types we are using are ok for the phone.
            //Debug thing.
            int bufferSize = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_DEFAULT, AudioFormat.ENCODING_PCM_16BIT);
            if (bufferSize > 0) {
                System.out.println("Buffer size not 0 for rate: " + rate);
            }
        }
        am = (AudioManager)this.getSystemService(Context.AUDIO_SERVICE);
        am.registerMediaButtonEventReceiver( new ComponentName(this, ButtonReciver.class));
        setUpFreqSlider();
//        playSine.start();
        playSine.adjustFreq(14000);
        TextView pHoutput = (TextView) findViewById(R.id.instrText);
        //Check for pateint ID and log patient in
        commandInterface = new CommandInterface(buadRate, this.getApplicationContext(), pHoutput);
        if (savedInstanceState != null) {
            idSet = savedInstanceState.getBoolean("idSet");
            if (idSet) {
                idText = savedInstanceState.getString("idText");
                hideIDViews();
                settings = dbhelper.getSettings(Integer.parseInt(idText));
                if (settings.isFirstTime()) {
                    Intent intent = new Intent(this, DemographicActivity.class);
                    intent.putExtra("patientID", idText);
                    startActivity(intent);
                }
                if (settings.isSurveyReady()) {
                    (findViewById(R.id.startSurvey)).setVisibility(View.VISIBLE);
                }
            }
        }

        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        clincMode = sharedPreferences.getBoolean("clincMode", false);
        if (!clincMode) {
            System.out.println("Not in clinic mode");
            idSet = sharedPreferences.getBoolean("idSet", false);
            if (idSet) {
                idText = sharedPreferences.getString("idText", "");
                System.out.println("id is: " + idText);
                hideIDViews();
            }
        }
//        new checkForUpdates().execute();
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            idSet = savedInstanceState.getBoolean("idSet");
            if (idSet) {
                idText = savedInstanceState.getString("idText");
                hideIDViews();
                settings = dbhelper.getSettings(Integer.parseInt(idText));
                if (settings.isFirstTime()) {
                    Intent intent = new Intent(this, DemographicActivity.class);
                    intent.putExtra("patientID", idText);
                    startActivity(intent);
                }
                if (settings.isSurveyReady()) {
                    (findViewById(R.id.startSurvey)).setVisibility(View.VISIBLE);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter iff = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        // Put whatever message you want to receive as the action
        this.registerReceiver(this.AudioBroadcastReciver, iff);
    }

    @Override
    public void onPause() {
        super.onPause();
        this.unregisterReceiver(this.AudioBroadcastReciver);
    }
    private LongOperation longOperation;
    private void receivedBroadcast(Intent i) {
        if(am.isWiredHeadsetOn()){
            System.out.println("HEADSET DETECTED!!!");
        }
        else{
            System.out.println("NO HEADSET!!!");
        }
        if (!idSet) {
            if (i.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = i.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        deviceConnected = false;
                        break;
                    case 1:
                        break;
                    default:
                }
            }
            return;
        }

        //final Animation scale = AnimationUtils.loadAnimation(this, R.anim.scale);
        if (i.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
            final Button activate = (Button) findViewById(R.id.seqButton);
            final TextView mainText = (TextView) findViewById(R.id.mainText);
            activate.setText("Activate Device");
            int state = i.getIntExtra("state", -1);
            switch (state) {
                case 0:
                    deviceConnected = false;
                    if (activate.getVisibility() == View.VISIBLE) {
//                        longOperation.cancel(true);
                        playSine.stop();
                        System.out.println("Playsine told to stop");
                        mainText.setText("\nPlease Plug in pH Sensor\n");
                    }
                    activate.setVisibility(View.GONE);
                    break;
                case 1:
                    deviceConnected = true;

//                    if (idSet) {
                        playSine.start();
                        System.out.println("Play Sine told to start");
                        if (!commandInterface.setUp&&false) {
                            longOperation = new LongOperation();
                            longOperation.execute();
                        }
                        else {
                            activate.setVisibility(View.VISIBLE);
                            findViewById(R.id.progressBar).setVisibility(View.GONE);
                            mainText.setText("\nSensor Ready!\n");
                        }
//                    }
                    break;
                default:
            }
        }
        else if(i.getAction().equals(Intent.ACTION_MEDIA_BUTTON)){
            //HERE TO CANCEL THE VOLUME CONTROL BUTTONS. IDK MAY CAUSE BUGS.
        }
    }

    private class LongOperation extends AsyncTask<String, Void, String> {
        TextView textView = (TextView) findViewById(R.id.mainText);

        @Override
        protected String doInBackground(String... params) {
            commandInterface.startUpLoop();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            findViewById(R.id.seqButton).setVisibility(View.VISIBLE);
            findViewById(R.id.progressBar).setVisibility(View.GONE);
            textView.setText("\nSensor Ready!\n");
        }

        @Override
        protected void onPreExecute() {
            findViewById(R.id.progressBar).setVisibility(View.VISIBLE);
            textView.setText("\nSyncing with device...\n");
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
    private double downloadUrl(String myurl) throws IOException {
        InputStream is = null;
        // Only display the first 500 characters of the retrieved
        // web page content.
        int len = 500;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(10000 /* milliseconds */);
            conn.setConnectTimeout(15000 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoInput(true);
            // Starts the query
            conn.connect();
            int response = conn.getResponseCode();
            Log.d("HTTP_TEST", "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string
            String contentAsString = readIt(is, len);
            int i=contentAsString.indexOf('.');
            double version = Double.parseDouble(contentAsString.charAt(i-1)+"."+contentAsString.charAt(i+1));
            System.out.println(version);
            return version;
            // Makes sure that the InputStream is closed after the app is
            // finished using it.
        }
        finally {
            if (is != null) {
                is.close();
            }
        }
    }
    public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
        Reader reader = null;
        reader = new InputStreamReader(stream, "UTF-8");
        char[] buffer = new char[len];
        reader.read(buffer);
        return new String(buffer);
    }
    private class checkForUpdates extends AsyncTask<String, Void, String> {
        //Code to enable remote updating
        TextView textView = (TextView) findViewById(R.id.mainText);
        double postedVersion=-1;

        @Override
        protected String doInBackground(String... params) {
            try {
                postedVersion =downloadUrl("http://cascode.ucsd.edu/cf/version.html");
                return "Executed";
            } catch (IOException e) {
                System.out.println("Broken link");
                return "Unable to retrieve web page. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if(postedVersion!=currentAppVersion&&postedVersion>0){
                startAppUpdate(postedVersion);
            }
        }

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }
    public void startAppUpdate(final double version) {
        System.out.println("START UPDATE");
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("There is a new update available, would you like to update now?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Uri website = Uri.parse("http://cascode.ucsd.edu/cf/cfapp_"+version+".apk");
                Intent webIntent = new Intent(Intent.ACTION_VIEW, website);
                startActivity(webIntent);
            }
        });
        builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    boolean playSineOn = false;

    public void stressTest(View view) {
        //General debug button
        /*
        This button starts the stress test. See command interface for more info.
         */
//        commandInterface.runTest();
//        buildNotification(this);
        setNotificationTimer();
//        Intent intent = new Intent(this, ShortQuizActivity.class);
//        intent.putExtra("filename", phCommands.getFilename());
//        intent.putExtra("arrayID", R.array.short_quiz);
//        intent.putExtra("patientID", idText);
//        startActivity(intent);
        if(!playSineOn)
        playSine.start();
        else{
            playSine.stop();
        }
        playSineOn=!playSineOn;
//
//        phCommands.createFile();
//        Intent intent = new Intent(this, pHResultsActivity.class);
//        intent.putExtra("filename", phCommands.getFilename());
//        startActivity(intent);
    }
    public void launchSnapshot(View  view){
        //Lanuch short patient survey
        phCommands.createFile(idText);
        Intent intent = new Intent(this, ShortQuizActivity.class);
        intent.putExtra("filename", phCommands.getFilename());
        intent.putExtra("arrayID", R.array.short_quiz);
        startActivity(intent);
    }
    public void lanuchPotStat(View view){
        //Piggy backed other device into this app
        Intent intent = new Intent(this, PotStatActivity.class);
        startActivity(intent);
    }
    public void lanuchPh(View view){
        //Take patient measurement
        Intent intent = new Intent(this,pHActivity.class);
        startActivity(intent);
    }
    public void lanuchTimeSeries(View vew){
        //For device calibration/Testing. Multiple measurements over a window of time.
        Intent intent = new Intent(this, TimeSeriesDataActivity.class);
        intent.putExtra("glassProbe",glassProbe);
        startActivity(intent);
    }
    public void changeProbeSettings(View view){
        //Used to adjust the ph measurements biasing to keep different probes in the opamps range.
        if(glassProbe){
            final int[] output = {170, 6, 0, 6, 238};
            AudioSerialOutMono.outputFSK(output);
            Button bt=(Button) findViewById(R.id.probeButton);
            bt.setText("Set for Glass Probe");
            glassProbe=false;
        }
        else{
            final int[] output = {170, 7, 0, 7, 238};
            AudioSerialOutMono.outputFSK(  output);
            Button bt=(Button) findViewById(R.id.probeButton);
            bt.setText("Set for 250 BT Probe");
            glassProbe=true;
        }
    }
    public void startTest(View view) {
        //General Debugging
        SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Button button = (Button) findViewById(R.id.testButton);
        if (!clincMode) {
            clincMode = true;
            button.setText("Set to Home Mode");
        } else {
            clincMode = false;
            button.setText("Set to Clinic Mode");
            if (idSet) {
                editor.putBoolean("idSet", idSet);
                editor.putString("idText", idText);
            }
        }
        editor.putBoolean("clincMode", clincMode);
        editor.commit();
    }


    public void hideIDViews() {
        //Used to swap out the patient log in screens.
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.id_layout);
        linearLayout.setVisibility(View.GONE);
        LinearLayout idLayout = (LinearLayout) findViewById(R.id.options_layout);
        idLayout.setVisibility(View.VISIBLE);
        findViewById(R.id.surveyButton).setVisibility(View.VISIBLE);
        TextView mainText = (TextView) findViewById(R.id.mainText);
        mainText.setText("Hello, Patient: " + idText + "!\nPlease plug in your sensor or take a survey.");
        Button activate = (Button) findViewById(R.id.seqButton);
        if (deviceConnected) {
            playSine.start();
            System.out.println("Play Sine told to start");
            if (!commandInterface.setUp)
                new LongOperation().execute();
            else {
            activate.setVisibility(View.VISIBLE);
                findViewById(R.id.progressBar).setVisibility(View.GONE);
            mainText.setText("\nSensor Ready!\n");
                activate.setText("Activate Device");
            }
        }
//        }
    }

    public void setID(View view) {
        //Store pateient information/Keep someone logged in.
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
        EditText editText = (EditText) findViewById(R.id.id_text);
//        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.id_layout);
//        TextView textView = (TextView) findViewById(R.id.mainText);
        idText = editText.getText().toString();
        if(idText.length()<1){
            return;
        }
//        textView.setText("Hello, "+idText+"!\nPlease plug in your sensor or take a survey.");
        System.out.println(idText);
        settings = dbhelper.getSettings(Integer.parseInt(idText));
        hideIDViews();
        idSet = true;
//        linearLayout.setVisibility(View.GONE);
        if (settings.isSurveyReady()) {
            (findViewById(R.id.startSurvey)).setVisibility(View.VISIBLE);
        }
        if (settings.isFirstTime() || clincMode) {
            Intent intent = new Intent(this, DemographicActivity.class);
            intent.putExtra("patientID", idText);
            startActivity(intent);
        }
        Log.i("TAGGERINO", dbhelper.getDemographics(Integer.parseInt(idText)).toString());
        if (true || !clincMode) {
            SharedPreferences sharedPreferences = getPreferences(MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("idSet", idSet);
            editor.putString("idText", idText);
            editor.commit();
        }
    }

    public void resetID(View view) {
        idSet = false;
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.id_layout);
        linearLayout.setVisibility(View.VISIBLE);
        linearLayout = (LinearLayout) findViewById(R.id.options_layout);
        linearLayout.setVisibility(View.GONE);
        findViewById(R.id.surveyButton).setVisibility(View.GONE);
        TextView mainText = (TextView) findViewById(R.id.mainText);
        mainText.setText("Welcome\nPlease enter your patient ID:");
        settings = null;
        Button activate = (Button) findViewById(R.id.seqButton);
        if (activate.getVisibility() == View.VISIBLE) {
            playSine.stop();
            mainText.setText("\nPlease Plug in pH Sensor\n");
        }
        activate.setVisibility(View.GONE);

    }

    public void startRecording(View view) {
        //For debugging
//        commandInterface.staticAudio.recordData();
        commandInterface.getResponse(5,true);
//        commandInterface.staticAudio.stopData();
    }

    public void activateDevice(View view) {
        //Launch the ph app after ensuring that the module is being powered and working.
        //Send the current patient ID to the pH activity for informatio storing.
        //TODO automate this. Should not need its own button.
        Button outputButton = (Button) findViewById(R.id.seqButton);

        String id = commandInterface.queryID(2);
        /*
        TODO make this actually do something.
        */
        if (false && id == null) {
            outputButton.setText("Activation Failed: Press again to retry.");
        } else if (true || id.equals("10")) {
            commandInterface.releaseAll();
//        if(settings.isFirstTime()){
//            Intent intent = new Intent(this, DemographicActivity.class);
//            intent.putExtra("patientID",idText);
//            startActivity(intent);
//            finish();
//        }
            Intent intent = new Intent(this, pHActivity.class);
            intent.putExtra("patientID", idText);
            startActivity(intent);
//            outputButton.setText("Activation Successful! \nDevice ID: " + id);
        } else {
            outputButton.setText("Activation Failed: Press again to retry.");
        }
    }
    /*
    ====The following commands are for potStatMain.
     */


    public boolean onCreateOptionsMenu(Menu menu) {
        // these show up in the primary screen: out of order for display reasons
        menu.add(0, 0, 0, "EXITING");
        return true;
    }

    public boolean onPrepareOptionsMenu(Menu menu) {
        android.os.Process.killProcess(android.os.Process.myPid());
        return true;
    }

    public void debugButton(View view) {
        //This button makes the debug buttons appear and dissapear if you tap the screen 7 times quickly.

        timesPressed++;
        GregorianCalendar cal = new GregorianCalendar();
        if (timesPressed == 1) {
            initTime = cal.getTimeInMillis();
        } else if (cal.getTimeInMillis() - initTime > 3000) {
            timesPressed = 1;
            initTime = cal.getTimeInMillis();
        } else if (!debugMode && timesPressed > 6 && cal.getTimeInMillis() - initTime < 3000) {
            //ACTIVATE DEBUG MODE
            debugMode = true;
            Button testButton = (Button) findViewById(R.id.testButton);
            testButton.setVisibility(View.VISIBLE);
            Button stressTest = (Button) findViewById(R.id.stressTest);
            stressTest.setVisibility(View.VISIBLE);
            Button record = (Button) findViewById(R.id.recordButton);
            record.setVisibility(View.VISIBLE);
            TextView freqText = (TextView) findViewById(R.id.freqtext);
            freqText.setVisibility(View.VISIBLE);
            findViewById(R.id.potStatButton).setVisibility(View.VISIBLE);
            SeekBar freqSeek = (SeekBar) findViewById(R.id.freqSeekBar);
            freqSeek.setVisibility(View.VISIBLE);
            findViewById(R.id.probeButton).setVisibility(View.VISIBLE);
            findViewById(R.id.lanuchPhButton).setVisibility(View.VISIBLE);
            findViewById(R.id.lanuchTimeSeries).setVisibility(View.VISIBLE);

            timesPressed = 0;
        } else if (debugMode && timesPressed > 6 && cal.getTimeInMillis() - initTime < 3000) {
            debugMode = false;
            Button stressTest = (Button) findViewById(R.id.stressTest);
            stressTest.setVisibility(View.GONE);
            Button testButton = (Button) findViewById(R.id.testButton);
            testButton.setVisibility(View.GONE);
            Button record = (Button) findViewById(R.id.recordButton);
            record.setVisibility(View.GONE);
            TextView freqText = (TextView) findViewById(R.id.freqtext);
            freqText.setVisibility(View.GONE);
            SeekBar freqSeek = (SeekBar) findViewById(R.id.freqSeekBar);
            freqSeek.setVisibility(View.GONE);
            findViewById(R.id.potStatButton).setVisibility(View.GONE);
            findViewById(R.id.probeButton).setVisibility(View.GONE);
            findViewById(R.id.lanuchPhButton).setVisibility(View.GONE);
            findViewById(R.id.lanuchTimeSeries).setVisibility(View.GONE);


            timesPressed = 0;
        }
    }

    public void setUpFreqSlider() {
        //sets up the freq slider.
        SeekBar seekBar = (SeekBar) findViewById(R.id.freqSeekBar);
        final TextView freqView = (TextView) findViewById(R.id.freqtext);
        seekBar.setProgress((int) 0.6 * seekBar.getProgress());
        seekBar.setMax(10000);
        seekBar.setProgress(4000);
        freqView.setText("Frequency: " + (10000 + 4000));
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                System.out.println("Current freq: " + (10000 + progress));
                freqView.setText("Frequency: " + (10000 + progress));
                playSine.adjustFreq(10000 + progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    public void startSurvey(View v) {

        Intent intent;

        if (settings.getSurvey() == 1) {
            intent = new Intent(this, QuestionnaireActivity.class);
            intent.putExtra("arrayID", R.array.q1);
            intent.putExtra("patientID", idText);

        } else {
            intent = new Intent(this, QuestionnaireActivity.class);
            intent.putExtra("arrayID", R.array.q2);
            intent.putExtra("patientID", idText);
        }

        startActivity(intent);
    }
    public void buildNotification(Context context){
        //Feature in progress, not in current version.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context);
        builder.setContentTitle("There is a new survey for you to complete!");
        builder.setContentText("Please take a second to fill it out.");
        builder.setSmallIcon(R.drawable.logo2);
        builder.setAutoCancel(true);
        System.out.println("BUILT");
        Intent resultIntent = new Intent(this,MainScreen.class);


        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
        stackBuilder.addParentStack(MainScreen.class);
        stackBuilder.addNextIntent(resultIntent);

        PendingIntent pendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(pendingIntent);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(100, builder.build());
    }
    public class AlarmReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            System.out.println("Recieved!");
            buildNotification(context);
        }
    }
    public void setNotificationTimer(){
        PendingIntent alarmSender = PendingIntent.getBroadcast(this, 0, new Intent(this, AlarmReceiver.class), 0);
        //Set the alarm to 10 seconds from now
        Calendar c = Calendar.getInstance();
        c.add(Calendar.SECOND, 10);
        long firstTime = c.getTimeInMillis();
        // Schedule the alarm!
        AlarmManager am = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, firstTime, alarmSender);
    }

    public void onSaveInstanceState(Bundle bundle) {
        bundle.putBoolean("idSet", idSet);
        bundle.putString("idText", idText);
        super.onSaveInstanceState(bundle);
    }


}

