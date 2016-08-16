package org.sil.lcroffline.authentication;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
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
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AccountAuthenticatorActivity {

    private final String LOG_TAG = LoginActivity.class.getSimpleName();

    // keys for values in the incoming intent
    public static final String KEY_CONFIRM_CRED_ONLY = "confirm_credentials";
    public static final String KEY_ACCOUNT = "account";

    // keys for user values put into the account manager
    public static final String KEY_USERNAME = "username";
    public static final String KEY_HASHED_PASSWORD = "hashed_password";
    public static final String KEY_HASH_SALT = "hash_salt";

    // we use a random number as part of the password hash salt
    private Random mRandom;

    private AccountManager mAccountManager;
    private Account mAccount;
    private String mAccountName;
    private String mAccountType;

    // if the activity only needs to confirm user credentials
    // then this will be set to true in the onCreate() method
    private boolean mConfirmCredentials;

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
        mRandom = new Random();

        setContentView(R.layout.activity_login);

        // get references to form fields.
        mPhoneView = (EditText) findViewById(R.id.phone);
        mPasswordView = (EditText) findViewById(R.id.password);
        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        // adjust layout according to data in intent
        mAccount = getIntent().getParcelableExtra(KEY_ACCOUNT);
        if (mAccount != null) {
            mAccountName = mAccount.name;
            mAccountType = mAccount.type;
            // if the account name is being passed in we put it in the phone field.
            mPhoneView.setText(mAccountName);
            // hide the phone field and show username and enter password message
            mPhoneView.setVisibility(View.GONE);
            View enterPassMsg = findViewById(R.id.enterPassMsg);
            enterPassMsg.setVisibility(View.VISIBLE);
            String username = mAccountManager.getUserData(mAccount, KEY_USERNAME);
            TextView usernameView = (TextView) findViewById(R.id.userName);
            if (username != null) {
                usernameView.setText(username + " (" + mAccountName + ")");
            } else {
                usernameView.setText("Phone: " + mAccountName);
            }
        } else {
            mAccountType = getIntent().getStringExtra(AccountManager.KEY_ACCOUNT_TYPE);
        }
        mConfirmCredentials = getIntent().getBooleanExtra(KEY_CONFIRM_CRED_ONLY, false);

        // set handlers on events
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    if (mConfirmCredentials) {
                        validateCredentials();
                    } else {
                        attemptLogin();
                    }
                    return true;
                }
                return false;
            }
        });

        Button mSignInButton = (Button) findViewById(R.id.sign_in_button);
        mSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mConfirmCredentials) {
                    validateCredentials();
                } else {
                    attemptLogin();
                }
            }
        });
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

        // Check for a valid phone number.
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

    private void validateCredentials() {
        Log.d(LOG_TAG, "validating credentials");
        if (mAccount == null) {
            Log.wtf(LOG_TAG, "we're trying to validate credentials without an account object");
            onFailure(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "must pass account to validate credentials");
        }

        // get the hashed password
        String hashedPassword = mAccountManager.getUserData(mAccount, KEY_HASHED_PASSWORD);
        if (hashedPassword == null) {
            // if we haven't got a stored password we have to validate with the server
            attemptLogin();
            return;
        }
        if (validateForm()) {
            // get the salt
            String salt = mAccountManager.getUserData(mAccount, KEY_HASH_SALT);
            // get the user provided password
            String password = mPasswordView.getText().toString();
            // compare
            try {
                if (hashedPassword.contentEquals(sha1(password, mAccountName + salt))) {
                    onSuccessfulLogin(null);
                } else {
                    // return negative response to the authenticator
                    Bundle result = new Bundle();
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, mAccountName);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
                    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
                    setAccountAuthenticatorResult(result);
                    finish();
                }
            } catch (NoSuchAlgorithmException e) {
                Log.e(LOG_TAG, "SHA1 algorithm not found when trying to hash a password!");
                onFailure(AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION, "no SHA1 algorithm available");
            }
        }

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
                // there's no network connection
                // and there's no account object (we must be trying to create an account)
                onFailure(AccountManager.ERROR_CODE_NETWORK_ERROR, "No existing account and no network connection.");
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
        // return response to the authenticator
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, mAccountName);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
        result.putString(AccountManager.KEY_AUTHTOKEN, token);
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, true);
        setAccountAuthenticatorResult(result);
        finish();
    }

    private void onFailure(int errorCode, String errorMessage) {
        Log.e(LOG_TAG, errorMessage);
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, mAccountName);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, mAccountType);
        result.putInt(AccountManager.KEY_ERROR_CODE, errorCode);
        result.putString(AccountManager.KEY_ERROR_MESSAGE, errorMessage);
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

    private void storeHashedPassword(Account account, String password) throws NoSuchAlgorithmException {
        // password should be hashed when stored
        // use a salt of the phone number plus a random long number
        String hashedPassword = null;
        String salt = String.valueOf(mRandom.nextLong());
        mAccountManager.setUserData(account, KEY_HASH_SALT, salt);
        hashedPassword = sha1(password, account.name + salt);
        mAccountManager.setUserData(account, KEY_HASHED_PASSWORD, hashedPassword);
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
                    Log.e(LOG_TAG, "server error: " + responseCode);
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
                // we've successfully retrieved the token from the server.
                // set the account name
                mAccountName = mPhone;

                // save the password as a hash
                try {
                    storeHashedPassword(mAccount, mPassword);
                } catch (NoSuchAlgorithmException e) {
                    // This is quite unexpected that the SHA1 algorithm is not available
                    // in this case we wont store the password
                    Log.w(LOG_TAG, "cannot apply SHA1 - algorithm not available.", e);
                }
                // and pass on the token
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

