package com.heyanle.easybangumi.tv;

import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.leanback.widget.PlaybackControlsRow;

import com.heyanle.easybangumi.R;

public class FavoriteAction extends PlaybackControlsRow.MultiAction {
    /**
     * Action index for the solid thumb icon.
     */
    public static final int INDEX_SOLID = 0;

    /**
     * Action index for the outline thumb icon.
     */
    public static final int INDEX_OUTLINE = 1;

    /**
     * Constructor
     * @param context Context used for loading resources.
     */
    public FavoriteAction(Context context) {
        super(R.drawable.ic_24_not_favourite);
        Drawable[] drawables = new Drawable[2];
        drawables[INDEX_SOLID] = context.getDrawable(R.drawable.ic_24_is_favourite);
        drawables[INDEX_OUTLINE] = context.getDrawable(R.drawable.ic_24_not_favourite);
        setDrawables(drawables);

    }
}
