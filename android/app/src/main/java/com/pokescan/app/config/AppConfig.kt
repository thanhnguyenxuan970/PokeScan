package com.snapdex.app.config

import com.snapdex.app.BuildConfig

object AppConfig {
    val BASE_URL: String = BuildConfig.BASE_URL
    // LAUNCH_BLOCKER: create github.io/snapdex-privacy repo before release
    const val PRIVACY_POLICY_URL = "https://thanhnguyenxuan970.github.io/snapdex-privacy"
}
