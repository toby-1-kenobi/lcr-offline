package org.sil.lcroffline;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;

import java.util.Arrays;

public class ReportActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener, SelectLanguagesDialogFragment.SelectLanguagesDialogListener {

    private final String LOG_TAG = ReportActivity.class.getSimpleName();
    private DatabaseHelper mDBHelper;

    // initial value is -1 to show no state has been selected yet
    private long mSelectedStateID = -1;

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
        state_select.setOnItemSelectedListener(this);
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "state id: " + id);
        // when a state is selected open the dialog to select languages
        // only if the state has changed and this isn't the first time the sate is selected
        // (because that happens in the layout inflation)
        if (mSelectedStateID != -1 && id != mSelectedStateID) {
            DialogFragment selectLanguages = SelectLanguagesDialogFragment.newInstance(id);
            selectLanguages.show(getSupportFragmentManager(), "select_languages");
        }
        mSelectedStateID = id;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(LOG_TAG, "no states selected");
        // Remove all languages from list
    }

    @Override
    public void onDialogPositiveClick(CharSequence[] selectedLanguageNames) {
        Log.d(LOG_TAG, "languages selected: " + Arrays.toString(selectedLanguageNames));
        // Add the languages into list
    }
}
