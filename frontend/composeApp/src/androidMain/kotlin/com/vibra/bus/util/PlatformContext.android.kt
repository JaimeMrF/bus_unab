package com.vibra.bus.util

import android.content.Context

actual typealias PlatformContext = Context

private lateinit var appContext: Context

fun initPlatformContext(context: Context) {
    appContext = context.applicationContext
}

actual fun getPlatformContext(): PlatformContext = appContext
