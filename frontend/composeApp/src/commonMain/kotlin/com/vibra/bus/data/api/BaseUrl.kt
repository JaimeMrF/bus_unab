package com.vibra.bus.data.api

/** URL base del build (Android: BuildConfig, iOS: clave ApiBaseUrl del Info.plist; vacía si no está). */
expect val BUILD_BASE_URL: String

/** Slug de organización por defecto de este build (vacío = pedir el código al usuario). */
expect val DEFAULT_ORG_SLUG: String

/** true solo en builds debug: único caso en que se aceptan servidores http (cleartext). */
expect val ALLOW_CLEARTEXT: Boolean
