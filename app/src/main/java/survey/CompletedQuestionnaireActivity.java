package survey;

import android.content.Intent;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Set;

import calit2.surveycreator.SurveyCompletedActivity;
import databasemanager.Demographics;
import databasemanager.PatientDbHelper;
import databasemanager.Settings;
import re.serialout.MainScreen;

/**
 * Created by Ara on 4/27/15.
 */
public class CompletedQuestionnaireActivity extends SurveyCompletedActivity{
    @Override
    public void saveData(String patientID) {
        PatientDbHelper dbHelper = new PatientDbHelper(this);
        Settings settings = dbHelper.getSettings(Integer.parseInt(patientID));
        int[] answers = getAnswers();
        String output ="";

        Calendar cal = new GregorianCalendar();
        output+=(cal.get(Calendar.MONTH)+1)+"/"+cal.get(Calendar.DATE )+"/"+cal.get(Calendar.YEAR)+",";
        output+=cal.get(Calendar.HOUR)+":"+cal.get(Calendar.MINUTE)+":"+cal.get(Calendar.SECOND);
        String filename = "Survey_"+patientID;

        if(settings.getSurvey()==1){
            filename+="1";
            Demographics demographics = dbHelper.getDemographics(Integer.parseInt(patientID));
            output +=","+ demographics.toString();

        }else{
            filename+="2";
        }
        for(int answer:answers){
            output+=","+answer;

        }
        File file = getFile(filename);
        PrintWriter stream = null;
        try {
            stream = new PrintWriter(new FileWriter(file));
            stream.write(output);
            stream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
        settings.completedSurvey();
        dbHelper.saveSettings(settings);
        dbHelper.close();
        Log.i("TEHEE", output);
        Intent intent = new Intent(this, MainScreen.class);

        startActivity(intent);
        finish();

    }
}
