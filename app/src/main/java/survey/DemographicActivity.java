package survey;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.DatePicker;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewFlipper;

import java.util.Calendar;
import java.util.Date;

import databasemanager.Demographics;
import databasemanager.PatientDbHelper;
import databasemanager.Settings;
import re.serialout.MainScreen;
import re.serialout.R;

/**
 * Created by Ara on 4/28/15.
 */
public class DemographicActivity extends Activity{


    private static final String TAG = "DemographicActivity";
    private ViewFlipper vf;
    private DatePicker dp;
    private String answer;
    private PatientDbHelper dbHelper;
    private String[] answers;
    private int index;
    private String patientID;
    private RadioGroup.OnCheckedChangeListener changeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {

            RadioButton rb=(RadioButton)(DemographicActivity.this.findViewById(checkedId));
            answer = rb.getText().toString();
            Log.i("TAG",answer);
        }
    };


    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.personal_info);
        dp = (DatePicker)findViewById(R.id.demo_date);
        vf = (ViewFlipper)findViewById(R.id.demographic);
        dp.init(1900,0,0,null);
        dbHelper = new PatientDbHelper(this);
        ((RadioGroup)(findViewById(R.id.demo2_rg))).setOnCheckedChangeListener(changeListener);
        ((RadioGroup)(findViewById(R.id.demo3_rg))).setOnCheckedChangeListener(changeListener);
        ((RadioGroup)(findViewById(R.id.demo4_rg))).setOnCheckedChangeListener(changeListener);
        ((RadioGroup)(findViewById(R.id.demo5_rg))).setOnCheckedChangeListener(changeListener);
        ((RadioGroup)(findViewById(R.id.demo6_rg))).setOnCheckedChangeListener(changeListener);
        ((RadioGroup)(findViewById(R.id.demo7_rg))).setOnCheckedChangeListener(changeListener);
        index =0;
        answers = new String[7];
        patientID = this.getIntent().getStringExtra("patientID");

        //SQLiteDatabase db = dbHelper.getWritableDatabase();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);



    }

    public void saveBirthDate(View v){
        String bd ="";
        int month= dp.getMonth()+1;
        int day =dp.getDayOfMonth();
        int year =dp.getYear();
        if(month<10){
            bd+="0";
        }
        bd+=month+"/";
        if(day<10){
            bd+="0";
        }
        bd+=day+"/";
        bd+=year;
        answers[index++] =bd;
        vf.showNext();



    }
    public void nextPage(View v){
        if(isChecked(v)){
            answers[index++]=answer;
            vf.showNext();

        }
        else
            Toast.makeText(this, "Please select a choice", Toast.LENGTH_LONG).show();

    }

    public void nextPageOtherField(View v){
        if(isChecked(v)){
            if(answer.equals("Other (Please specify in box below)")) {
                TextView tv = (TextView) findViewById(R.id.demo5_et);
                if (!tv.getText().toString().isEmpty()) {
                    answer = tv.getText().toString();
                    answers[index++]=answer;
                    vf.showNext();
                } else {
                    Toast.makeText(this, "Please specify other", Toast.LENGTH_LONG).show();
                }
            }else{
                answers[index++]=answer;
                vf.showNext();
            }
        }else{
            Toast.makeText(this, "Please select a choice", Toast.LENGTH_LONG).show();
        }
    }
    public boolean isChecked(View v){
        RelativeLayout rv = (RelativeLayout)v.getParent();
        final RadioGroup rg = (RadioGroup)(((RelativeLayout) rv).getChildAt(1));
        if(rg.getCheckedRadioButtonId()==-1){
            return false;
        }
        return true;
    }

    public void endDemo(View v){
        if(isChecked(v)){
            answers[index++]=answer;
            Settings settings = dbHelper.getSettings(Integer.parseInt(patientID));
            settings.completedFirstTime();
            dbHelper.saveSettings(settings);
            Demographics demographics = new Demographics(Integer.parseInt(patientID), answers);
            dbHelper.saveDemographics(demographics);
//           Intent intent = new Intent(this, MainScreen.class);
//            startActivity(intent);
            finish();
        }
        else
            Toast.makeText(this, "Please select a choice", Toast.LENGTH_LONG).show();

    }
}
