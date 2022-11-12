val deps: Map<String, String> by extra

dependencies {
    implementation(project(":RoaringBitmap"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:${deps["jupiter"]}")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${deps["jupiter"]}")
    testImplementation("com.google.guava:guava:${deps["guava"]}")
    testImplementation("com.fasterxml.jackson.core", "jackson-databind", "2.10.3")
}

tasks.test {
    // set the property on the CLI with -P or add to gradle.properties to enable tests
    if (!project.hasProperty("roaringbitmap.fuzz-tests")) {
       // exclude("**")
    }
    useJUnitPlatform()
    failFast = true
    testLogging {
        // We exclude 'passed' events
        events( "skipped", "failed")
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        // Helps investigating OOM. But too verbose to be activated by default
        // showStandardStreams = true
    }

    // Define the memory requirements of tests, to prevent issues in CI while OK locally
    minHeapSize = "2G"
    maxHeapSize = "2G"
}
