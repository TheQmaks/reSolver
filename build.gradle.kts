plugins {
    id("java")
}

group = "cli.li"
version = "1.1"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("net.portswigger.burp.extensions:montoya-api:2024.12")

    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20250107")
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