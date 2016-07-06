package org.sil.lcroffline;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;


/**
 * A simple {@link Fragment} subclass.
 * Represents the user
 */
public class UserFragment extends Fragment {

    private final String LOG_TAG = UserFragment.class.getSimpleName();

    private static final String ARG_NAME = "name";
    private static final String ARG_UPDATED = "name";

    private static final int DATA_EXPIRATION_DAYS = 1;

    private String mName;
    private Date mUpdated;

    DatabaseHelper mDBHelper;

    public UserFragment() {
        // Required empty public constructor
    }
    /**
     * Parameters are fetched from local storage.
     * If the values fetched from local storage are not there or not recently updated,
     * and if there is an Internet connection and a valid JWT
     * they are fetched from online and updated in local storage
     *
     * @param savedInstanceState
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDBHelper = new DatabaseHelper(getContext());
        SharedPreferences sharedPref = getContext().getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        );
        if (getArguments() == null) {
            // get current user phone number from shared preferences
            String phone = sharedPref.getString(getString(R.string.current_user_phone_key), null);
            if (phone != null) {
                Bundle args =getArgsFromDatabase(phone);
                if (args != null) setArguments(args);
            }
        }
        if (getArguments() != null) {
            mName = getArguments().getString(ARG_NAME);
            SimpleDateFormat dateParser = new SimpleDateFormat();
            try {
                mUpdated = dateParser.parse(getArguments().getString(ARG_UPDATED));
            } catch (ParseException e) {
                Log.e(LOG_TAG, "Could not parse Updated date for user " + mName, e);
            }
        }
        // if the data is old or missing, update it from online if possible
        if (mUpdated == null || mUpdated.after(subtractDays(new Date(), DATA_EXPIRATION_DAYS))) {
            String token = sharedPref.getString(getString(R.string.jwt_key), null);
            if (token != null) {
                FetchUserTask fetchTask = new FetchUserTask();
                fetchTask.execute(token);
            }
        }
    }

    private Bundle getArgsFromDatabase(String phone) {
        Bundle args = new Bundle();
        Cursor result = mDBHelper.getUser(phone);
        if (result.getCount() == 0) return null;
        args.putString(ARG_NAME, result.getString(result.getColumnIndexOrThrow(DatabaseHelper.USER_NAME_FIELD)));
        args.putString(ARG_UPDATED, result.getString(result.getColumnIndexOrThrow(DatabaseHelper.USER_UPDATED_FIELD)));
        return args;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_user, container, false);
    }

    public static Date subtractDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days * -1);
        return cal.getTime();
    }

    private class FetchUserTask extends AsyncTask<String, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(String... params) {// These need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpsURLConnection urlConnection = null;
            BufferedWriter postWriter = null;
            BufferedReader responseReader = null;

            // Will contain the raw JSON response as a string.
            String resultJsonStr = null;

            try {

                Uri builtUri = Uri.parse(BuildConfig.LCR_URL).buildUpon().appendPath("users").appendPath("me").build();
                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "connecting to " + url.toString());

                // connect to LCR
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Authorization", "Bearer " + params[0]);
                urlConnection.connect();

                // check the response
                int responseCode  = urlConnection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    // successful response
                    Log.d(LOG_TAG, "server responses success " + responseCode);
                    // next read the data
                    InputStream inputStream = urlConnection.getInputStream();
                    if (inputStream == null) {
                        // data is missing
                        Log.e(LOG_TAG, "User data not received");
                        return null;
                    }
                    responseReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuffer buffer = new StringBuffer();

                    String line;
                    while ((line = responseReader.readLine()) != null) {
                        buffer.append(line).append("\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No data here.
                        Log.e(LOG_TAG, "User data not received");
                        return null;
                    }
                    resultJsonStr = buffer.toString();
                } else if (responseCode >= 400 && responseCode < 500) {
                    // server error response means the authentication didn't go through
                    Log.d(LOG_TAG, "server error " + responseCode);
                    return null;
                } else {
                    Log.e(LOG_TAG, "Unexpected server response code: " + responseCode);
                    return null;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error fetching user data", e);
                return null;
            } finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (postWriter != null) {
                    try {
                        postWriter.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
                if (responseReader != null) {
                    try {
                        responseReader.close();
                    } catch (IOException e) {
                        Log.e(LOG_TAG, "Error closing stream", e);
                    }
                }
            }

            try {
                return new JSONObject(resultJsonStr);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error fetching user data from JSON", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject userData) {
            try {
                TextView textView = (TextView) findViewById(R.id.user_name);
                textView.setText(userData.getString("name"));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "could not decode fetched user data JSON", e);
            }

        }
    }
}
