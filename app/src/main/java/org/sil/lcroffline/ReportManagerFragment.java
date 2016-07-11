package org.sil.lcroffline;


import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReportManagerFragment extends Fragment {

    private final String LOG_TAG = ReportManagerFragment.class.getSimpleName();

    private long userID;

    private DatabaseHelper mDBHelper;
    private View mRootView;


    public ReportManagerFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDBHelper = new DatabaseHelper(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mRootView = inflater.inflate(R.layout.fragment_report_manager, container, false);
        SharedPreferences sharedPref = getContext().getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        );
        // get current user id from shared preferences
        userID = sharedPref.getLong(getString(R.string.current_user_id_key), -1);
        if (userID == -1) {
            Log.e(LOG_TAG, "could not get user id from shared preferences.");
        } else {
            countReports();
        }
        return mRootView;
    }

    private void countReports() {

        Cursor queued = mDBHelper.getQueuedReports(userID);
        Log.d(LOG_TAG, queued.getCount() + " queued reports.");
        TextView queuedCount = (TextView) mRootView.findViewById(R.id.queued_count);
        queuedCount.setText(String.valueOf(queued.getCount()));

        Cursor uploaded = mDBHelper.getUploadedReports(userID);
        Log.d(LOG_TAG, uploaded.getCount() + " uploaded reports.");
        TextView uploadedCount = (TextView) mRootView.findViewById(R.id.uploaded_count);
        uploadedCount.setText(String.valueOf(uploaded.getCount()));
    }

    public void incrementQueuedReports() {
        TextView queuedCount = (TextView) mRootView.findViewById(R.id.queued_count);
        int count = Integer.parseInt((String) queuedCount.getText());
        queuedCount.setText(String.valueOf(++count));
    }

}
