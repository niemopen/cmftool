import org.gradle.jvm.application.tasks.CreateStartScripts

plugins {
    application
}

dependencies {
    implementation(project(":lib-cmf"))
    implementation(project(":lib-util"))

    implementation("info.picocli:picocli:4.7.7")
    implementation("commons-io:commons-io:2.18.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.apache.jena:jena-arq:5.6.0")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.16")

    testImplementation(libs.junit.jupiter)
    testImplementation("org.assertj:assertj-core:3.26.0")
    testImplementation("io.github.hakky54:logcaptor:2.9.3")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("org.mitre.niem.translate.NIEMTran")
}

tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "niemtran"
}
