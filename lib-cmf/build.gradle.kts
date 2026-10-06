plugins {
    `java-library`
    `maven-publish`
}

dependencies {
    api(projects.libUtil)

    implementation(libs.xerces)
    implementation(libs.xalan)
    implementation(libs.xalan.serializer)
    implementation(libs.commons.io)
    implementation(libs.gson)
    implementation(libs.commons.lang3)
    implementation(libs.log4j.core)
    implementation(libs.log4j.api)
    implementation(libs.javatuples)

    testImplementation(libs.mockito.core)
    testImplementation(libs.assertj.core)
    testImplementation(libs.logcaptor)
    testImplementation(libs.json.path)
    testImplementation(libs.json.path.assert)
    testImplementation(libs.json.schema.validator)
    testImplementation(libs.jackson.databind)
}
