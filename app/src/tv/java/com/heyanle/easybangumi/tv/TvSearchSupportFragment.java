package com.heyanle.easybangumi.tv;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import android.Manifest;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;
import androidx.leanback.app.RowsSupportFragment;
import androidx.leanback.app.SearchSupportFragment;
import androidx.leanback.widget.ObjectAdapter;
import androidx.leanback.widget.OnItemViewClickedListener;
import androidx.leanback.widget.OnItemViewSelectedListener;
import androidx.leanback.widget.Presenter;
import androidx.leanback.widget.Row;
import androidx.leanback.widget.RowPresenter;
import androidx.leanback.widget.SearchBar;
import androidx.leanback.widget.SpeechRecognitionCallback;
import androidx.leanback.widget.VerticalGridView;

import com.heyanle.easybangumi.R;

import java.util.List;

public class TvSearchSupportFragment extends Fragment {
    static final String TAG = TvSearchSupportFragment.class.getSimpleName();
    static final boolean DEBUG = false;

    private static final String EXTRA_LEANBACK_BADGE_PRESENT = "LEANBACK_BADGE_PRESENT";
    private static final String ARG_PREFIX = TvSearchSupportFragment.class.getCanonicalName();
    private static final String ARG_QUERY =  ARG_PREFIX + ".query";
    private static final String ARG_TITLE = ARG_PREFIX  + ".title";

    static final long SPEECH_RECOGNITION_DELAY_MS = 300;

    static final int RESULTS_CHANGED = 0x1;
    static final int QUERY_COMPLETE = 0x2;

    static final int AUDIO_PERMISSION_REQUEST_CODE = 0;

    /**
     * Search API to be provided by the application.
     */
    public static interface SearchResultProvider {
        /**
         * <p>Method invoked some time prior to the first call to onQueryTextChange to retrieve
         * an ObjectAdapter that will contain the results to future updates of the search query.</p>
         *
         * <p>As results are retrieved, the application should use the data set notification methods
         * on the ObjectAdapter to instruct the SearchSupportFragment to update the results.</p>
         *
         * @return ObjectAdapter The result object adapter.
         */
        public ObjectAdapter getResultsAdapter();

        /**
         * <p>Method invoked when the search query is updated.</p>
         *
         * <p>This is called as soon as the query changes; it is up to the application to add a
         * delay before actually executing the queries if needed.
         *
         * <p>This method might not always be called before onQueryTextSubmit gets called, in
         * particular for voice input.
         *
         * @param newQuery The current search query.
         * @return whether the results changed as a result of the new query.
         */
        public boolean onQueryTextChange(String newQuery);

        /**
         * Method invoked when the search query is submitted, either by dismissing the keyboard,
         * pressing search or next on the keyboard or when voice has detected the end of the query.
         *
         * @param query The query entered.
         * @return whether the results changed as a result of the query.
         */
        public boolean onQueryTextSubmit(String query, String key);
    }

    final ObjectAdapter.DataObserver mAdapterObserver = new ObjectAdapter.DataObserver() {
        @Override
        public void onChanged() {
            // onChanged() may be called multiple times e.g. the provider add
            // rows to ArrayObjectAdapter one by one.
            mHandler.removeCallbacks(mResultsChangedCallback);
            mHandler.post(mResultsChangedCallback);
        }
    };

    final Handler mHandler = new Handler();

    final Runnable mResultsChangedCallback = new Runnable() {
        @Override
        public void run() {
            if (DEBUG) Log.v(TAG, "results changed, new size " + mResultAdapter.size());
            if (mRowsSupportFragment != null
                    && mRowsSupportFragment.getAdapter() != mResultAdapter) {
                if (!(mRowsSupportFragment.getAdapter() == null && mResultAdapter.size() == 0)) {
                    mRowsSupportFragment.setAdapter(mResultAdapter);
                    mRowsSupportFragment.setSelectedPosition(0);
                }
            }
            updateSearchBarVisibility();
            mStatus |= RESULTS_CHANGED;
            if ((mStatus & QUERY_COMPLETE) != 0) {
                updateFocus();
            }
            updateSearchBarNextFocusId();
        }
    };

    /**
     * Runs when a new provider is set AND when the fragment view is created.
     */
    private final Runnable mSetSearchResultProvider = new Runnable() {
        @Override
        public void run() {
            if (mRowsSupportFragment == null) {
                // We'll retry once we have a rows fragment
                return;
            }
            // Retrieve the result adapter
            ObjectAdapter adapter = mProvider.getResultsAdapter();
            if (DEBUG) Log.v(TAG, "Got results adapter " + adapter);
            if (adapter != mResultAdapter) {
                boolean firstTime = mResultAdapter == null;
                releaseAdapter();
                mResultAdapter = adapter;
                if (mResultAdapter != null) {
                    mResultAdapter.registerObserver(mAdapterObserver);
                }
                if (DEBUG) {
                    Log.v(TAG, "mResultAdapter " + mResultAdapter + " size "
                            + (mResultAdapter == null ? 0 : mResultAdapter.size()));
                }
                // delay the first time to avoid setting a empty result adapter
                // until we got first onChange() from the provider
                if (!(firstTime && (mResultAdapter == null || mResultAdapter.size() == 0))) {
                    mRowsSupportFragment.setAdapter(mResultAdapter);
                }
                executePendingQuery();
            }
            updateSearchBarNextFocusId();

            if (DEBUG) {
                Log.v(TAG, "mAutoStartRecognition " + mAutoStartRecognition
                        + " mResultAdapter " + mResultAdapter
                        + " adapter " + mRowsSupportFragment.getAdapter());
            }
            if (mAutoStartRecognition) {
                mHandler.removeCallbacks(mStartRecognitionRunnable);
                mHandler.postDelayed(mStartRecognitionRunnable, SPEECH_RECOGNITION_DELAY_MS);
            } else {
                updateFocus();
            }
        }
    };

    final Runnable mStartRecognitionRunnable = new Runnable() {
        @Override
        public void run() {
            mAutoStartRecognition = false;
//            mSearchBar.startRecognition();
        }
    };

    RowsSupportFragment mRowsSupportFragment;
    TvSearchBar mSearchBar;
    SearchResultProvider mProvider;
    String mPendingQuery = null;

    OnItemViewSelectedListener mOnItemViewSelectedListener;
    private OnItemViewClickedListener mOnItemViewClickedListener;
    ObjectAdapter mResultAdapter;
    private SpeechRecognitionCallback mSpeechRecognitionCallback;

    private String mTitle;
    private Drawable mBadgeDrawable;
    private ExternalQuery mExternalQuery;

//    private SpeechRecognizer mSpeechRecognizer;

    int mStatus;
    boolean mAutoStartRecognition = true;

    private boolean mIsPaused;
    private boolean mPendingStartRecognitionWhenPaused;
    private SearchBar.SearchBarPermissionListener mPermissionListener =
            new SearchBar.SearchBarPermissionListener() {
                @Override
                public void requestAudioPermission() {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},
                            AUDIO_PERMISSION_REQUEST_CODE);
                }
            };

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        if (requestCode == AUDIO_PERMISSION_REQUEST_CODE && permissions.length > 0) {
            if (permissions[0].equals(Manifest.permission.RECORD_AUDIO)
                    && grantResults[0] == PERMISSION_GRANTED) {
                startRecognition();
            }
        }
    }

    /**
     * @param args Bundle to use for the arguments, if null a new Bundle will be created.
     */
    public static Bundle createArgs(Bundle args, String query) {
        return createArgs(args, query, null);
    }

    public static Bundle createArgs(Bundle args, String query, String title)  {
        if (args == null) {
            args = new Bundle();
        }
        args.putString(ARG_QUERY, query);
        args.putString(ARG_TITLE, title);
        return args;
    }

    /**
     * Creates a search fragment with a given search query.
     *
     * <p>You should only use this if you need to start the search fragment with a
     * pre-filled query.
     *
     * @param query The search query to begin with.
     * @return A new 
     */
    public static SearchSupportFragment newInstance(String query) {
        SearchSupportFragment fragment = new SearchSupportFragment();
        Bundle args = createArgs(null, query);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        if (mAutoStartRecognition) {
            mAutoStartRecognition = savedInstanceState == null;
        }
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_tv_search, container, false);

        FrameLayout searchFrame = (FrameLayout) root.findViewById(R.id.lb_search_frame);
        mSearchBar = (TvSearchBar) searchFrame.findViewById(R.id.lb_search_bar);
        mSearchBar.setSearchBarListener(new TvSearchBar.SearchBarListener() {
            @Override
            public void onSearchQueryChange(String query) {
                if (DEBUG) Log.v(TAG, String.format("onSearchQueryChange %s %s", query,
                        null == mProvider ? "(null)" : mProvider));
                if (null != mProvider) {
                    retrieveResults(query);
                } else {
                    mPendingQuery = query;
                }
            }

            @Override
            public void onSearchQuerySubmit(String query, String key) {
                if (DEBUG) Log.v(TAG, String.format("onSearchQuerySubmit %s", query));
                submitQuery(query, key);
            }

            @Override
            public void onKeyboardDismiss(String query) {
                if (DEBUG) Log.v(TAG, String.format("onKeyboardDismiss %s", query));
                queryComplete();
            }
        });
//        mSearchBar.setSpeechRecognitionCallback(mSpeechRecognitionCallback);
        mSearchBar.setPermissionListener(mPermissionListener);
//        applyExternalQuery();

        readArguments(getArguments());
        if (null != mBadgeDrawable) {
            setBadgeDrawable(mBadgeDrawable);
        }
        if (null != mTitle) {
            setTitle(mTitle);
        }

        // Inject the RowsSupportFragment in the results container
        if (getChildFragmentManager().findFragmentById(R.id.lb_results_frame) == null) {
            mRowsSupportFragment = new RowsSupportFragment();
            getChildFragmentManager().beginTransaction()
                    .replace(R.id.lb_results_frame, mRowsSupportFragment).commit();
        } else {
            mRowsSupportFragment = (RowsSupportFragment) getChildFragmentManager()
                    .findFragmentById(R.id.lb_results_frame);
        }
        mRowsSupportFragment.setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
                if (DEBUG) {
                    int position = mRowsSupportFragment.getSelectedPosition();
                    Log.v(TAG, String.format("onItemSelected %d", position));
                }
                updateSearchBarVisibility();
                if (null != mOnItemViewSelectedListener) {
                    mOnItemViewSelectedListener.onItemSelected(itemViewHolder, item,
                            rowViewHolder, row);
                }
            }
        });
        mRowsSupportFragment.setOnItemViewClickedListener(mOnItemViewClickedListener);
        mRowsSupportFragment.setExpand(true);
        if (null != mProvider) {
            onSetSearchResultProvider();
        }
        return root;
    }

    private void resultsAvailable() {
        if ((mStatus & QUERY_COMPLETE) != 0) {
            focusOnResults();
        }
        updateSearchBarNextFocusId();
    }

    @Override
    public void onStart() {
        super.onStart();

        VerticalGridView list = mRowsSupportFragment.getVerticalGridView();
        int mContainerListAlignTop =
                getResources().getDimensionPixelSize(R.dimen.search_browse_rows_align_top);
        list.setItemAlignmentOffset(0);
        list.setItemAlignmentOffsetPercent(VerticalGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);
        list.setWindowAlignmentOffset(mContainerListAlignTop);
        list.setWindowAlignmentOffsetPercent(VerticalGridView.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        list.setWindowAlignment(VerticalGridView.WINDOW_ALIGN_NO_EDGE);
        // VerticalGridView should not be focusable (see b/26894680 for details).
        list.setFocusable(false);
        list.setFocusableInTouchMode(false);
    }

    @Override
    public void onResume() {
        super.onResume();
        mIsPaused = false;
//        if (mSpeechRecognitionCallback == null && null == mSpeechRecognizer) {
//            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(
//                    getContext());
//            mSearchBar.setSpeechRecognizer(mSpeechRecognizer);
//        }
        if (mPendingStartRecognitionWhenPaused) {
            mPendingStartRecognitionWhenPaused = false;
//            mSearchBar.startRecognition();
        } else {
            // Ensure search bar state consistency when using external recognizer
//            mSearchBar.stopRecognition();
        }
    }

    public void setSourceLabel(List<String> sourceText, int defaultFocus){
        mSearchBar.setRadioGroup(sourceText, defaultFocus);
    }

    @Override
    public void onPause() {
        mIsPaused = true;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        releaseAdapter();
        super.onDestroy();
    }

    /**
     * Returns RowsSupportFragment that shows result rows. RowsSupportFragment is initialized after
     * onCreateView().
     *
     * @return RowsSupportFragment that shows result rows.
     */
    public RowsSupportFragment getRowsSupportFragment() {
        return mRowsSupportFragment;
    }



    /**
     * Starts speech recognition.  Typical use case is that
     * activity receives onNewIntent() call when user clicks a MIC button.
     * Note that SearchSupportFragment automatically starts speech recognition
     * at first time created, there is no need to call startRecognition()
     * when fragment is created.
     */
    public void startRecognition() {
        if (mIsPaused) {
            mPendingStartRecognitionWhenPaused = true;
        } else {
//            mSearchBar.startRecognition();
        }
    }

    /**
     * Sets the search provider that is responsible for returning results for the
     * search query.
     */
    public void setSearchResultProvider(SearchResultProvider searchResultProvider) {
        if (mProvider != searchResultProvider) {
            mProvider = searchResultProvider;
            onSetSearchResultProvider();
        }
    }

    /**
     * Sets an item selection listener for the results.
     *
     * @param listener The item selection listener to be invoked when an item in
     *        the search results is selected.
     */
    public void setOnItemViewSelectedListener(OnItemViewSelectedListener listener) {
        mOnItemViewSelectedListener = listener;
    }

    /**
     * Sets an item clicked listener for the results.
     *
     * @param listener The item clicked listener to be invoked when an item in
     *        the search results is clicked.
     */
    public void setOnItemViewClickedListener(OnItemViewClickedListener listener) {
        if (listener != mOnItemViewClickedListener) {
            mOnItemViewClickedListener = listener;
            if (mRowsSupportFragment != null) {
                mRowsSupportFragment.setOnItemViewClickedListener(mOnItemViewClickedListener);
            }
        }
    }

    /**
     * Sets the title string to be be shown in an empty search bar. The title
     * may be placed in a call-to-action, such as "Search <i>title</i>" or
     * "Speak to search <i>title</i>".
     */
    public void setTitle(String title) {
        mTitle = title;
        if (null != mSearchBar) {
            mSearchBar.setTitle(title);
        }
    }

    /**
     * Returns the title set in the search bar.
     */
    public String getTitle() {
        if (null != mSearchBar) {
            return mSearchBar.getTitle();
        }
        return null;
    }

    /**
     * Sets the badge drawable that will be shown inside the search bar next to
     * the title.
     */
    public void setBadgeDrawable(Drawable drawable) {
        mBadgeDrawable = drawable;
        if (null != mSearchBar) {
            mSearchBar.setBadgeDrawable(drawable);
        }
    }

    /**
     * Returns the badge drawable in the search bar.
     */
    public Drawable getBadgeDrawable() {
        if (null != mSearchBar) {
            return mSearchBar.getBadgeDrawable();
        }
        return null;
    }



    /**
     * Displays the completions shown by the IME. An application may provide
     * a list of query completions that the system will show in the IME.
     *
     * @param completions A list of completions to show in the IME. Setting to
     *        null or empty will clear the list.
     */
    public void displayCompletions(List<String> completions) {
        mSearchBar.displayCompletions(completions);
    }

    /**
     * Displays the completions shown by the IME. An application may provide
     * a list of query completions that the system will show in the IME.
     *
     * @param completions A list of completions to show in the IME. Setting to
     *        null or empty will clear the list.
     */
    public void displayCompletions(CompletionInfo[] completions) {
        mSearchBar.displayCompletions(completions);
    }




    void retrieveResults(String searchQuery) {
        if (DEBUG) Log.v(TAG, "retrieveResults " + searchQuery);
        if (mProvider.onQueryTextChange(searchQuery)) {
            mStatus &= ~QUERY_COMPLETE;
        }
    }

    void submitQuery(String query, String key) {
        queryComplete();
        if (null != mProvider) {
            mProvider.onQueryTextSubmit(query,key);
        }
    }

    void queryComplete() {
        if (DEBUG) Log.v(TAG, "queryComplete");
        mStatus |= QUERY_COMPLETE;
        focusOnResults();
    }

    void updateSearchBarVisibility() {
        int position = mRowsSupportFragment != null ? mRowsSupportFragment.getSelectedPosition() : -1;
        mSearchBar.setVisibility(position <=0 || mResultAdapter == null
                || mResultAdapter.size() == 0 ? View.VISIBLE : View.GONE);
    }

    void updateSearchBarNextFocusId() {
        if (mSearchBar == null || mResultAdapter == null) {
            return;
        }
        final int viewId = (mResultAdapter.size() == 0 || mRowsSupportFragment == null
                || mRowsSupportFragment.getVerticalGridView() == null)
                ? 0 : mRowsSupportFragment.getVerticalGridView().getId();
        mSearchBar.setNextFocusDownId(viewId);
    }

    void updateFocus() {
        if (mResultAdapter != null && mResultAdapter.size() > 0
                && mRowsSupportFragment != null && mRowsSupportFragment.getAdapter() == mResultAdapter) {
            focusOnResults();
        } else {
            mSearchBar.requestFocus();
        }
    }

    private void focusOnResults() {
        if (mRowsSupportFragment == null || mRowsSupportFragment.getVerticalGridView() == null
                || mResultAdapter.size() == 0) {
            return;
        }
        if (mRowsSupportFragment.getVerticalGridView().requestFocus()) {
            mStatus &= ~RESULTS_CHANGED;
        }
    }

    private void onSetSearchResultProvider() {
        mHandler.removeCallbacks(mSetSearchResultProvider);
        mHandler.post(mSetSearchResultProvider);
    }

    void releaseAdapter() {
        if (mResultAdapter != null) {
            mResultAdapter.unregisterObserver(mAdapterObserver);
            mResultAdapter = null;
        }
    }

    void executePendingQuery() {
        if (null != mPendingQuery && null != mResultAdapter) {
            String query = mPendingQuery;
            mPendingQuery = null;
            retrieveResults(query);
        }
    }

    private void applyExternalQuery() {
        if (mExternalQuery == null || mSearchBar == null) {
            return;
        }
        mSearchBar.setSearchQuery(mExternalQuery.mQuery);
        if (mExternalQuery.mSubmit) {
            submitQuery(mExternalQuery.mQuery, "");
        }
        mExternalQuery = null;
    }

    private void readArguments(Bundle args) {
        if (null == args) {
            return;
        }
        if (args.containsKey(ARG_QUERY)) {
            setSearchQuery(args.getString(ARG_QUERY));
        }

        if (args.containsKey(ARG_TITLE)) {
            setTitle(args.getString(ARG_TITLE));
        }
    }

    private void setSearchQuery(String query) {
        mSearchBar.setSearchQuery(query);
    }

    static class ExternalQuery {
        String mQuery;
        boolean mSubmit;

        ExternalQuery(String query, boolean submit) {
            mQuery = query;
            mSubmit = submit;
        }
    }
}
