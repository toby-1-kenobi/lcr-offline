package org.sil.lcroffline;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.DialogFragment;
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
import android.widget.Toast;

import org.sil.lcroffline.data.DatabaseContract;
import org.sil.lcroffline.data.DatabaseHelper;

import java.text.ParseException;
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
    private SimpleCursorAdapter mStateAdapter;

    private SimpleDateFormat myDateFormat = new SimpleDateFormat("d MMM yyyy");

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
        mStateAdapter = new SimpleCursorAdapter(
                this,
                android.R.layout.simple_spinner_item,
                mDBHelper.getUserStates(phone),
                new String[] {DatabaseContract.StateEntry.COLUMN_NAME},
                new int[] {android.R.id.text1}
        );
        mStateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        state_select.setAdapter(mStateAdapter);
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
    protected void onPause() {
        super.onPause();
        if (mStateAdapter != null) {
            mStateAdapter.getCursor().close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mStateAdapter != null && mDBHelper != null) {
            SharedPreferences sharedPref = this.getSharedPreferences(
                    getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
            );
            String phone = sharedPref.getString(getString(R.string.current_user_phone_key), null);
            mStateAdapter.changeCursor(mDBHelper.getUserStates(phone));
        }
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
            setResult(RESULT_CANCELED);
            finish();
        } else if (v == findViewById(R.id.ok_button)) {
            Log.d(LOG_TAG, "OK button clicked");
            SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                    getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
            );
            // get current user id number from shared preferences
            long userID = sharedPref.getLong(getString(R.string.current_user_id_key), -1);
            if (userID == -1) {
                Log.e(LOG_TAG, "Report not saved: Could not get current user ID from shared pref.");
                Toast.makeText(getApplicationContext(), "Report not saved - User unknown", Toast.LENGTH_SHORT).show();
                return;
            }
            // get language names from the list
            CharSequence[] languageNames = new CharSequence[mLanguagesArray.getCount()];
            for (int i = 0; i < mLanguagesArray.getCount(); ++i) {
                languageNames[i] = mLanguagesArray.getItem(i);
            }
            if (languageNames.length == 0 || getString(R.string.pick_languages).contentEquals(languageNames[0])) {
                Log.d(LOG_TAG, "Report not saved: No languages found in list.");
                Toast.makeText(getApplicationContext(), "Report not saved - needs languages", Toast.LENGTH_SHORT).show();
                return;
            }
            TextView contentView = (TextView) findViewById(R.id.report_content);
            if (contentView.getText().length() == 0) {
                Log.d(LOG_TAG, "Report not saved: No content.");
                Toast.makeText(getApplicationContext(), "Report not saved - needs content", Toast.LENGTH_SHORT).show();
                return;
            }
            TextView dateView = (TextView) findViewById(R.id.report_date);
            Date reportDate = null;
            try {
                reportDate = myDateFormat.parse(dateView.getText().toString());
                mDBHelper.createReport(userID, mSelectedStateID, languageNames, reportDate, contentView.getText());
                setResult(RESULT_OK);
                finish();
            } catch (ParseException e) {
                Log.d(LOG_TAG, "Report not saved: could not parse date: " + dateView.getText());
                Toast.makeText(getApplicationContext(), "Report not saved - date problem", Toast.LENGTH_SHORT).show();
            }

        }
    }

    public void onDateSet(DatePicker view, int year, int month, int day) {
        TextView dateText = (TextView) findViewById(R.id.report_date);
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.set(year, month, day);
        dateText.setText(myDateFormat.format(selectedDate.getTime()));
    }
}
