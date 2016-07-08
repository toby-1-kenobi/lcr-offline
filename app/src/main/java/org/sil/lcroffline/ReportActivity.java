package org.sil.lcroffline;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

public class ReportActivity extends AppCompatActivity {

    private DatabaseHelper mDBHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report);

        mDBHelper = new DatabaseHelper(this);

        SharedPreferences sharedPref = this.getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        );
        // get current user phone number from shared preferences
        String phone = sharedPref.getString(getString(R.string.current_user_phone_key), null);

        Spinner state_select = (Spinner) findViewById(R.id.report_state);
        SimpleCursorAdapter stateAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_spinner_item,
                mDBHelper.getUserStates(phone),
                new String[] {DatabaseHelper.STATE_NAME_FIELD},
                new int[] {android.R.id.text1}
        );
        stateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        state_select.setAdapter(stateAdapter);
    }
}
