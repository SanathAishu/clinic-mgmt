plugins {
    java
    id("io.quarkus")
}

dependencies {
    // Quarkus Platform BOM
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.17.0"))

    // Common library
    implementation(project(":hospital-common-lib"))

    // Reactive REST
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // Security - BCrypt for password hashing
    implementation("io.quarkus:quarkus-elytron-security-common")

    // Reactive Database
    implementation("io.quarkus:quarkus-hibernate-reactive-panache")
    implementation("io.quarkus:quarkus-reactive-pg-client")

    // RabbitMQ Reactive Messaging
    implementation("io.quarkus:quarkus-messaging-rabbitmq")

    // Redis Cache
    implementation("io.quarkus:quarkus-redis-client")
    implementation("io.quarkus:quarkus-cache")

    // JWT Security
    implementation("io.quarkus:quarkus-smallrye-jwt")
    implementation("io.quarkus:quarkus-smallrye-jwt-build")

    // Service Discovery (using basic config for now)
    implementation("io.quarkus:quarkus-config-yaml")

    // Health and Metrics
    implementation("io.quarkus:quarkus-smallrye-health")
    implementation("io.quarkus:quarkus-micrometer-registry-prometheus")

    // Validation
    implementation("io.quarkus:quarkus-hibernate-validator")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.quarkus:quarkus-junit5-mockito")
    testImplementation("io.rest-assured:rest-assured")
    testImplementation("org.assertj:assertj-core:3.25.1")
}

tasks.test {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
