plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    implementation(libs.saxon.he)
    implementation(libs.xerces)
    implementation(libs.xmlresolver)
    implementation(libs.xalan)
    implementation(libs.xalan.serializer)
    implementation(libs.commons.io)
    implementation(libs.commons.lang3)
    implementation(libs.log4j.core)
    implementation(libs.log4j.api)
    implementation(libs.javatuples)
    implementation(libs.picocli)

    testImplementation(libs.assertj.core)
    testImplementation(libs.logcaptor)
}
