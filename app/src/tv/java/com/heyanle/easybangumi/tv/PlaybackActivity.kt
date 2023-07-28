package com.heyanle.easybangumi.tv

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.WindowManager
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.heyanle.easybangumi.R

/** Loads [PlaybackVideoFragment]. */
class PlaybackActivity : FragmentActivity() {
    private var gamepadTriggerPressed = false
    private var mPlaybackVideoFragment: PlaybackVideoFragment? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tv_playback)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        val fragment: Fragment? =
            getSupportFragmentManager().findFragmentByTag(getString(R.string.playback_tag))
        if (fragment is PlaybackVideoFragment) {
            mPlaybackVideoFragment = fragment as PlaybackVideoFragment
        }
    }

    override protected fun onStop() {
        super.onStop()
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        finish()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BUTTON_R1) {
            mPlaybackVideoFragment?.skipToNext()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L1) {
            mPlaybackVideoFragment?.skipToPrevious()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_L2) {
            mPlaybackVideoFragment?.rewind()
        } else if (keyCode == KeyEvent.KEYCODE_BUTTON_R2) {
            mPlaybackVideoFragment?.fastForward()
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        // This method will handle gamepad events.
        if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) > GAMEPAD_TRIGGER_INTENSITY_ON
            && !gamepadTriggerPressed
        ) {
            mPlaybackVideoFragment?.rewind()
            gamepadTriggerPressed = true
        } else if (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) > GAMEPAD_TRIGGER_INTENSITY_ON
            && !gamepadTriggerPressed
        ) {
            mPlaybackVideoFragment?.fastForward()
            gamepadTriggerPressed = true
        } else if (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) < GAMEPAD_TRIGGER_INTENSITY_OFF
            && event.getAxisValue(MotionEvent.AXIS_RTRIGGER) < GAMEPAD_TRIGGER_INTENSITY_OFF
        ) {
            gamepadTriggerPressed = false
        }
        return super.onGenericMotionEvent(event)
    }

    companion object {
        private const val GAMEPAD_TRIGGER_INTENSITY_ON = 0.5f

        // Off-condition slightly smaller for button debouncing.
        private const val GAMEPAD_TRIGGER_INTENSITY_OFF = 0.45f

        const val SHARED_ELEMENT_NAME = "hero"

        const val VIDEO = "VIDEO"
    }
}