package databasemanager;

import android.util.Log;

/**
 * Created by Ara on 4/30/15.
 */
public class Demographics {
    private int id;
    private String birthdate;
    private String gender;
    private  String vacation;
    private  String marital_status;
    private  String ethnicity;
    private  String education;
    private String work;
    public static final String COMMA =",";


    public Demographics(){}
    public Demographics(int id, String birthdate, String gender,String vacation,String marital_status,String ethnicity,String education,String work){
        this.id=id;
        this.birthdate=birthdate;
        this.gender=gender;
        this.vacation = vacation;
        this.marital_status=marital_status;
        this.ethnicity = ethnicity;
        this.education=education;
        this.work=work;
        Log.i("DEMOGR", marital_status + ", " );

    }

    public Demographics(int id, String[] demographics){
        this.id = id;
        this.birthdate=demographics[0];
        this.gender=demographics[1];
        this.vacation=demographics[2];
        this.marital_status=demographics[3];
        this.ethnicity=demographics[4];
        this.education=demographics[5];
        this.work=demographics[6];
        Log.i("DEMOGR", marital_status + ", " + demographics[3]);
    }


    public int getId() {
        return id;
    }

    public String getBirthDate() {
        return birthdate;
    }

    public String getGender() {
        return gender;
    }

    public String getVacation() {
        return vacation;
    }

    public String getWork() {
        return work;
    }

    public String getEducation() {
        return education;
    }

    public String getEthnicity() {
        return ethnicity;
    }

    public String getMaritalStatus() {
        return marital_status;
    }

    public String toString(){

        return birthdate + COMMA+gender + COMMA + vacation + COMMA + marital_status+ COMMA + ethnicity  + COMMA + education +COMMA+work  ;
    }
}
