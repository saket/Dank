package me.saket.dank.submission;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toolbar;

import net.dean.jraw.models.Submission;

import butterknife.BindView;
import butterknife.ButterKnife;
import me.saket.dank.R;

public class SubmissionFragment extends Fragment {

    @BindView(R.id.toolbar) Toolbar toolbar;
    @BindView(R.id.submission_title) TextView titleView;
    @BindView(R.id.submission_subtitle) TextView subtitleView;

    public interface Callbacks {
        void onSubmissionToolbarUpClick();
    }

    public static SubmissionFragment create() {
        return new SubmissionFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View fragmentLayout = inflater.inflate(R.layout.fragment_submission, container, false);
        ButterKnife.bind(this, fragmentLayout);

        toolbar.setNavigationOnClickListener(v -> {
            ((Callbacks) getActivity()).onSubmissionToolbarUpClick();
        });

        return fragmentLayout;
    }

    public void populate(Submission submission) {
        titleView.setText(submission.getTitle());
        subtitleView.setText(getString(R.string.subreddit_name_r_prefix, submission.getSubredditName()));
    }

}
