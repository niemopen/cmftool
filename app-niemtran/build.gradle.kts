plugins {
    application
}

dependencies {
    implementation(projects.libCmf)
    implementation(projects.libUtil)

    implementation(libs.picocli)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.log4j.core)
    implementation(libs.gson)
    implementation(libs.jena.arq)

    runtimeOnly(libs.logback.classic)

    testImplementation(libs.assertj.core)
    testImplementation(libs.logcaptor)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit.jupiter)
}
