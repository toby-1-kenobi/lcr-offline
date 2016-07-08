package org.sil.lcroffline;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by toby on 8/07/16.
 */
public class SelectLanguagesDialogFragment extends DialogFragment {

    public interface SelectLanguagesDialogListener {
        public void onDialogPositiveClick(CharSequence[] selectedLanguageNames);
    }

    private long mSelectedStateID;
    private CharSequence[] mValidLanguageNames;
    private List<Integer> mSelectedLanguages;

    private DatabaseHelper mDBHelper;
    private SelectLanguagesDialogListener mListener;

    private static final String SELECTED_STATE_ARG_KEY = "selected_state";

    /**
     * Create a new instance of SelectLanguagesDialogFragment, providing "selectedStateID"
     * as an argument.
     */
    static SelectLanguagesDialogFragment newInstance(long selectedStateID) {
        SelectLanguagesDialogFragment f = new SelectLanguagesDialogFragment();

        // Supply num input as an argument.
        Bundle args = new Bundle();
        args.putLong(SELECTED_STATE_ARG_KEY, selectedStateID);
        f.setArguments(args);

        return f;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDBHelper = new DatabaseHelper(getContext());

        mSelectedStateID = getArguments().getLong(SELECTED_STATE_ARG_KEY);
        mSelectedLanguages = new ArrayList();  // Where we track the selected languages

        // get all valid language from storage based on state id
        mValidLanguageNames = mDBHelper.getLanguageNames(mSelectedStateID);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        builder.setTitle(R.string.pick_languages)
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected
                .setMultiChoiceItems(mValidLanguageNames, getSelectedBools(),
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                if (isChecked) {
                                    // If the user checked the item, add it to the selected items
                                    mSelectedLanguages.add(which);
                                } else if (mSelectedLanguages.contains(which)) {
                                    // Else, if the item is already in the array, remove it
                                    mSelectedLanguages.remove(Integer.valueOf(which));
                                }
                            }
                        })
                // Set the action buttons
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK, so return the selected languages
                        // to the component that opened the dialog

                        mListener.onDialogPositiveClick(getSelectedNames());
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        return builder.create();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the SelectLanguagesDialogListener so we can send events to the host
            mListener = (SelectLanguagesDialogListener) activity;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException(activity.toString()
                    + " must implement SelectLanguagesDialogListener");
        }
    }

    private boolean[] getSelectedBools() {
        boolean[] selected = new boolean[mValidLanguageNames.length];
        for (Integer i : mSelectedLanguages) {
            selected[i] = true;
        }
        return selected;
    }

    private CharSequence[] getSelectedNames() {
        CharSequence[] selected = new CharSequence[mSelectedLanguages.size()];
        for (int i = 0; i < selected.length; ++i) {
            selected[i] = mValidLanguageNames[mSelectedLanguages.get(i)];
        }
        return selected;
    }
}
