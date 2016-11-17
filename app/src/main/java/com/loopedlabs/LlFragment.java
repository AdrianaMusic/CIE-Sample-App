package com.loopedlabs;

import android.app.Activity;
import android.support.v4.app.Fragment;

import com.cie.btp.DebugLog;

public class LlFragment extends Fragment {
    protected FragmentMessageListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        DebugLog.logTrace();
        try {
            mListener = (FragmentMessageListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement FragmentMessageListener");
        }
    }
}
