package org.sil.lcroffline.authentication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by toby on 15/08/16.
 */
public class AuthenticatorService extends Service {

    Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        mAuthenticator = new Authenticator(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
