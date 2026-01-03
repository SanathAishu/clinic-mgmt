plugins {
    java
    id("io.quarkus")
}

dependencies {
    // Quarkus Platform BOM
    implementation(enforcedPlatform("io.quarkus.platform:quarkus-bom:3.17.0"))

    // Quarkus Core
    implementation("io.quarkus:quarkus-core")
    implementation("io.quarkus:quarkus-arc")

    // REST and Exception Handling
    implementation("io.quarkus:quarkus-rest")
    implementation("io.quarkus:quarkus-rest-jackson")

    // Health Checks
    implementation("io.quarkus:quarkus-smallrye-health")

    // Quarkus SmallRye JWT
    implementation("io.quarkus:quarkus-smallrye-jwt")

    // Logging
    implementation("io.quarkus:quarkus-logging-json")

    // Reactive Database
    implementation("io.quarkus:quarkus-hibernate-reactive-panache")
    implementation("io.quarkus:quarkus-reactive-pg-client")

    // Flyway for migrations
    implementation("io.quarkus:quarkus-flyway")
    implementation("io.quarkus:quarkus-jdbc-postgresql")

    // Jackson for JSON
    implementation("io.quarkus:quarkus-jackson")

    // Redis
    implementation("io.quarkus:quarkus-redis-client")

    // Validation
    implementation("io.quarkus:quarkus-hibernate-validator")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // Testing
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("io.rest-assured:rest-assured")
}

tasks.test {
    systemProperty("java.util.logging.manager", "org.jboss.logmanager.LogManager")
}
