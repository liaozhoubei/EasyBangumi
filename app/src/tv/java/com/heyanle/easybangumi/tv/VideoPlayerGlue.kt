package com.heyanle.easybangumi.tv

import android.content.Context
import android.util.Log
import androidx.leanback.media.PlaybackTransportControlGlue
import androidx.leanback.widget.Action
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.PlaybackControlsRow.*
import com.google.android.exoplayer2.ext.leanback.LeanbackPlayerAdapter
import java.util.concurrent.TimeUnit

class VideoPlayerGlue(
    context: Context?,
    playerAdapter: LeanbackPlayerAdapter?,
    private val mActionListener: OnActionClickedListener
) : PlaybackTransportControlGlue<LeanbackPlayerAdapter>(context, playerAdapter) {
    /** Listens for when skip to next and previous actions have been dispatched.  */
    interface OnActionClickedListener {
        /** Skip to the previous item in the queue.  */
        fun onPrevious()

        /** Skip to the next item in the queue.  */
        fun onNext()
        /*收藏*/
        fun star()

        fun openOutPlayer()
    }

    private val mRepeatAction: RepeatAction

    private val mSkipPreviousAction: SkipPreviousAction
    private val mSkipNextAction: SkipNextAction
    private val mFastForwardAction: FastForwardAction
    private val mRewindAction: RewindAction
    private val mFavoriteAction:FavoriteAction
    private val mOutPlayerAction:OutPlayerAction
    override fun onCreatePrimaryActions(adapter: ArrayObjectAdapter) {
        // Order matters, super.onCreatePrimaryActions() will create the play / pause action.
        // Will display as follows:
        // play/pause, previous, rewind, fast forward, next
        //   > /||      |<        <<        >>         >|
        super.onCreatePrimaryActions(adapter)
        adapter.add(mSkipPreviousAction)
        adapter.add(mRewindAction)
        adapter.add(mFastForwardAction)
        adapter.add(mSkipNextAction)
    }



    override fun onCreateSecondaryActions(adapter: ArrayObjectAdapter) {
        super.onCreateSecondaryActions(adapter)

//        adapter.add(mRepeatAction)
        adapter.add(mFavoriteAction)
        adapter.add(mOutPlayerAction)
    }

    override fun onActionClicked(action: Action) {
        if (shouldDispatchAction(action)) {
            dispatchAction(action)
            return
        }
        // Super class handles play/pause and delegates to abstract methods next()/previous().
        super.onActionClicked(action)
    }

    // Should dispatch actions that the super class does not supply callbacks for.
    private fun shouldDispatchAction(action: Action): Boolean {
//        return action === mRewindAction || action === mFastForwardAction || action === mThumbsDownAction || action === mThumbsUpAction || action === mRepeatAction
        return action === mRewindAction || action === mFastForwardAction
                || action === mFavoriteAction  || action === mRepeatAction || action === mOutPlayerAction
    }

    private fun dispatchAction(action: Action) {
        // Primary actions are handled manually.
        if (action === mRewindAction) {
            rewind()
        } else if (action === mFastForwardAction) {
            fastForward()
        }
        else if (action === mFavoriteAction){
            mActionListener.star()

            val index = if (mFavoriteAction.index < mFavoriteAction.getActionCount() - 1) mFavoriteAction.index + 1 else 0
            mFavoriteAction.index = index
            Log.e("dispatchAction", "dispatchAction: ${mFavoriteAction.index}")
            notifyActionChanged(
                mFavoriteAction,
                controlsRow.secondaryActionsAdapter as ArrayObjectAdapter
            )
        }else if (action === mOutPlayerAction){
            mActionListener.openOutPlayer()
        }
        else if (action is MultiAction) {
            val multiAction = action
            multiAction.nextIndex()
            // Notify adapter of action changes to handle secondary actions, such as, thumbs up/down
            // and repeat.
            notifyActionChanged(
                multiAction,
                controlsRow.secondaryActionsAdapter as ArrayObjectAdapter
            )
        }
    }

    public fun star(star:Boolean){
        if (star){// 0为收藏
            mFavoriteAction.index = 0
        }else{
            mFavoriteAction.index = 1
        }
        notifyActionChanged(
            mFavoriteAction,
            controlsRow.secondaryActionsAdapter as ArrayObjectAdapter
        )
    }

    private fun notifyActionChanged(
        action: MultiAction, adapter: ArrayObjectAdapter?
    ) {
        if (adapter != null) {
            val index = adapter.indexOf(action)
            if (index >= 0) {
                adapter.notifyArrayItemRangeChanged(index, 1)
            }
        }
    }

    override fun next() {
        mActionListener.onNext()
    }

    override fun previous() {
        mActionListener.onPrevious()
    }

    /** Skips backwards 10 seconds.  */
    fun rewind() {
        var newPosition = currentPosition - TEN_SECONDS
        newPosition = if (newPosition < 0) 0 else newPosition
        playerAdapter!!.seekTo(newPosition)
    }

    var preFastForwardTime = 0L;
    /** Skips forward 10 seconds.  */
    fun fastForward() {
        if (duration > -1) {
            var newPosition = currentPosition + TEN_SECONDS
            // 当按下一次快进小于2s时以30s时间快进
            if ((System.currentTimeMillis()/1000) - preFastForwardTime < 2){
                newPosition = currentPosition + (3*TEN_SECONDS)
            }
            preFastForwardTime = System.currentTimeMillis()/1000
            newPosition = if (newPosition > duration) duration else newPosition
            playerAdapter!!.seekTo(newPosition)
        }
    }

    companion object {
        private val TEN_SECONDS = TimeUnit.SECONDS.toMillis(10)
    }

    init {
        mSkipPreviousAction = SkipPreviousAction(context)
        mSkipNextAction = SkipNextAction(context)
        mFastForwardAction = FastForwardAction(context)
        mRewindAction = RewindAction(context)

        mRepeatAction = RepeatAction(context)
        mFavoriteAction = FavoriteAction(context)
        mFavoriteAction.index = FavoriteAction.INDEX_OUTLINE
        mOutPlayerAction= OutPlayerAction(context)
    }
}
