package survey;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import calit2.surveycreator.SurveyCompletedActivity;

/**
 * Created by Ara on 4/27/15.
 */
public class CompletedShortQuizActivity extends SurveyCompletedActivity {
    @Override
    public void saveData(String a) {
        //File file = ;
        String output = "";
        int[] answers = getAnswers();
        for(int answer:answers){
            output+=answer+",";
        }
        output = output.substring(0, output.length() - 2) + "";
        String filename = getIntent().getStringExtra("filename");
        File file = new File(filename);
        PrintWriter printOut;
        try {
            printOut = new PrintWriter(new FileWriter(file,true));
            printOut.print(output);
            printOut.flush();
            printOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        finish();
    }
}
