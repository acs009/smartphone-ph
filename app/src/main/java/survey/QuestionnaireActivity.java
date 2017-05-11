package survey;

import android.content.Intent;

import calit2.surveycreator.SurveyActivity;

/**
 * Created by Ara on 4/27/15.
 */
public class QuestionnaireActivity extends SurveyActivity {
    @Override
    public void onComplete() {

        Intent intent = getCompleteIntent(CompletedQuestionnaireActivity.class);
        startActivity(intent);
        finish();
    }
}
