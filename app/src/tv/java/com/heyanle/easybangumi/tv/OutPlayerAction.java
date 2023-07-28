package com.heyanle.easybangumi.tv;

import android.content.Context;

import androidx.leanback.widget.PlaybackControlsRow;

import com.heyanle.easybangumi.R;

public class OutPlayerAction extends PlaybackControlsRow.MultiAction {

    /**
     * Constructor
     * @param context Context used for loading resources.
     */
    public OutPlayerAction(Context context) {
        super(R.drawable.ic_24_external_player);
        setIcon(context.getDrawable(R.drawable.ic_24_external_player));

    }
}
