package re.serialout;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import re.serialout.R;

public class TimeSeriesDataActivity extends ActionBarActivity {
    private Spinner rateSpinner;
    private Spinner durSpinner;
    private boolean pHchecked = false;
    private boolean tempChecked = false;
    private boolean glassProbe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_series_app);
        rateSpinner = (Spinner) findViewById(R.id.timeSamp_rateSpinner);
        durSpinner = (Spinner) findViewById(R.id.timeSamp_durSpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.sampleRate_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rateSpinner.setAdapter(adapter);
        durSpinner.setAdapter(adapter);
        glassProbe=getIntent().getBooleanExtra("glassProbe",true);
    }
    public void timeSampStart(View vew){
        EditText rateText = (EditText) findViewById(R.id.timeSamp_rateText);
        EditText durText = (EditText) findViewById(R.id.timeSamp_durText);
        if(rateText.getText().length()<1||durText.getText().length()<1||!(pHchecked||tempChecked) ){
            Toast toast = Toast.makeText(this,"Fill in all Data Fields",Toast.LENGTH_SHORT);
            toast.show();
            return;
        }
        //get raw values
        int rate = Integer.parseInt(rateText.getText().toString());
        int dur = Integer.parseInt(durText.getText().toString());
        //convert for units
        rate=rate*getConversion(rateSpinner.getSelectedItemPosition());
        dur=dur*getConversion(durSpinner.getSelectedItemPosition());

        Intent intent = new Intent(this,TimeSeriesRecordingActivity.class);
        intent.putExtra("rate",rate);
        intent.putExtra("duration",dur);
        intent.putExtra("ph",pHchecked);
        intent.putExtra("temp",tempChecked);
        intent.putExtra("glassProbe",glassProbe);
        startActivity(intent);
    }
    private int getConversion(int pos){
        int factor=1;
        switch (pos){
            case 0:
                factor=1000;
                break;
            case 1:
                factor=60000;
                break;
        }
        return factor;
    }
    public void onBoxCheck(View view){
        boolean checked = ((CheckBox) view).isChecked();
        if(view.getId()==R.id.timeSamp_pHCheckbox){
            pHchecked=checked;
        }
        else{
            tempChecked=checked;
        }
    }
}
