plugins {
    java
    id("io.quarkus")
}

dependencies {
    // Quarkus Platform BOM
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.17.0"))

    // Common library
    implementation(project(":hospital-common-lib"))

    // Vert.x for gateway routing
    implementation("io.quarkus:quarkus-vertx")
    implementation("io.quarkus:quarkus-vertx-http")

    // Reactive REST client
    implementation("io.quarkus:quarkus-rest-client")

    // JWT Security
    implementation("io.quarkus:quarkus-smallrye-jwt")

    // Service Discovery with Stork
    implementation("io.quarkus:quarkus-config-yaml")
    implementation("io.quarkus:quarkus-smallrye-stork")
    implementation("io.smallrye.stork:stork-service-discovery-consul")

    // Health and Metrics
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")

    // Redis for rate limiting
    implementation("io.quarkus:quarkus-redis-client")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

tasks.test {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
