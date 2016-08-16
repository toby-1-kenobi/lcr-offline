package org.sil.lcroffline;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import org.sil.lcroffline.authentication.LoginActivity;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements ChooseAccountDialog.ChooseAccountDialogListener {

    private final String LOG_TAG = MainActivity.class.getSimpleName();
    static final int NEW_REPORT_REQUEST = 1;
    AccountManager mAccountManager;
    private Account mAccount;

    // if we've authenticated the user in the last 10 minutes we don't need to again
    private final long AUTHENTICATION_EXPIRY_MILLISECONDS = TimeUnit.MINUTES.toMillis(10);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent reportIntent = new Intent(getApplicationContext(), ReportActivity.class);
                startActivityForResult(reportIntent, NEW_REPORT_REQUEST);
            }
        });

        mAccountManager = AccountManager.get(getApplicationContext());
        Account[] accounts = mAccountManager.getAccountsByType(getString(R.string.LCIR_account_type));
        if (accounts.length == 0) {
            // there are no LCIR accounts so the user must make one.
            mAccountManager.addAccount(getString(R.string.LCIR_account_type), null, null, null, this,
                    new AccountManagerCallback<Bundle>() {
                        public void run(AccountManagerFuture<Bundle> future) {
                            Bundle addAccountResult;
                            try {
                                // Get the authenticator result, it blocks the thread until the
                                // account authenticator completes
                                addAccountResult = future.getResult();
                                mAccount = new Account(
                                        addAccountResult.getString(AccountManager.KEY_ACCOUNT_NAME),
                                        addAccountResult.getString(AccountManager.KEY_ACCOUNT_TYPE));
                                Log.d(LOG_TAG, "account created: " + addAccountResult.getString(AccountManager.KEY_ACCOUNT_NAME));
                            } catch (OperationCanceledException e) {
                                Log.i(LOG_TAG, "No LCIR account. Account creation cancelled");
                                finish();
                            } catch (IOException e) {
                                Log.e(LOG_TAG, "No LCIR account. Account creation IO problem", e);
                                finish();
                            } catch (AuthenticatorException e) {
                                Log.e(LOG_TAG, "No LCIR account. Account creation authentication problem", e);
                                finish();
                            }
                        }
                    }, null);
        } else if (accounts.length == 1) {
            // easy: there's only one LCIR account so that's what we're using
            mAccount = accounts[0];
        } else {
            // more than one LCIR account on the device.
            // ask the user to choose.
            ChooseAccountDialog chooseAccountDialog = new ChooseAccountDialog();
            Bundle args = new Bundle();
            args.putParcelableArray(ChooseAccountDialog.KEY_ACCOUNTS_ARRAY, accounts);
            chooseAccountDialog.setArguments(args);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == NEW_REPORT_REQUEST) {
            if (resultCode == RESULT_OK) {
                ReportManagerFragment reportManager =
                        (ReportManagerFragment) getSupportFragmentManager().
                                findFragmentById(R.id.report_manager_fragment);
                reportManager.incrementQueuedReports();
            }
        }
    }

    @Override
    public void onDialogListItemClick(Account selected) {
        mAccount = selected;
    }

    @Override
    public void onStart() {
        super.onStart();
        // user returning to the app
        // we want to validate their credentials (if the account is set up)
        // so if someone else picks up the phone they don't see private data.
        if (mAccount != null) {
            // don't do this if we've recently done it
            long lastAuthenticated = getApplicationContext().getSharedPreferences(
                    getString(R.string.user_cred_preference_file_key), Context.MODE_PRIVATE)
                    .getLong(LoginActivity.KEY_LAST_AUTHENTICATED, 0L);
            Log.d(LOG_TAG, "last authenticated: " + lastAuthenticated);
            long millisSinceLastAuthenticated = new Date().getTime() - lastAuthenticated;
            if (millisSinceLastAuthenticated > AUTHENTICATION_EXPIRY_MILLISECONDS) {
                mAccountManager.confirmCredentials(mAccount, null, this,
                        new AccountManagerCallback<Bundle>() {
                            public void run(AccountManagerFuture<Bundle> future) {
                                Bundle confirmCredResult;
                                try {
                                    confirmCredResult = future.getResult();
                                    if (!confirmCredResult.getBoolean(AccountManager.KEY_BOOLEAN_RESULT)) {
                                        Log.e(LOG_TAG, "user credentials not valid");
                                        finish();
                                    }
                                } catch (OperationCanceledException e) {
                                    Log.i(LOG_TAG, "User credentials not confirmed: operation cancelled");
                                    finish();
                                } catch (IOException e) {
                                    Log.e(LOG_TAG, "User credentials not confirmed: IO problem", e);
                                    finish();
                                } catch (AuthenticatorException e) {
                                    Log.e(LOG_TAG, "User credentials not confirmed: authentication problem", e);
                                    finish();
                                }
                            }
                        }, null);
            }
        }
    }

}
