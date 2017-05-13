package databasemanager;

import android.provider.BaseColumns;
import android.support.v4.app.NotificationCompat;

import java.util.Date;

/**
 * Created by Ara on 4/29/15.
 */
public final class Settings {



    private static final long TIME_DIFFERENCE = 1000*60*60*24*7;
    private int id;
    private int first_time;
    private int survey;
    private long survey_completed;

    public Settings(int id, int first_time, int survey, long survey_completed ){
        this.id = id;
        this.first_time=first_time;
        this.survey = survey;
        this.survey_completed = survey_completed;
    }

    public Settings(){}

    public int getSurvey(){
        return survey;
    }

    public int getFirst_time(){
        return first_time;
    }

    public long getSurvey_completed(){
        return  survey_completed;
    }

    public int getId(){
        return id;
    }

    public void completedSurvey(){
        setSurvey_completed();
        if (survey == 1){
            survey =2;
        }else{
            survey=1;
        }
    }

    public void completedFirstTime(){
        first_time=0;
    }
    public boolean isFirstTime(){
        return first_time==1;
    }
    private void setSurvey_completed(){
        survey_completed= new Date().getTime();
    }
    
    public boolean isSurveyReady(){
        long now = new Date().getTime();
        if(now-getSurvey_completed()>TIME_DIFFERENCE){
            return true;
        }
        
        else return false;
    }




}
