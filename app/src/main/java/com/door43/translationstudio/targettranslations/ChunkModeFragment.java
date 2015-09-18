package com.door43.translationstudio.targettranslations;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.door43.translationstudio.R;
import com.door43.translationstudio.core.Library;
import com.door43.translationstudio.core.SourceTranslation;
import com.door43.translationstudio.core.TargetTranslation;
import com.door43.translationstudio.core.Translator;
import com.door43.translationstudio.util.AppContext;

import java.security.InvalidParameterException;

/**
 * Displays translations in chunks
 */
public class ChunkModeFragment extends ViewModeFragment {

    @Override
    ViewModeAdapter getAdapter(Context context, String targetTranslationId, String sourceTranslationId) {
        return new ChunkModeAdapter(context, targetTranslationId, sourceTranslationId);
    }
}
