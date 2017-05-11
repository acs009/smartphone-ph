package pH;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import re.serialout.CommandInterface;
import re.serialout.InputDecoder;
import re.serialout.R;
import survey.ShortQuizActivity;


public class pHActivity extends Activity {
    private CommandInterface commandInterface;
    private int buadRate = 2400;
    private float lastX = 0;
    Context context;
    final int ID = 15;
    final int SERIAL = 13;
    final int TEMP = 10;
    final int PH = 11;
    String patientID;
    private pHCommands phCommands = new pHCommands();

//    private BroadcastReceiver AudioBroadcastReciver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            pHActivity.this.receivedBroadcast(intent);
//        }
//    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ph_main);
        context = this;
        TextView pHoutput = (TextView) findViewById(R.id.instrText);
        commandInterface = new CommandInterface(buadRate, this.getApplicationContext(), pHoutput);
        InputDecoder.textView = pHoutput;
        commandInterface.staticAudio.inputDecoder.toggleScreen(true);
        patientID = getIntent().getStringExtra("patientID");
        phCommands.createFile(patientID);
        ViewFlipper phFlipper = (ViewFlipper) findViewById(R.id.phFlipper);
//        phFlipper.showNext();
        Intent intent = new Intent(this, ShortQuizActivity.class);
        intent.putExtra("filename", phCommands.getFilename());
        intent.putExtra("arrayID", R.array.short_quiz);
        startActivity(intent);
//        showOverLay();
    }

    public boolean onTouchEvent(MotionEvent event) {
        ViewFlipper phFlipper = (ViewFlipper) findViewById(R.id.phFlipper);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                if (lastX == 0) {
                    System.out.println("Saw this");
                    lastX = event.getX();
                }
            }
            break;
            case MotionEvent.ACTION_UP: {
                Float currentX = event.getX();
                float diff = lastX - currentX;
                System.out.println("Diff:" + diff);
                //If flip right to left advance the steps as long as we are not at the end
                if (diff > 50 && phFlipper.getDisplayedChild() != 6) {
                    phFlipper.setInAnimation(this, R.animator.in_from_right);
                    phFlipper.setOutAnimation(this, R.animator.out_to_left);
                    phFlipper.showNext();
                    lastX = 0;
                } else if (diff < -50 && phFlipper.getDisplayedChild() != 0) {
                    phFlipper.setInAnimation(this, R.animator.in_from_left);
                    phFlipper.setOutAnimation(this, R.animator.out_to_right);
                    phFlipper.showPrevious();
                    lastX = 0;
                } else {
                    lastX = 0;
                }
            }
            break;
        }
        return true;
    }

    //    boolean flag = false;
    boolean phSuccess;
    private class LongOperation extends AsyncTask<String, Void, String> {
        private Dialog dialog=new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

        @Override
        protected String doInBackground(String... params) {
            phSuccess = phCommands.recordSamples();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
//            phSuccess = phCommands.recordSamples();
            if (!phSuccess) {
                dialog.dismiss();
                TextView pHoutput = (TextView) findViewById(R.id.instrText);
                pHoutput.setText("Commands failed. Please retry.");
                Toast.makeText(getApplication(), "There was an Error, please make sure everything is set up for testing and retry.",
                        Toast.LENGTH_LONG).show();
            } else {

                Intent intent = new Intent(getApplicationContext(), pHResultsActivity.class);
                intent.putExtra("filename", phCommands.getFilename());
                startActivity(intent);
                dialog.dismiss();
            }
//            dialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
            dialog.setContentView(R.layout.test_in_progress);
            dialog.show();
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    public void pHStart(View view) {
        phCommands.createFile(patientID);
        new LongOperation().execute();

//        flag = !flag;
//        if (flag) {
//            new Thread(new Runnable() {
//                @Override
//                public void run() {
//                    while (flag) {
//                        phCommands.recordSamples();
//                        try {
//                            Thread.sleep(500, 0);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//            }).start();
//        }


    }

    public void pHFetch(View vew) {
        phCommands.sendCommand(PH);
        commandInterface.staticAudio.inputDecoder.refreshText();
//        phCommands.phTest();
    }

    public void tempFetch(View view) {
        phCommands.sendCommand(TEMP);
        commandInterface.staticAudio.inputDecoder.refreshText();

    }

    public void pHserialFetch(View view) {
        System.out.println("Called fucntion");

        phCommands.sendCommand(SERIAL);
        commandInterface.staticAudio.inputDecoder.refreshText();

    }

    private void showTestingScreen() {
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setContentView(R.layout.test_in_progress);
        dialog.show();
    }

    private void showOverLay() {

        final Dialog dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.ph_overlay);
        LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.phOverlay);
        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(150);
        dialog.getWindow().setBackgroundDrawable(d);
        layout.setOnClickListener(new View.OnClickListener() {

            @Override

            public void onClick(View arg0) {
                dialog.dismiss();
            }

        });
        dialog.show();

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_p_h, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
