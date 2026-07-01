import org.gradle.jvm.application.tasks.CreateStartScripts

plugins {
    application
}

dependencies {
    implementation(project(":lib-cmf"))
    implementation(project(":lib-util"))

    implementation("info.picocli:picocli:4.7.6")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("commons-io:commons-io:2.18.0")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
}

application {
    mainClass.set("org.mitre.niem.cmftool.CMFTool")
}

tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "cmftool"
}
