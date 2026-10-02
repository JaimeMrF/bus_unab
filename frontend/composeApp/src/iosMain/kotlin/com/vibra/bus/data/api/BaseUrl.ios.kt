package com.vibra.bus.data.api

import platform.Foundation.NSBundle

/** Se define en el Info.plist de la app (ApiBaseUrl); sin él, el usuario fija el servidor en runtime. */
actual val BUILD_BASE_URL: String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("ApiBaseUrl") as? String).orEmpty()

actual val DEFAULT_ORG_SLUG: String =
    (NSBundle.mainBundle.objectForInfoDictionaryKey("DefaultOrgSlug") as? String).orEmpty()

actual val ALLOW_CLEARTEXT: Boolean = false
