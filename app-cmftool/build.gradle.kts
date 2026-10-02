plugins {
    application
}

dependencies {
    implementation(projects.libCmf)
    implementation(projects.libUtil)

    implementation(libs.picocli)
    implementation(libs.gson)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.log4j.api)
    implementation(libs.log4j.core)
}
