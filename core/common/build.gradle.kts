plugins {
    alias(libs.plugins.wifiwidget.library)
    alias(libs.plugins.wifiwidget.hilt)
}

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.annotation)

    implementation(libs.w2sv.androidutils)
    implementation(libs.w2sv.datastoreutils.preferences)
    implementation(libs.slimber)

    testImplementation(libs.bundles.unitTest)
}
