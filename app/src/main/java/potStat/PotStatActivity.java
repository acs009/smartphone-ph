package potStat;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import re.serialout.CommandInterface;
import re.serialout.PlaySine;
import re.serialout.R;


public class PotStatActivity extends ActionBarActivity {
    CommandInterface commandInterface;
    PotStatCommands commands = new PotStatCommands();
    TextView mainText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.pot_stat_main);
        mainText=(TextView) findViewById(R.id.testSelectText);
        commandInterface = new CommandInterface(2400, this.getApplicationContext(), mainText);
//        PlaySine.start();
    }

    public void setTest(View view) {
        Spinner testSpinner = (Spinner) findViewById(R.id.testSpinner);
        TextView textView = (TextView) findViewById(R.id.testSelectText);
        Spinner rateSpinner = (Spinner) findViewById(R.id.rateSpinner);
//        System.out.println("Values " + testSpinner.getSelectedItemPosition() + " " + rateSpinner.getSelectedItemPosition());
        int test = testSpinner.getSelectedItemPosition();
        int rate = rateSpinner.getSelectedItemPosition();
        if (test != 0 && rate != 0) {
            if (commands.setTest(test, rate)) {
//                if (commands.readyCheck()) {
//                        Button button =(Button) findViewById(R.id.startPotStatTestButton);
//                        button.setVisibility(View.VISIBLE);
//                    }
//                else{
//                    textView.setText("ReadyCheck Failed!\n");
//                }
            } else {

                textView.append("Test set failed! Try again.\n");
            }

        } else {
            textView.append("You need to select a test and a rate. \n");
        }
        commandInterface.staticAudio.inputDecoder.refreshText();
    }
    private class runFullTest extends AsyncTask<String, Void, String> {
        TextView textView = (TextView) findViewById(R.id.mainText);
        @Override
        protected String doInBackground(String... params) {
            commands.sendCommand(255);
            commands.recordRawData();
            return "Executed";
        }

        @Override
        protected void onPostExecute(String result) {
            mainText.setText("Storing Data");
            mainText.setText("Testing Complete, data stored on drive.");
        }

        @Override
        protected void onPreExecute() {
            mainText.setText("Testing in progress");
        }

        @Override
        protected void onProgressUpdate(Void... values) {
        }
    }

    public void startRecording(View view) {
//        commandInterface.getResponse(10);
        int command[] = {15};
        commandInterface.sendCommand(command, 0);
//         commandInterface.recordRawData();
    }
    public void potStatQueryID(View view){
        commands.sendCommand(13);
        commandInterface.staticAudio.inputDecoder.refreshText();
    }
    public void startPotStatTest(View view){
        new runFullTest().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_pot_stat, menu);
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
