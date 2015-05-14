package com.door43.util.wizard;

import android.app.Activity;
import android.app.Fragment;

/**
 * This abstract class provides a simple mechanism for creating multiple steps within a wizard.
 */
public abstract class WizardFragment extends Fragment {
    private OnFragmentInteractionListener mListener;

    public void onPrevious() {
        if(mListener != null) {
            mListener.onPrevious();
        }
    }

    public void onNext() {
        if (mListener != null) {
            mListener.onNext();
        }
    }

    public void onCancel() {
        if (mListener != null) {
            mListener.onCancel();
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        void onNext();
        void onCancel();
        void onPrevious();
    }

}
