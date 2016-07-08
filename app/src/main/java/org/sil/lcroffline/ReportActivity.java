package org.sil.lcroffline;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.NavUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;

public class ReportActivity extends AppCompatActivity
        implements AdapterView.OnItemSelectedListener,
        SelectLanguagesDialogFragment.SelectLanguagesDialogListener,
        AdapterView.OnItemClickListener,
        View.OnClickListener,
        DatePickerDialog.OnDateSetListener {

    private final String LOG_TAG = ReportActivity.class.getSimpleName();
    private DatabaseHelper mDBHelper;

    // initial value is -1 to show no state has been selected yet
    private long mSelectedStateID = -1;
    private ArrayAdapter<CharSequence> mLanguagesArray;

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

        ArrayList<CharSequence> langArray = new ArrayList<CharSequence>();
        langArray.add(getString(R.string.pick_languages));
        mLanguagesArray = new ArrayAdapter<CharSequence>(
                this,
                android.R.layout.simple_list_item_1,
                langArray
        );
        ListView languagesList = (ListView) findViewById(R.id.report_languages);
        languagesList.setAdapter(mLanguagesArray);
        languagesList.setOnItemClickListener(this);

        TextView dateText = (TextView) findViewById(R.id.report_date);
        dateText.setOnClickListener(this);

        Button cancelButton = (Button) findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(this);

        Button okButton = (Button) findViewById(R.id.ok_button);
        okButton.setOnClickListener(this);

    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Log.d(LOG_TAG, "state id: " + id);
        // when a state is selected open the dialog to select languages
        // only if the state has changed and this isn't the first time the sate is selected
        // (because that happens in the layout inflation)
        if (mSelectedStateID != -1 && id != mSelectedStateID) {
            DialogFragment selectLanguages = SelectLanguagesDialogFragment.newInstance(id);
            selectLanguages.show(getSupportFragmentManager(), getString(R.string.languages_dialog_tag));
        }
        mSelectedStateID = id;
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        Log.d(LOG_TAG, "no states selected");
        // Remove all languages from list
        mLanguagesArray.clear();
        mLanguagesArray.add(getString(R.string.pick_languages));
        mLanguagesArray.notifyDataSetChanged();
    }

    @Override
    public void onDialogPositiveClick(CharSequence[] selectedLanguageNames) {
        Log.d(LOG_TAG, "languages selected: " + Arrays.toString(selectedLanguageNames));
        // Add the languages into list
        mLanguagesArray.clear();
        if (selectedLanguageNames.length == 0) {
            mLanguagesArray.add(getString(R.string.pick_languages));
        } else {
            for (CharSequence languageName : selectedLanguageNames) {
                mLanguagesArray.add(languageName);
            }
        }
        mLanguagesArray.notifyDataSetChanged();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        DialogFragment selectLanguages = SelectLanguagesDialogFragment.newInstance(mSelectedStateID);
        selectLanguages.show(getSupportFragmentManager(), getString(R.string.languages_dialog_tag));
    }

    @Override
    public void onClick(View v) {
        if (v == findViewById(R.id.report_date)) {
            DialogFragment newFragment = new DatePickerFragment();
            newFragment.show(getSupportFragmentManager(), "datePicker");
        } else if (v == findViewById(R.id.cancel_button)) {
            Log.d(LOG_TAG, "cancel button clicked");
            finish();
        } else if (v == findViewById(R.id.ok_button)) {
            Log.d(LOG_TAG, "OK button clicked");
            finish();
        }
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        TextView dateText = (TextView) findViewById(R.id.report_date);
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);
        SimpleDateFormat myDateFormat = new SimpleDateFormat("d MMM yyyy");
        dateText.setText(myDateFormat.format(selectedDate.getTime()));
    }
}
