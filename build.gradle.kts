plugins {
    id("java")
}

group = "ch.epfl.chacun.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

sourceSets {
    main {
        java {
            setSrcDirs(listOf("src"))
        }
    }

    test {
        java {
            setSrcDirs(listOf("test"))
        }
    }
}

var ENABLE_PREVIEW = "--enable-preview"

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add(ENABLE_PREVIEW)
}

tasks.withType<Test>().configureEach {
    jvmArgs(ENABLE_PREVIEW)
    useJUnitPlatform()
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs(ENABLE_PREVIEW)
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "ch.epfl.chacun.Main"
        )
    }
}

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS
