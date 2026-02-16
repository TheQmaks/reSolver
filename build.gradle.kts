plugins {
    id("java")
    id("checkstyle")
    id("com.github.spotbugs") version "6.4.8"
}

group = "cli.li"
version = "2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:2025.12")

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core:3.27.3")
}

tasks.test {
    useJUnitPlatform()
}

// --- Static Analysis ---

checkstyle {
    toolVersion = "10.21.4"
    configFile = file("config/checkstyle/checkstyle.xml")
    isIgnoreFailures = false         // fail the build on violations
    isShowViolations = true
}

spotbugs {
    ignoreFailures.set(false)        // fail the build on bugs
    effort.set(com.github.spotbugs.snom.Effort.DEFAULT)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM)
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask>().configureEach {
    reports.create("html") { required.set(true) }
    reports.create("xml") { required.set(false) }
}

tasks.jar {
    archiveFileName.set("resolver-v$version.jar")

    manifest {
        attributes["Plugin"] = "reSolver"
        attributes["Version"] = version
        attributes["Author"] = "Anatoliy Fedorenko @ cli.li"
    }

    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })

    // Handle duplicate files
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
