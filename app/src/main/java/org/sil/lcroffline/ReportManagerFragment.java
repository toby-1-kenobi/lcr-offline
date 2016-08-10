package org.sil.lcroffline;


import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.LongSparseArray;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sil.lcroffline.data.DatabaseContract;
import org.sil.lcroffline.data.DatabaseHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import static org.sil.lcroffline.data.DatabaseContract.*;


/**
 * A simple {@link Fragment} subclass.
 */
public class ReportManagerFragment extends Fragment implements UserFragment.UserDataListener {

    private final String LOG_TAG = ReportManagerFragment.class.getSimpleName();

    private long mUserID;

    private DatabaseHelper mDBHelper;
    private View mRootView;
    private SimpleDateFormat mDateFormat = new SimpleDateFormat("d MMMM yyyy");


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

        // listen to the user fragment for changes in the user data if possible
        UserFragment userFragment = (UserFragment) getFragmentManager().findFragmentByTag(getString(R.string.user_fragment_tag));
        if (userFragment != null) {
            userFragment.addUserDataListener(this);
        }

        // get current user id from shared preferences
        mUserID = sharedPref.getLong(getString(R.string.current_user_id_key), -1);
        if (mUserID == -1) {
            Log.e(LOG_TAG, "could not get user id from shared preferences.");
        } else {
            countReports();
        }
        return mRootView;
    }

    @Override
    public void userUpdated(long userID) {
        mUserID = userID;
        countReports();
        pushReportsToLCR();
    }

    private void countReports() {

        Cursor queued = mDBHelper.getQueuedReports(mUserID);
        Log.d(LOG_TAG, queued.getCount() + " queued reports.");
        TextView queuedCountView = (TextView) mRootView.findViewById(R.id.queued_count);
        int queuedCount = queued.getCount();
        queuedCountView.setText(String.valueOf(queuedCount));

        Cursor uploaded = mDBHelper.getUploadedReports(mUserID);
        Log.d(LOG_TAG, uploaded.getCount() + " uploaded reports.");
        TextView uploadedCount = (TextView) mRootView.findViewById(R.id.uploaded_count);
        uploadedCount.setText(String.valueOf(uploaded.getCount()));

        queued.close();
        uploaded.close();

    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(LOG_TAG, "fragment started");
        pushReportsToLCR();
    }

    private void pushReportsToLCR() {
        if (mUserID >= 0 && isNetworkAvailable()) {
            Cursor queued = mDBHelper.getQueuedReports(mUserID);
            LongSparseArray<JSONObject> reports = buildJSONReports(queued);
            queued.close();
            Log.d(LOG_TAG, "reports JSON built");
            if (reports.size() > 0) {
                ReportUploadTask uploadTask = new ReportUploadTask(reports);
                uploadTask.execute((Void) null);
            }
        }
    }

    private LongSparseArray<JSONObject> buildJSONReports(Cursor dbReports) {
        LongSparseArray<JSONObject> reports = new LongSparseArray<JSONObject>();
        dbReports.moveToFirst();
        while(!dbReports.isAfterLast()) {
            long reportID = dbReports.getLong(dbReports.getColumnIndex(ReportEntry._ID));
            Log.d(LOG_TAG, "building report " + reportID);
            JSONObject languages = new JSONObject();
            long[] reportLanguages = mDBHelper.getReportLanguages(reportID);
            try {
                for (long language : reportLanguages) {
                    languages.put(String.valueOf(language), language);
                }
                long stateId = mDBHelper.getLanguageState(reportLanguages[0]);
                Date reportDate = new Date(dbReports.getLong(dbReports.getColumnIndex(ReportEntry.COLUMN_DATE)));
                String reportContent = dbReports.getString(dbReports.getColumnIndex(ReportEntry.COLUMN_CONTENT));
                String packageName = getActivity().getPackageName();
                String versionName = null;
                try {
                    versionName = getActivity().getPackageManager().getPackageInfo(packageName, 0).versionName;
                } catch (PackageManager.NameNotFoundException e) {
                    Log.e(LOG_TAG, "could not get app version");
                    versionName = "unknown";
                }
                JSONObject report = new JSONObject();
                report.put(getString(R.string.LCR_report_state_key), stateId);
                report.put(getString(R.string.LCR_report_date_key), mDateFormat.format(reportDate));
                report.put(getString(R.string.LCR_report_content_key), reportContent);
                report.put(getString(R.string.LCR_report_impact_key), "1");
                report.put(getString(R.string.LCR_report_languages_key), languages);
                report.put(getString(R.string.LCR_report_client_key),packageName);
                report.put(getString(R.string.LCR_report_version_key), versionName);
                JSONObject reportRoot = new JSONObject();
                reportRoot.put(getString(R.string.LCR_report_key), report);
                reports.put(reportID, reportRoot);
            } catch (JSONException e) {
                Log.e(LOG_TAG, "error building JSON report for report id: " + reportID, e);
            }
            dbReports.moveToNext();
        }
        return reports;
    }

    public void incrementQueuedReports() {
        TextView queuedCount = (TextView) mRootView.findViewById(R.id.queued_count);
        int count = Integer.parseInt((String) queuedCount.getText());
        queuedCount.setText(String.valueOf(++count));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class ReportUploadTask extends AsyncTask<Void, Integer, Void> {

        private final String LOG_TAG = ReportUploadTask.class.getSimpleName();

        LongSparseArray<JSONObject> mJSONReports;
        private int mReportTotalCount;

        ReportUploadTask(LongSparseArray<JSONObject> JSONReports) {
            mJSONReports = JSONReports;
            mReportTotalCount = JSONReports.size();
        }

        @Override
        protected Void doInBackground(Void... params) {

            // get the authorisation token
            SharedPreferences sharedPref = getContext().getSharedPreferences(
            getString(R.string.preference_file_key),
                    Context.MODE_PRIVATE
            );
            String jwt = sharedPref.getString(getString(R.string.jwt_key), null);

            // These need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpsURLConnection urlConnection = null;
            BufferedWriter postWriter = null;
            BufferedReader responseReader = null;

            // Will contain the raw JSON response as a string.
            String JSONResponseStr = null;

            try {

                Uri builtUri = Uri.parse(BuildConfig.LCR_URL).buildUpon().appendPath("reports").appendPath("create_external").build();
                URL url = new URL(builtUri.toString());
                Log.d(LOG_TAG, "connecting to " + url.toString());


                for (int i = 0; i < mJSONReports.size(); ++i) {

                    String postData = mJSONReports.valueAt(i).toString();

                    // connect to LCR
                    urlConnection = (HttpsURLConnection) url.openConnection();
                    urlConnection.setRequestMethod("POST");
                    urlConnection.setRequestProperty("Authorization", "Bearer " + jwt);
                    urlConnection.setDoOutput(true);
                    urlConnection.setRequestProperty("Content-Type", "application/json");
                    urlConnection.setFixedLengthStreamingMode(postData.getBytes("UTF-8").length);
                    urlConnection.connect();

                    postWriter = new BufferedWriter(new OutputStreamWriter(urlConnection.getOutputStream()));
                    InputStream inputStream;

                    publishProgress(i + 1);
                    Log.d(LOG_TAG, "uploading report " + mJSONReports.keyAt(i));

                    // post the user credentials
                    postWriter.write(postData);
                    postWriter.flush();
                    postWriter.close();

                    // check the response
                    int responseCode = urlConnection.getResponseCode();
                    if (responseCode >= 200 && responseCode < 300) {
                        // successful response
                        Log.d(LOG_TAG, "server responses success " + responseCode);

                        inputStream = urlConnection.getInputStream();
                        responseReader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuffer buffer = new StringBuffer();
                        String line;
                        while ((line = responseReader.readLine()) != null) {
                            buffer.append(line).append("\n");
                        }

                        if (buffer.length() == 0) {
                            // Stream was empty.
                            Log.e(LOG_TAG, "Empty response after submitting report");
                            continue;
                        }
                        JSONResponseStr = buffer.toString();
                    } else if (responseCode >= 400 && responseCode < 500) {
                        // server error response means the authentication didn't go through
                        Log.d(LOG_TAG, "server error " + responseCode);

                    } else {
                        Log.e(LOG_TAG, "Unexpected server response code: " + responseCode);
                    }

                    try {
                        JSONObject JSONResponse = new JSONObject(JSONResponseStr);
                        boolean success = JSONResponse.getBoolean(getString(R.string.LCR_response_success_key));
                        if (success) {
                            int LCRReportID = JSONResponse.getInt(getString(R.string.LCR_response_report_id_key));
                            Log.d(LOG_TAG, "LCR report id: " + LCRReportID);
                            mDBHelper.addLCRidToReport(mJSONReports.keyAt(i), LCRReportID);
                        } else {
                            JSONArray errors = JSONResponse.getJSONArray(getString(R.string.LCR_response_errors_key));
                            Log.e(LOG_TAG, "report upload failed: " + errors.toString());
                        }
                    } catch (JSONException e) {
                        Log.e(LOG_TAG, "Error fetching data from JSON", e);
                    }
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Error ", e);
            }
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
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            double percentProgress = (100.0 * progress[0]) / mReportTotalCount;
            Log.d(LOG_TAG, "progress update: " + progress[0] + " of " + mReportTotalCount + " (" + percentProgress + "%)");
            ProgressBar progressBar = (ProgressBar) mRootView.findViewById(R.id.report_upload_progress);
            progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress((int) percentProgress);
        }

        @Override
        protected void onPostExecute(Void result) {
            ProgressBar progressBar = (ProgressBar) mRootView.findViewById(R.id.report_upload_progress);
            progressBar.setVisibility(View.GONE);
            countReports();
        }
    }

}
