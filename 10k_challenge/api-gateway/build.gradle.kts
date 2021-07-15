import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.testing.logging.TestLogEvent.*

plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("io.freefair.lombok") version "6.0.0-m2"
}

group = "br.com.diegoalexandro.challenge"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val vertxVersion = "4.1.1"
val junitJupiterVersion = "5.7.0"

val mainVerticleName = "br.com.diegoalexandro.challenge.api_gateway.Main"
val launcherClassName = "br.com.diegoalexandro.challenge.api_gateway.Main"

val watchForChange = "src/**/*"
val doOnChange = "${projectDir}/gradlew classes"

application {
    mainClass.set(launcherClassName)
}

dependencies {
    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    implementation("io.vertx:vertx-web-client")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-auth-jwt")
    implementation("io.vertx:vertx-config")
    implementation("io.vertx:vertx-config-yaml")
    compileOnly("org.projectlombok:lombok:1.18.20")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("io.vertx:vertx-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("fat")
    manifest {
        attributes(mapOf("Main-Verticle" to mainVerticleName))
    }
    mergeServiceFiles()
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events = setOf(PASSED, SKIPPED, FAILED)
    }
}

tasks.withType<JavaExec> {
    args = listOf("run", mainVerticleName, "--redeploy=$watchForChange", "--launcher-class=$launcherClassName", "--on-redeploy=$doOnChange")
}
