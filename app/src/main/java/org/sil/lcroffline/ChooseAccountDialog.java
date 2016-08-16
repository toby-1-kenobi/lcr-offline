package org.sil.lcroffline;

import android.accounts.Account;
import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

/**
 * Created by toby on 16/08/16.
 */
public class ChooseAccountDialog extends DialogFragment {

    public static final String KEY_ACCOUNTS_ARRAY = "accounts_array";

    String[] accountNames;
    Account[] mAccounts;

    public ChooseAccountDialog() {
        Bundle args = getArguments();
        mAccounts = (Account[]) args.getParcelableArray(KEY_ACCOUNTS_ARRAY);
        accountNames = new String[mAccounts.length];
        for (int i = 0; i < mAccounts.length; ++i) {
            accountNames[i] = mAccounts[i].name;
        }
    }

    public interface ChooseAccountDialogListener {
        void onDialogListItemClick(Account selected);
    }

    // Use this instance of the interface to deliver action events
    ChooseAccountDialogListener mListener;

    // Override the Fragment.onAttach() method to instantiate the ChooseAccountDialogListener
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            mListener = (ChooseAccountDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement ChooseAccountDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.pick_account)
                .setItems(accountNames, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        mListener.onDialogListItemClick(mAccounts[which]);
                    }
                });
        return builder.create();
    }
}
