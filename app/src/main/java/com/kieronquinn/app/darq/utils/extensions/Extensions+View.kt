package com.kieronquinn.app.darq.utils.extensions

import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.isVisible
import com.kieronquinn.app.darq.R

fun View.slideOut(callback: () -> Unit): Animation {
    AnimationUtils.loadAnimation(context, R.anim.slide_out_bottom).apply {
        onEnd {
            isVisible = false
            callback.invoke()
        }
    }.also {
        startAnimation(it)
        return it
    }
}

fun View.slideIn(callback: () -> Unit): Animation {
    isVisible = true
    AnimationUtils.loadAnimation(context, R.anim.slide_in_bottom).apply {
        onEnd {
            callback.invoke()
        }
    }.also {
        startAnimation(it)
        return it
    }
}

fun Animation.onEnd(callback: () -> Unit){
    setAnimationListener(object: Animation.AnimationListener {
        override fun onAnimationRepeat(animation: Animation?) {}
        override fun onAnimationStart(animation: Animation?) {}
        override fun onAnimationEnd(animation: Animation?) {
            callback.invoke()
        }
    })
}

fun SwitchCompat.applyMD3SwitchMonet() {
    setTrackTintList(null)
    setThumbTintList(null)
}

fun View.findSwitch(): SwitchCompat? {
    if (this is SwitchCompat) return this
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val child = getChildAt(i).findSwitch()
            if (child != null) return child
        }
    }
    return null
}