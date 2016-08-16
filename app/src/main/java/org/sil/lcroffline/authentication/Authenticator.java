package org.sil.lcroffline.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by toby on 12/08/16.
 */
public class Authenticator extends AbstractAccountAuthenticator {

    private final String LOG_TAG = Authenticator.class.getSimpleName();

    Context mContext;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle addAccount(
            AccountAuthenticatorResponse response,
            String accountType,
            String authTokenType,
            String[] requiredFeatures,
            Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, "Adding new account");

        // We're going to use a LoginActivity to talk to the user
        final Intent intent = new Intent(mContext, LoginActivity.class);

        // configure that activity via the intent
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(LoginActivity.KEY_CONFIRM_CRED_ONLY, false);

        // It will also need to know how to send its response to the account manager
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);

        return referToLoginActivity(intent);
    }

    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse response,
            Account account,
            Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, "Confirming account credentials");
        for (String option_key: options.keySet()) {
            Log.d(LOG_TAG, option_key);
        }
        return referToLoginActivity(response, account, true);
    }

    @Override
    public Bundle getAuthToken(
            AccountAuthenticatorResponse response,
            Account account,
            String authTokenType,
            Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, "Getting Authentication Token");
        return referToLoginActivity(response, account, false);
    }

    private Bundle referToLoginActivity(
            AccountAuthenticatorResponse response,
            Account account,
            boolean confirmCred) {

        // We're going to use a LoginActivity to talk to the user
        final Intent intent = new Intent(mContext, LoginActivity.class);

        // configure that activity via the intent
        intent.putExtra(LoginActivity.KEY_ACCOUNT, account);
        intent.putExtra(LoginActivity.KEY_CONFIRM_CRED_ONLY, confirmCred);

        // It will also need to know how to send its response to the account manager
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);

        return referToLoginActivity(intent);

    }

    private Bundle referToLoginActivity(Intent intent) {
        final Bundle bundle = new Bundle();
        // Wrap up this intent, and return it, which will cause the
        // intent to be run
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
    }


    @Override
    public String getAuthTokenLabel(String authTokenType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle updateCredentials(
            AccountAuthenticatorResponse response,
            Account account, String authTokenType,
            Bundle options
    ) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Bundle hasFeatures(
            AccountAuthenticatorResponse response,
            Account account,
            String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException();
    }
}
