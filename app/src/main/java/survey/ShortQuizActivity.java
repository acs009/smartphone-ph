package survey;

import android.app.Activity;
import android.content.Intent;

/**
 * Created by Ara on 4/27/15.
 */
public class ShortQuizActivity extends calit2.surveycreator.SurveyActivity {
    @Override
    public void onComplete() {
        Intent intent = getCompleteIntent(CompletedShortQuizActivity.class);
        intent.putExtra("filename",getIntent().getStringExtra("filename"));

        startActivity(intent);
        finish();

    }


}
