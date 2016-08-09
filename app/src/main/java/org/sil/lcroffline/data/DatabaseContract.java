package org.sil.lcroffline.data;

import android.provider.BaseColumns;

/**
 * Created by toby on 9/08/16.
 */
public class DatabaseContract {

    public static final class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";

        // user phone number
        public static final String COLUMN_PHONE = "phone";

        // user name
        public static final String COLUMN_NAME = "name";

        // date of last data pulled from online app for this user
        public static final String COLUMN_UPDATED = "updated";
    }

    public static final class ReportEntry implements BaseColumns {
        public static final String TABLE_NAME = "reports";

        // date of the report
        public static final String COLUMN_DATE = "date";

        // content of the report
        public static final String COLUMN_CONTENT = "content";

        // id that this report holds in the web app
        public static final String COLUMN_LCR_ID = "id_lcr";

        // message returned from the web app if report upload fails
        public static final String COLUMN_FAIL_MSG = "fail_message";

        // foreign key referring to the user that made the report
        public static final String COLUMN_USER_KEY = "user_id";
    }

    public static final class StateEntry implements BaseColumns {
        public static final String TABLE_NAME = "states";
        public static final String COLUMN_NAME = "name";
    }

    public static final class LanguageEntry implements BaseColumns {
        public static final String TABLE_NAME = "languages";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_STATE_KEY = "state_id";
    }

    public static final class StateUserJoinEntry implements BaseColumns {

        public static final String TABLE_NAME = "states_users";
        public static final String COLUMN_USER_KEY = "user_id";
        public static final String COLUMN_STATE_KEY = "state_id";
    }

    public static final class LanguageReportJoinEntry implements BaseColumns {

        public static final String TABLE_NAME = "languages_reports";
        public static final String COLUMN_REPORT_KEY = "Report_id";
        public static final String COLUMN_LANGUAGE_KEY = "Language_id";
    }

}
