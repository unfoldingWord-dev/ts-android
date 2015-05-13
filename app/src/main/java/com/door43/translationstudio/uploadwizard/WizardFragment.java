package com.door43.translationstudio.uploadwizard;

import android.app.Activity;

import com.door43.translationstudio.util.TranslatorBaseFragment;


public class WizardFragment extends TranslatorBaseFragment {
    private OnFragmentInteractionListener mListener;

    public void onPrevious() {
        if(mListener != null) {
            mListener.onPrevious();
        }
    }

    public void onContinue() {
        if (mListener != null) {
            mListener.onContinue();
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
        void onContinue();
        void onCancel();
        void onPrevious();
    }

}
