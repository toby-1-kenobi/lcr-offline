package org.sil.lcroffline.authentication;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;
import org.sil.lcroffline.BuildConfig;
import org.sil.lcroffline.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.HttpsURLConnection;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AccountAuthenticatorActivity {

    private final String LOG_TAG = LoginActivity.class.getSimpleName();

    public static final String NEEDS_TOKEN_KEY = "needs_token";

    private AccountManager mAccountManager;
    private String mAccountName;
    private String mAccountType;

    // if the activity needs to return an authenticity token to the authenticator
    // then this will be set to true in the onCreate() method
    private boolean mNeedsToken;

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private EditText mPhoneView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(LOG_TAG, "create");

        mAccountManager = AccountManager.get(getBaseContext());
        mAccountType = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        mNeedsToken = getIntent().getBooleanExtra(NEEDS_TOKEN_KEY, true);

        setContentView(R.layout.activity_login);

        // Set up the login form.
        mPhoneView = (EditText) findViewById(R.id.phone);
        // if the account name is being passed in we put it in the phone field.
        mPhoneView.setText(getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_NAME));

        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private boolean validateForm() {
        // Reset errors.
        mPhoneView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mAccountName = mPhoneView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean valid = true;
        View focusView = null;

        // Check for a valid password
        if (TextUtils.isEmpty(password)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            valid = false;
        } else if (!isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            valid = false;
        }

        // Check for a valid phone address.
        if (TextUtils.isEmpty(mAccountName)) {
            mPhoneView.setError(getString(R.string.error_field_required));
            focusView = mPhoneView;
            valid = false;
        } else if (!isPhoneValid(mAccountName)) {
            mPhoneView.setError(getString(R.string.error_invalid_phone));
            focusView = mPhoneView;
            valid = false;
        }

        if (!valid) {
            // There was an error; focus the first
            // form field with an error.
            focusView.requestFocus();
        }

        return valid;

    }




    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }
        if(validateForm()) {
            String password = mPasswordView.getText().toString();
            if (isNetworkAvailable()) {
                // Show a progress spinner, and kick off a background task to
                // perform the user login attempt.
                showProgress(true);
                mAuthTask = new UserLoginTask(mAccountName, password);
                mAuthTask.execute((Void) null);
            } else {
                Log.i(LOG_TAG, "Offline Login");
                // without a network connection we try to authenticate with
                // stored credentials that were previously used successfully
                SharedPreferences userCredPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.user_cred_preference_file_key),
                        Context.MODE_PRIVATE
                );
                if (userCredPref.contains(mAccountName)) {
                    String stored_password = userCredPref.getString(mAccountName, null);
                    Log.d(LOG_TAG, "stored password: " + stored_password);
                    boolean match;
                    if (userCredPref.getBoolean(mAccountName + getString(R.string.password_is_hashed_key), true)) {
                        try {
                            Log.d(LOG_TAG, "password hash: " + sha1(password, mAccountName));
                            match = stored_password.contentEquals(sha1(password, mAccountName));
                        } catch (NoSuchAlgorithmException e) {
                            Log.wtf(LOG_TAG, "SHA1 algorithm not available, yet this branch of code indicates we've used it before", e);
                            match = stored_password.contentEquals(password);
                        }
                    } else {
                        match = stored_password.contentEquals(password);
                    }
                    if (match) {
                        // correct credentials
                        // fetch existing token from storage
                        String token = userCredPref.getString(mAccountName + getString(R.string.jwt_key), null);
                        // if there's no token and we need to have one, put an error into the response
                        // use network error code because we're trying to get the token without network access
                        // and we haven't got it.
                        if (mNeedsToken) {
                            Bundle result = new Bundle();
                            result.putString(AccountManager.KEY_ACCOUNT_NAME, mAccountName);
                            result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
                            result.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_NETWORK_ERROR);
                            result.putString(AccountManager.KEY_ERROR_MESSAGE, "No existing session, and no network connection.");
                            setAccountAuthenticatorResult(result);
                        }

                        onSuccessfulLogin(token);
                    } else {
                        // bad password
                        mPasswordView.setError(getString(R.string.error_incorrect_password));
                        mPasswordView.requestFocus();
                    }
                } else {
                    // user credentials not stored
                    mPhoneView.setError(getString(R.string.connection_required));
                    mPhoneView.requestFocus();
                }
            }
        }
    }

    private boolean isPhoneValid(String phone) {
        String phoneStripped = phone.replaceAll("[^0-9]", "");
        return phoneStripped.length() >= 10 && phoneStripped.length() <= 12;
    }

    private boolean isPasswordValid(String password) {
        return password.length() >= 6;
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void onSuccessfulLogin(String token) {
        // if we're fetching the token and we don't have one
        // then don't set the response here
        if (token == null && mNeedsToken) {
            finish();
            return;
        }
        // return response to the authenticator
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, mAccountName);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
        result.putString(AccountManager.KEY_AUTHTOKEN, token);
        setAccountAuthenticatorResult(result);
        finish();
    }

    private String sha1(String data, String salt) throws NoSuchAlgorithmException {
        data = salt + data;
        MessageDigest md = MessageDigest.getInstance("SHA1");
        md.update(data.getBytes());
        return bytesToHex(md.digest());
    }

    private String bytesToHex(byte[] bytes) {
        char hexDigit[] = {'0', '1', '2', '3', '4', '5', '6', '7',
                '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        StringBuffer buf = new StringBuffer();
        for (int j=0; j<bytes.length; j++) {
            buf.append(hexDigit[(bytes[j] >> 4) & 0x0f]);
            buf.append(hexDigit[bytes[j] & 0x0f]);
        }
        return buf.toString();
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, String> {

        private final String LOG_TAG = UserLoginTask.class.getSimpleName();

        private final String mPhone;
        private final String mPassword;

        UserLoginTask(String phone, String password) {
            mPhone = phone;
            mPassword = password;
        }

        @Override
        protected String doInBackground(Void... params) {

            // put together the post request
            JSONObject jsonCredentials = new JSONObject();
            JSONObject jsonRoot = new JSONObject();
            try {
                jsonCredentials.put("phone", mPhone);
                jsonCredentials.put("password", mPassword);
                jsonRoot.put("auth", jsonCredentials);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error building authentication post request ", e);
                return null;
            }
            String postData = jsonRoot.toString();

            // These need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpsURLConnection urlConnection = null;
            BufferedWriter postWriter = null;
            BufferedReader responseReader = null;

            // Will contain the raw JSON response as a string.
            String tokenJsonStr = null;

            try {

                Uri builtUri = Uri.parse(BuildConfig.LCR_URL).buildUpon().appendPath("knock").appendPath("auth_token").build();
                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "connecting to " + url.toString());

                // connect to LCR
                urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setDoOutput(true);
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setFixedLengthStreamingMode(postData.getBytes("UTF-8").length);
                urlConnection.connect();

                // post the user credentials
                postWriter = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
                postWriter.write(postData);
                postWriter.flush();
                postWriter.close();

                // check the response
                int responseCode  = urlConnection.getResponseCode();
                if (responseCode >= 200 && responseCode < 300) {
                    // successful response
                    Log.d(LOG_TAG, "server responses success " + responseCode);
                    // next read the token
                    InputStream inputStream = urlConnection.getInputStream();
                    if (inputStream == null) {
                        // token is missing
                        Log.e(LOG_TAG, "Authentication token not received");
                        return null;
                    }
                    responseReader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuffer buffer = new StringBuffer();

                    String line;
                    while ((line = responseReader.readLine()) != null) {
                        buffer.append(line).append("\n");
                    }

                    if (buffer.length() == 0) {
                        // Stream was empty.  No token here.
                        Log.e(LOG_TAG, "Authentication token not received");
                        return null;
                    }
                    tokenJsonStr = buffer.toString();
                } else if (responseCode >= 400 && responseCode < 500) {
                    // server error response means the authentication didn't go through
                    Log.d(LOG_TAG, "server error " + responseCode);
                    return null;
                } else {
                    Log.e(LOG_TAG, "Unexpected server response code: " + responseCode);
                    return null;
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
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
                JSONObject tokenJson = new JSONObject(tokenJsonStr);
                return tokenJson.getString("jwt");
            } catch (JSONException e) {
                Log.e(LOG_TAG, "Error fetching token from JSON", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(final String token) {
            mAuthTask = null;
            showProgress(false);

            Log.d(LOG_TAG, "token: " + token);

            if (token == null) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else {
                // save the token and credentials
                SharedPreferences sharedPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.preference_file_key),
                        Context.MODE_PRIVATE
                );
                sharedPref
                        .edit()
                        .putString(getString(R.string.current_user_phone_key), mPhone)
                        .putString(getString(R.string.jwt_key), token)
                        .apply();

                SharedPreferences userCredPref = getApplicationContext().getSharedPreferences(
                        getString(R.string.user_cred_preference_file_key),
                        Context.MODE_PRIVATE
                );
                SharedPreferences.Editor userCredEdit = userCredPref.edit();
                userCredEdit.putString(mPhone + getString(R.string.jwt_key), token);
                // password should be hashed when stored
                // use the phone number as salt
                String hashedPassword = null;
                try {
                    hashedPassword = sha1(mPassword, mPhone);
                    userCredEdit.putString(mPhone, hashedPassword);
                    userCredEdit.putBoolean(mPhone + getString(R.string.password_is_hashed_key), true);
                } catch (NoSuchAlgorithmException e) {
                    // This is quite unexpected that the SHA1 algorithm is not available
                    // Instead of crashing we'll risk storing the password in shared preferences without hashing
                    Log.w(LOG_TAG, "cannot apply SHA1 so password is stored in plain text", e);
                    userCredEdit.putString(mPhone, mPassword);
                    userCredEdit.putBoolean(mPhone + getString(R.string.password_is_hashed_key), false);
                }
                userCredEdit.apply();

                onSuccessfulLogin(token);
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}

