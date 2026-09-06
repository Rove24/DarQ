package com.kieronquinn.app.darq.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

fun Context.openLink(url: String){
    try {
        startActivity(Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(url)
        })
    }catch (e: ActivityNotFoundException){
        //No browser
    }
}

object Links {
    const val LINK_GITHUB = "https://github.com/Rove24/DarQ"
}