package databasemanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Created by Ara on 4/29/15.
 */
public class PatientDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION=3;
    public static final String DATABASE_NAME="Patient.db";
    public static final String SETTINGS = "settings";
    public static final String _ID="id";
    public static final String SETTINGS_FIRST_TIME = "first_time";
    public static final String SETTINGS_SURVEY = "survey";
    public static final String SETTINGS_SURVEY_COMPLETED= "survey_completed";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";
    private static final String TEXT = " TEXT";
    private static final String SQL_CREATE_ENTRIES =
            "CREATE TABLE " + PatientDbHelper.SETTINGS + " (" +
                    PatientDbHelper._ID + " INTEGER PRIMARY KEY," +
                    PatientDbHelper.SETTINGS_FIRST_TIME + INTEGER_TYPE + COMMA_SEP +
                    PatientDbHelper.SETTINGS_SURVEY + INTEGER_TYPE + COMMA_SEP +
                    PatientDbHelper.SETTINGS_SURVEY_COMPLETED + INTEGER_TYPE +
                    " )";

    private static final String SQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS " + PatientDbHelper.SETTINGS;

    public static abstract class DemographicsContract implements BaseColumns {
        public static final String TABLE_NAME = "demographics";
        public static final String _ID="id";
        public static final String BIRTH_DATE="birth_date";
        public static final String GENDER = "gender";
        public static final String VACATION="vacation";
        public static final String MARITAL_STATUS="marital_status";
        public static final String ETHNICITY= "ethnicity";
        public static final String EDUCATION="education";
        public static final String WORK="work";
        public static final String CREATE_DEMOGRAPHICS=
                "CREATE TABLE " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY," +
                        BIRTH_DATE + PatientDbHelper.TEXT + PatientDbHelper.COMMA_SEP +
                        GENDER + PatientDbHelper.TEXT + PatientDbHelper.COMMA_SEP +
                        VACATION  + PatientDbHelper.TEXT + PatientDbHelper.COMMA_SEP +
                        MARITAL_STATUS  + PatientDbHelper.TEXT + PatientDbHelper.COMMA_SEP +
                        ETHNICITY + PatientDbHelper.TEXT + PatientDbHelper.COMMA_SEP +
                        EDUCATION + PatientDbHelper.TEXT + PatientDbHelper.COMMA_SEP +
                        WORK + PatientDbHelper.TEXT  +
                        " )";
        public static final String DROP_DEMOGRAPHICS=
                "DROP TABLE IF EXISTS " + TABLE_NAME;


    }



    public PatientDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES);
        db.execSQL(DemographicsContract.CREATE_DEMOGRAPHICS);
        db.execSQL("Insert into settings(id,first_time,survey,survey_completed) values(1,1,1,1)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DELETE_ENTRIES);
        db.execSQL(DemographicsContract.DROP_DEMOGRAPHICS);
        onCreate(db);
    }
    public void checkID(Integer id) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selector = "Select * from "+ PatientDbHelper.SETTINGS + " WHERE id='"+id+"'";
        Cursor cursor = db.rawQuery(selector,null);
        if(cursor.moveToFirst()){
            System.out.println("ID FOUND!");
        }
        else{
            System.out.println("ID NOT FOUND!");
            db.execSQL("Insert into settings(id,first_time,survey,survey_completed) values(" + id + ",1,1,1)");
        }
        cursor.close();

    }

    public Settings getSettings(Integer patentID){
        SQLiteDatabase db = this.getReadableDatabase();
        String selector = "Select * from "+ PatientDbHelper.SETTINGS + " WHERE id='"+patentID+"'";
        Cursor cursor = db.rawQuery(selector,null);
        Settings settings = new Settings();
        if(cursor.moveToFirst()){
            System.out.println("ID FOUND!");
            Log.i("TAG", "SUCCESS");
            Log.i("DBBOO",cursor.getString(1));
            int id = Integer.parseInt(cursor.getString(0));
            int first_time = Integer.parseInt(cursor.getString(1));
            int survey = Integer.parseInt(cursor.getString(2));
            long survey_completed = Long.parseLong(cursor.getString(3));
            settings= new Settings(id,first_time,survey,survey_completed);
            System.out.println("RETRIVED STUFF:"+id+" "+first_time+" "+survey+" "+survey_completed+" Length: "+cursor.getCount());
        }
        else{
            System.out.println("ID NOT FOUND!");
            db.execSQL("Insert into settings(id,first_time,survey,survey_completed) values(" + patentID + ",1,1,1)");
            settings= new Settings(patentID,1,1,1);
        }
        cursor.close();
        db.close();
        return  settings;
    }

    public void saveSettings(Settings settings){
        ContentValues values = new ContentValues();
        values.put(_ID,settings.getId());
        values.put(SETTINGS_FIRST_TIME, settings.getFirst_time());
        values.put(SETTINGS_SURVEY, settings.getSurvey());
        values.put(SETTINGS_SURVEY_COMPLETED, settings.getSurvey_completed());
        SQLiteDatabase db = this.getWritableDatabase();
        String[] args = {String.valueOf(settings.getId())};
        db.update(SETTINGS,values,_ID+"=?",args);
        Log.i("Tag", "insert");
        db.close();



    }

    public Demographics getDemographics(Integer patientID){
        SQLiteDatabase db = this.getReadableDatabase();
//        String selector = "Select * from " + DemographicsContract.TABLE_NAME;
        String selector = "Select * from "+ DemographicsContract.TABLE_NAME + " WHERE id='"+patientID+"'";
        Cursor cursor = db.rawQuery(selector,null);
        Demographics demographics = new Demographics();

        if(cursor.moveToFirst()){
            Log.i("Demo", "get was SUCCESS");
            Log.i("DBBOO",cursor.getString(1));
            int id = Integer.parseInt(cursor.getString(0));
            String birth_date = cursor.getString(1);
            String gender = cursor.getString(2);
            String vacation = cursor.getString(3);
            String marital_status = cursor.getString(4);
            String ethnicity = cursor.getString(5);
            String education = cursor.getString(6);
            String work = cursor.getString(7);
            demographics= new Demographics(id,birth_date,gender,vacation,marital_status,ethnicity,education,work);


        }
        cursor.close();
        db.close();

        return demographics;
    }

    public void saveDemographics(Demographics demographics){
        ContentValues cv = new ContentValues();
        cv.put(DemographicsContract._ID,demographics.getId());
        cv.put(DemographicsContract.BIRTH_DATE,demographics.getBirthDate());
        cv.put(DemographicsContract.GENDER, demographics.getGender());
        cv.put(DemographicsContract.VACATION, demographics.getVacation());
        cv.put(DemographicsContract.MARITAL_STATUS,demographics.getMaritalStatus());
        cv.put(DemographicsContract.ETHNICITY, demographics.getEthnicity());
        cv.put(DemographicsContract.EDUCATION, demographics.getEducation());
        cv.put(DemographicsContract.WORK,demographics.getWork());
        SQLiteDatabase db = this.getWritableDatabase();
        //String[] args = {String.valueOf(demographics.getId())};
        db.insert(DemographicsContract.TABLE_NAME, null, cv);
        Log.i("Demo","Update successful");
        db.close();
    }


}
