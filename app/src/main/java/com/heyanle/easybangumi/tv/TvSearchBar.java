package com.heyanle.easybangumi.tv;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Handler;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.leanback.widget.SearchBar;
import androidx.leanback.widget.SearchEditText;

import com.heyanle.easybangumi.R;

import java.util.ArrayList;
import java.util.List;

public class TvSearchBar extends RelativeLayout {
    static final String TAG = SearchBar.class.getSimpleName();
    static final boolean DEBUG = false;

    static final float FULL_LEFT_VOLUME = 1.0f;
    static final float FULL_RIGHT_VOLUME = 1.0f;
    static final int DEFAULT_PRIORITY = 1;
    static final int DO_NOT_LOOP = 0;
    static final float DEFAULT_RATE = 1.0f;
    private RadioGroup mRadioGroup;

    /**
     * Interface for receiving notification of search query changes.
     */
    public interface SearchBarListener {

        /**
         * Method invoked when the search bar detects a change in the query.
         *
         * @param query The current full query.
         */
        public void onSearchQueryChange(String query);

        /**
         * <p>Method invoked when the search query is submitted.</p>
         *
         * <p>This method can be called without a preceeding onSearchQueryChange,
         * in particular in the case of a voice input.</p>
         *
         * @param query The query being submitted.
         */
        public void onSearchQuerySubmit(String query, String key);

        /**
         * Method invoked when the IME is being dismissed.
         *
         * @param query The query set in the search bar at the time the IME is being dismissed.
         */
        public void onKeyboardDismiss(String query);

    }

    /**
     * Interface that handles runtime permissions requests. App sets listener on SearchBar via
     */
    public interface SearchBarPermissionListener {

        /**
         * Method invoked when SearchBar asks for "android.permission.RECORD_AUDIO" runtime
         * permission.
         */
        void requestAudioPermission();

    }

    SearchBarListener mSearchBarListener;
    SearchEditText mSearchTextEditor;
//    SpeechOrbView mSpeechOrbView;
    private ImageView mBadgeView;
    String mSearchQuery;
    private String mHint;
    private String mTitle;
    private Drawable mBadgeDrawable;
    final Handler mHandler = new Handler();
    private final InputMethodManager mInputMethodManager;
    boolean mAutoStartRecognition = false;
    private Drawable mBarBackground;

    private final int mTextColor;
    private final int mTextColorSpeechMode;
    private final int mTextHintColor;
    private final int mTextHintColorSpeechMode;
    private int mBackgroundAlpha;
    private int mBackgroundSpeechAlpha;
    private int mBarHeight;
//    private SpeechRecognizer mSpeechRecognizer;
//    private SpeechRecognitionCallback mSpeechRecognitionCallback;
    private boolean mListening;
//    SoundPool mSoundPool;
//    SparseIntArray mSoundMap = new SparseIntArray();
    boolean mRecognizing = false;
    private final Context mContext;
    private AudioManager mAudioManager;
    private SearchBar.SearchBarPermissionListener mPermissionListener;

    public TvSearchBar(Context context) {
        this(context, null);
    }

    public TvSearchBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TvSearchBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;

        Resources r = getResources();

        LayoutInflater inflater = LayoutInflater.from(getContext());

        inflater.inflate(R.layout.view_search_bar, this, true);

        mBarHeight = getResources().getDimensionPixelSize(androidx.leanback.R.dimen.lb_search_bar_height);
        RelativeLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                mBarHeight);
        params.addRule(ALIGN_PARENT_TOP, RelativeLayout.TRUE);
        setLayoutParams(params);
        setBackgroundColor(Color.TRANSPARENT);
        setClipChildren(false);

        mSearchQuery = "";
        mInputMethodManager =
                (InputMethodManager)context.getSystemService(Context.INPUT_METHOD_SERVICE);

        mTextColorSpeechMode = r.getColor(androidx.leanback.R.color.lb_search_bar_text_speech_mode);
        mTextColor = r.getColor(androidx.leanback.R.color.lb_search_bar_text);

        mBackgroundSpeechAlpha = r.getInteger(androidx.leanback.R.integer.lb_search_bar_speech_mode_background_alpha);
        mBackgroundAlpha = r.getInteger(androidx.leanback.R.integer.lb_search_bar_text_mode_background_alpha);

        mTextHintColorSpeechMode = r.getColor(androidx.leanback.R.color.lb_search_bar_hint_speech_mode);
        mTextHintColor = r.getColor(androidx.leanback.R.color.lb_search_bar_hint);

        mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        RelativeLayout items = (RelativeLayout)findViewById(R.id.lb_search_bar_items);
        mBarBackground = items.getBackground();

        mSearchTextEditor = (SearchEditText)findViewById(R.id.lb_search_text_editor);
        mBadgeView = (ImageView)findViewById(R.id.lb_search_bar_badge);
        if (null != mBadgeDrawable) {
            mBadgeView.setImageDrawable(mBadgeDrawable);
        }

        mRadioGroup = findViewById(R.id.rg_source);

        mSearchTextEditor.setOnFocusChangeListener(new OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (DEBUG) Log.v(TAG, "EditText.onFocusChange " + hasFocus);
                if (hasFocus) {
                    showNativeKeyboard();
                } else {
                    hideNativeKeyboard();
                }
                updateUi(hasFocus);
            }
        });
        final Runnable mOnTextChangedRunnable = new Runnable() {
            @Override
            public void run() {
                setSearchQueryInternal(mSearchTextEditor.getText().toString());
            }
        };
        mSearchTextEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
                // don't propagate event during speech recognition.
                if (mRecognizing) {
                    return;
                }
                // while IME opens,  text editor becomes "" then restores to current value
                mHandler.removeCallbacks(mOnTextChangedRunnable);
                mHandler.post(mOnTextChangedRunnable);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        mSearchTextEditor.setOnKeyboardDismissListener(
                new SearchEditText.OnKeyboardDismissListener() {
                    @Override
                    public void onKeyboardDismiss() {
                        if (null != mSearchBarListener) {
                            mSearchBarListener.onKeyboardDismiss(mSearchQuery);
                        }
                    }
                });

        mSearchTextEditor.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int action, KeyEvent keyEvent) {
                if (DEBUG) Log.v(TAG, "onEditorAction: " + action + " event: " + keyEvent);
                boolean handled = true;
                if ((EditorInfo.IME_ACTION_SEARCH == action
                        || EditorInfo.IME_NULL == action) && null != mSearchBarListener) {
                    if (DEBUG) Log.v(TAG, "Action or enter pressed");
                    hideNativeKeyboard();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) Log.v(TAG, "Delayed action handling (search)");
                            submitQuery();
                        }
                    }, 500);

                } else if (EditorInfo.IME_ACTION_NONE == action && null != mSearchBarListener) {
                    if (DEBUG) Log.v(TAG, "Escaped North");
                    hideNativeKeyboard();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) Log.v(TAG, "Delayed action handling (escape_north)");
                            mSearchBarListener.onKeyboardDismiss(mSearchQuery);
                        }
                    }, 500);
                } else if (EditorInfo.IME_ACTION_GO == action) {
                    if (DEBUG) Log.v(TAG, "Voice Clicked");
                    hideNativeKeyboard();
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (DEBUG) Log.v(TAG, "Delayed action handling (voice_mode)");
                            mAutoStartRecognition = true;
//                            mSpeechOrbView.requestFocus();
                        }
                    }, 500);
                } else {
                    handled = false;
                }

                return handled;
            }
        });

        mSearchTextEditor.setPrivateImeOptions("escapeNorth,voiceDismiss");



        updateUi(hasFocus());
        updateHint();
    }

    public void setRadioGroup(List<String> keys, int defaultFocus){
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            RadioButton radioButton = new RadioButton(mContext);
            if (i == defaultFocus){
                radioButton.setChecked(true);
            }
            radioButton.setText(key);
            radioButton.setId(i);
            mRadioGroup.addView(radioButton);
        }
        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.e(TAG, "onCheckedChanged: " + checkedId );
                RadioButton radioButton = group.findViewById(checkedId);
                Log.e(TAG, "onCheckedChanged: " + radioButton.getText() );
            }
        });
//        mRadioGroup.getCheckedRadioButtonId();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (DEBUG) Log.v(TAG, "Loading soundPool");
//        mSoundPool = new SoundPool(2, AudioManager.STREAM_SYSTEM, 0);
//        loadSounds(mContext);
    }

    @Override
    protected void onDetachedFromWindow() {
//        stopRecognition();
        if (DEBUG) Log.v(TAG, "Releasing SoundPool");
//        mSoundPool.release();
        super.onDetachedFromWindow();
    }

    /**
     * Sets a listener for when the term search changes
     * @param listener
     */
    public void setSearchBarListener(TvSearchBar.SearchBarListener listener) {
        mSearchBarListener = listener;
    }

    /**
     * Sets the search query
     * @param query the search query to use
     */
    public void setSearchQuery(String query) {
//        stopRecognition();
        mSearchTextEditor.setText(query);
        setSearchQueryInternal(query);
    }

    void setSearchQueryInternal(String query) {
        if (DEBUG) Log.v(TAG, "setSearchQueryInternal " + query);
        if (TextUtils.equals(mSearchQuery, query)) {
            return;
        }
        mSearchQuery = query;

        if (null != mSearchBarListener) {
            mSearchBarListener.onSearchQueryChange(mSearchQuery);
        }
    }

    /**
     * Sets the title text used in the hint shown in the search bar.
     * @param title The hint to use.
     */
    public void setTitle(String title) {
        mTitle = title;
        updateHint();
    }

    /**
     * Returns the current title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the current search bar hint text.
     */
    public CharSequence getHint() {
        return mHint;
    }

    /**
     * Sets the badge drawable showing inside the search bar.
     * @param drawable The drawable to be used in the search bar.
     */
    public void setBadgeDrawable(Drawable drawable) {
        mBadgeDrawable = drawable;
        if (null != mBadgeView) {
            mBadgeView.setImageDrawable(drawable);
            if (null != drawable) {
                mBadgeView.setVisibility(View.VISIBLE);
            } else {
                mBadgeView.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Returns the badge drawable
     */
    public Drawable getBadgeDrawable() {
        return mBadgeDrawable;
    }

    /**
     * Updates the completion list shown by the IME
     *
     * @param completions list of completions shown in the IME, can be null or empty to clear them
     */
    public void displayCompletions(List<String> completions) {
        List<CompletionInfo> infos = new ArrayList<>();
        if (null != completions) {
            for (String completion : completions) {
                infos.add(new CompletionInfo(infos.size(), infos.size(), completion));
            }
        }
        CompletionInfo[] array = new CompletionInfo[infos.size()];
        displayCompletions(infos.toArray(array));
    }

    /**
     * Updates the completion list shown by the IME
     *
     * @param completions list of completions shown in the IME, can be null or empty to clear them
     */
    public void displayCompletions(CompletionInfo[] completions) {
        mInputMethodManager.displayCompletions(mSearchTextEditor, completions);
    }


    void hideNativeKeyboard() {
        mInputMethodManager.hideSoftInputFromWindow(mSearchTextEditor.getWindowToken(),
                InputMethodManager.RESULT_UNCHANGED_SHOWN);
    }

    void showNativeKeyboard() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mSearchTextEditor.requestFocusFromTouch();
                mSearchTextEditor.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN,
                        mSearchTextEditor.getWidth(), mSearchTextEditor.getHeight(), 0));
                mSearchTextEditor.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(),
                        SystemClock.uptimeMillis(), MotionEvent.ACTION_UP,
                        mSearchTextEditor.getWidth(), mSearchTextEditor.getHeight(), 0));
            }
        });
    }

    /**
     * This will update the hint for the search bar properly depending on state and provided title
     */
    private void updateHint() {
        String title = getResources().getString(androidx.leanback.R.string.lb_search_bar_hint);
        if (!TextUtils.isEmpty(mTitle)) {
//            if (isVoiceMode()) {
//                title = getResources().getString(androidx.leanback.R.string.lb_search_bar_hint_with_title_speech, mTitle);
//            } else {
                title = getResources().getString(androidx.leanback.R.string.lb_search_bar_hint_with_title, mTitle);
//            }
        }
//        else if (isVoiceMode()) {
//            title = getResources().getString(androidx.leanback.R.string.lb_search_bar_hint_speech);
//        }
        mHint = title;
        if (mSearchTextEditor != null) {
            mSearchTextEditor.setHint(mHint);
        }
    }

    /**
     * Returns true if is not running Recognizer, false otherwise.
     * @return True if is not running Recognizer, false otherwise.
     */
    public boolean isRecognizing() {
        return mRecognizing;
    }


    /**
     * Sets listener that handles runtime permission requests.
     * @param listener Listener that handles runtime permission requests.
     */
    public void setPermissionListener(SearchBar.SearchBarPermissionListener listener) {
        mPermissionListener = listener;
    }


    void updateUi(boolean hasFocus) {
        if (hasFocus) {
            mBarBackground.setAlpha(mBackgroundSpeechAlpha);
//            if (isVoiceMode()) {
//                mSearchTextEditor.setTextColor(mTextHintColorSpeechMode);
//                mSearchTextEditor.setHintTextColor(mTextHintColorSpeechMode);
//            } else {
                mSearchTextEditor.setTextColor(mTextColorSpeechMode);
                mSearchTextEditor.setHintTextColor(mTextHintColorSpeechMode);
//            }
        } else {
            mBarBackground.setAlpha(mBackgroundAlpha);
            mSearchTextEditor.setTextColor(mTextColor);
            mSearchTextEditor.setHintTextColor(mTextHintColor);
        }

        updateHint();
    }


    void submitQuery() {
        if (!TextUtils.isEmpty(mSearchQuery) && null != mSearchBarListener) {
            String key = "";
            if (mRadioGroup!= null){
                int checkedRadioButtonId = mRadioGroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId!= -1){
                    RadioButton radioButton = mRadioGroup.findViewById(checkedRadioButtonId);
                    key = radioButton.getText().toString();
                }
            }
            mSearchBarListener.onSearchQuerySubmit(mSearchQuery, key);
        }
    }


    @Override
    public void setNextFocusDownId(int viewId) {
//        mSpeechOrbView.setNextFocusDownId(viewId);
        mSearchTextEditor.setNextFocusDownId(viewId);
    }

}
