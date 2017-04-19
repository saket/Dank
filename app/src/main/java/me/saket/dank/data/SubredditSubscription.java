package me.saket.dank.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcelable;

import com.google.auto.value.AutoValue;

import rx.functions.Func1;

/**
 * Represents a subreddit subscribed by the user.
 */
@AutoValue
public abstract class SubredditSubscription implements Parcelable {

    static final String TABLE = "SubredditSubscription";
    static final String COLUMN_NAME = "name";
    static final String COLUMN_PENDING_ACTION = "pending_action";

    static final String COLUMN_IS_HIDDEN = "is_hidden";
    static final String HIDDEN = "1";
    static final String NOT_HIDDEN = "0";

    static final String QUERY_CREATE_TABLE =
            "CREATE TABLE " + TABLE + " ("
                    + COLUMN_NAME + " TEXT NOT NULL PRIMARY KEY, "
                    + COLUMN_PENDING_ACTION + " TEXT NOT NULL, "
                    + COLUMN_IS_HIDDEN + " INTEGER NOT NULL)";

    static final String QUERY_GET_ALL =
            "SELECT * FROM " + TABLE + " ORDER BY " + COLUMN_NAME + " ASC";

    static final String QUERY_GET_ALL_PENDING =
            "SELECT * FROM " + TABLE
                    + " WHERE " + COLUMN_PENDING_ACTION + " == '" + PendingState.PENDING_SUBSCRIBE + "'"
                    + " OR " + COLUMN_PENDING_ACTION + " == '" + PendingState.PENDING_UNSUBSCRIBE + "'"
                    + " ORDER BY " + COLUMN_NAME + " COLLATE NOCASE";

    static final String QUERY_SEARCH_ALL_SUBSCRIBED_INCLUDING_HIDDEN =
            "SELECT * FROM " + TABLE
                    + " WHERE " + COLUMN_NAME + " LIKE ?"
                    + " AND " + COLUMN_PENDING_ACTION + " != '" + PendingState.PENDING_UNSUBSCRIBE + "' "
                    + " ORDER BY " + COLUMN_NAME + " COLLATE NOCASE";

    static final String QUERY_SEARCH_ALL_SUBSCRIBED_EXCLUDING_HIDDEN =
            "SELECT * FROM " + TABLE
                    + " WHERE " + COLUMN_NAME + " LIKE ?"
                    + " AND " + COLUMN_PENDING_ACTION + " != '" + PendingState.PENDING_UNSUBSCRIBE + "'"
                    + " AND " + COLUMN_IS_HIDDEN + " != '" + HIDDEN + "'"
                    + " ORDER BY " + COLUMN_NAME + " COLLATE NOCASE";

    static final String WHERE_NAME = String.format(
            "%s = ?",
            COLUMN_NAME
    );

    public enum PendingState {
        NONE,
        PENDING_SUBSCRIBE,
        PENDING_UNSUBSCRIBE,
    }

    public abstract String name();

    public abstract PendingState pendingState();

    /**
     * This is not a state because a pending-subscribe subreddit can also be hidden.
     */
    public abstract boolean isHidden();

    public boolean isUnsubscribePending() {
        return pendingState() == PendingState.PENDING_UNSUBSCRIBE;
    }

    public boolean isSubscribePending() {
        return pendingState() == PendingState.PENDING_SUBSCRIBE;
    }

    public ContentValues toContentValues() {
        ContentValues values = new ContentValues(3);
        values.put(COLUMN_NAME, name());
        values.put(COLUMN_PENDING_ACTION, pendingState().toString());
        values.put(COLUMN_IS_HIDDEN, isHidden() ? HIDDEN : NOT_HIDDEN);
        return values;
    }

    public Builder toBuilder() {
        return new AutoValue_SubredditSubscription.Builder(this);
    }

    public static final Func1<Cursor, SubredditSubscription> MAPPER = cursor -> {
        String subredditName = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_NAME));
        String pendingStateString = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_PENDING_ACTION));
        boolean isHidden = HIDDEN.equals(cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_IS_HIDDEN)));
        return SubredditSubscription.create(subredditName, PendingState.valueOf(pendingStateString), isHidden);
    };

    public static SubredditSubscription create(String name, PendingState pendingState, boolean isHidden) {
        return new AutoValue_SubredditSubscription(name, pendingState, isHidden);
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder name(String name);

        public abstract Builder pendingState(PendingState pendingState);

        public abstract Builder isHidden(boolean hidden);

        public abstract SubredditSubscription build();
    }

}
