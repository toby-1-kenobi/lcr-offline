package org.sil.lcroffline.authentication;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.ContentValues;
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
        return referToLoginActivity(response, null, accountType, false);
    }

    @Override
    public Bundle confirmCredentials(
            AccountAuthenticatorResponse response,
            Account account,
            Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, "Confirming account credentials");
        return referToLoginActivity(response, account.name, account.type, false);
    }

    @Override
    public Bundle getAuthToken(
            AccountAuthenticatorResponse response,
            Account account,
            String authTokenType,
            Bundle options) throws NetworkErrorException {
        Log.d(LOG_TAG, "Getting Authentication Token");
        return referToLoginActivity(response, account.name, account.type, true);
    }

    private Bundle referToLoginActivity(
            AccountAuthenticatorResponse response,
            String accountName, String accountType,
            boolean needsToken) {

        final Bundle bundle = new Bundle();

        // We're going to use a LoginActivity to talk to the user
        final Intent intent = new Intent(mContext, LoginActivity.class);

        // configure that activity via the intent
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, accountName);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, accountType);
        intent.putExtra(LoginActivity.NEEDS_TOKEN_KEY, needsToken);

        // It will also need to know how to send its response to the account manager
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE,
                response);

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
