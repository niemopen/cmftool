import org.gradle.jvm.application.tasks.CreateStartScripts

plugins {
    application
}

dependencies {
    implementation(project(":lib-cmf"))
    implementation(project(":lib-util"))

    implementation("net.sf.saxon:Saxon-HE:12.5")
    implementation("info.picocli:picocli:4.7.7")
    implementation("commons-io:commons-io:2.18.0")
    implementation("org.apache.logging.log4j:log4j-core:2.24.3")
    implementation("org.apache.logging.log4j:log4j-api:2.24.3")
}

application {
    mainClass.set("org.mitre.niem.scheval.SCHEval")
}

tasks.named<CreateStartScripts>("startScripts") {
    applicationName = "scheval"
}
