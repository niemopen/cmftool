plugins {
    application
}

dependencies {
    implementation(projects.libUtil)

    implementation(libs.saxon.he)
    implementation(libs.picocli)
    implementation(libs.commons.io)
    implementation(libs.log4j.core)
    implementation(libs.log4j.api)
}
