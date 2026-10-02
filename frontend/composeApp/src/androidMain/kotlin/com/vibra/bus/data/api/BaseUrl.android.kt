package com.vibra.bus.data.api

import com.vibra.bus.BuildConfig

actual val BUILD_BASE_URL: String = BuildConfig.BASE_URL_ANDROID

actual val DEFAULT_ORG_SLUG: String = BuildConfig.DEFAULT_ORG_SLUG

actual val ALLOW_CLEARTEXT: Boolean = BuildConfig.DEBUG
