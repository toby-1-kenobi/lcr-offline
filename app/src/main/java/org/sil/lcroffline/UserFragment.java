package org.sil.lcroffline;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.sil.lcroffline.data.DatabaseHelper;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import static org.sil.lcroffline.data.DatabaseContract.*;


/**
 * A simple {@link Fragment} subclass.
 * Represents the user
 */
public class UserFragment extends Fragment {

    private final String LOG_TAG = UserFragment.class.getSimpleName();


    // These Strings must correspond to the keys specified in LCR in the :me action of the users controller
    // https://github.com/toby-1-kenobi/sag_reporter/blob/develop/app/controllers/users_controller.rb
    public static final String LCR_USER_KEY_ID = "id";
    public static final String LCR_USER_KEY_NAME = "name";
    public static final String LCR_USER_KEY_PHONE = "phone";
    public static final String LCR_USER_KEY_STATES = "geo_states";
    public static final String LCR_STATE_KEY_ID = "id";
    public static final String LCR_STATE_KEY_NAME = "name";
    public static final String LCR_STATE_KEY_LANGUAGES = "languages";
    public static final String LCR_LANGUAGE_KEY_ID = "id";
    public static final String LCR_LANGUAGE_KEY_NAME = "language_name";

    private static final int DATA_EXPIRATION_DAYS = 1;

    private Date mUpdated;
    private Account mAccount;

    private DatabaseHelper mDBHelper;
    private View rootView;

    private List<UserDataListener> userDataListeners = new ArrayList<UserDataListener>();

    public UserFragment() {
        // Required empty public constructor
    }

    public interface UserDataListener {
        public void userUpdated(long userID);
    }

    public void addUserDataListener(UserDataListener listener) {
        userDataListeners.add(listener);
        Log.d(LOG_TAG, "Got user data listener");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDBHelper = new DatabaseHelper(getContext());
        loadAccount();
    }

    private boolean loadAccount() {
        SharedPreferences pref = getActivity().getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
        String accountName = pref.getString(getString(R.string.key_account_name), null);
        if (accountName != null) {
            mAccount = new Account(accountName, getString(R.string.LCIR_account_type));
            return true;
        } else {
            return false;
        }
    }

    private long GetUserFromDatabase(String phone, View rootView) {
        Cursor result = mDBHelper.getUser(phone);
        if (!result.moveToFirst()) return -1; // user not found
        int idIndex, nameIndex, updatedIndex;
        try {
            idIndex = result.getColumnIndexOrThrow(UserEntry._ID);
            nameIndex = result.getColumnIndexOrThrow(UserEntry.COLUMN_NAME);
            updatedIndex = result.getColumnIndexOrThrow(UserEntry.COLUMN_UPDATED);
        } catch (IllegalArgumentException e) {
            Log.e(LOG_TAG, "Cannot get user data from db - fields missing in db response.", e);
            return -1;
        }
        TextView textView = (TextView) rootView.findViewById(R.id.user_name);
        textView.setText(result.getString(nameIndex));
        try {
            mUpdated = DatabaseHelper.SQLITE_DATE_FORMAT.parse(result.getString(updatedIndex));
        } catch (ParseException e) {
            Log.e(LOG_TAG, "could not parse user updated date from database: " + result.getString(updatedIndex), e);
        }
        long userID = result.getLong(idIndex);
        result.close();
        return userID;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_user, container, false);

        if (mAccount == null) loadAccount();

        if (mAccount != null) {
            // use the phone number to fetch more user data from db
            long userId = GetUserFromDatabase(mAccount.name, rootView);
            if (userId == -1) {
                Log.d(LOG_TAG, "could not retrieve user fom db with phone " + mAccount.name);
            } else {
                // and save the user id to shared pref
                getContext().getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
                        .edit()
                        .putLong(getString(R.string.current_user_id_key), userId)
                        .apply();
            }
        }
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "fragment started");
        // if the data is old or missing, update it from online if possible
        if (isNetworkAvailable()) {
            Log.d(LOG_TAG, "user data last updated: " + mUpdated);
            if (mUpdated == null || mUpdated.before(subtractDays(new Date(), DATA_EXPIRATION_DAYS))) {
                if (mAccount != null) {
                    FetchUserTask fetchTask = new FetchUserTask();
                    fetchTask.execute();
                }
            }
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public static Date subtractDays(Date date, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, days * -1);
        return cal.getTime();
    }

    private class FetchUserTask extends AsyncTask<Void, Void, JSONObject> {

        @Override
        protected JSONObject doInBackground(Void... params) {
            // These need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpsURLConnection urlConnection = null;
            BufferedWriter postWriter = null;
            BufferedReader responseReader = null;

            // Will contain the raw JSON response as a string.
            String resultJsonStr = null;

            if (mAccount == null && !loadAccount()) return null;

            try {

                String token = AccountManager.get(getContext()).blockingGetAuthToken(mAccount, getString(R.string.auth_token_type), false);

                Uri builtUri = Uri.parse(BuildConfig.LCR_URL).buildUpon().appendPath("users").appendPath("me").build();
                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "connecting to " + url.toString());

                // connect to LCR
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Authorization", "Bearer " + token);
                urlConnection.connect();

                // check the response
                int responseCode  = urlConnection.getResponseCode();
                if (responseCode == 401) {
                    Log.d(LOG_TAG, "server denied authorisation, invalidating token and trying again.");
                    AccountManager accountManager = AccountManager.get(getContext());
                    accountManager.invalidateAuthToken(getString(R.string.LCIR_account_type), token);
                    token = accountManager.blockingGetAuthToken(mAccount, getString(R.string.auth_token_type), false);
                    urlConnection.disconnect();
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("GET");
                    urlConnection.setRequestProperty("Authorization", "Bearer " + token);
                    urlConnection.connect();
                    responseCode = urlConnection.getResponseCode();
                }
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
                    Log.e(LOG_TAG, "server error " + responseCode);
                    return null;
                } else {
                    Log.e(LOG_TAG, "Unexpected server response code: " + responseCode);
                    return null;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error fetching user data", e);
                return null;
            } catch (AuthenticatorException e) {
                Log.e(LOG_TAG, "Authentication Error fetching user data", e);
                return null;
            } catch (OperationCanceledException e) {
                Log.e(LOG_TAG, "Authentication Error fetching user data", e);
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
            if (userData == null) {
                Log.e(LOG_TAG, "failed to get user data from the server");
                return;
            }
            try {
                boolean success = mDBHelper.setUser(userData);
                long userID = (long) userData.getInt(UserFragment.LCR_USER_KEY_ID);
                if (!success) {
                    Log.e(LOG_TAG, "something went wrong with storing data about the user.");
                } else {
                    Log.d(LOG_TAG, "Got fresh user data, updating listeners.");
                    for(UserDataListener listener : userDataListeners) {
                        listener.userUpdated(userID);
                    }
                    mUpdated = new Date();
                }
                SharedPreferences sharedPref = getContext().getSharedPreferences(
                        getString(R.string.preference_file_key),
                        Context.MODE_PRIVATE
                );
                sharedPref
                        .edit()
                        .putLong(getString(R.string.current_user_id_key), userID)
                        .apply();
                TextView textView = (TextView) rootView.findViewById(R.id.user_name);
                textView.setText(userData.getString("name"));
            } catch (JSONException e) {
                Log.e(LOG_TAG, "could not decode fetched user data JSON", e);
            }

        }
    }
}
