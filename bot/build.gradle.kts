import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0"
    id("org.springframework.boot.aot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") apply true
    kotlin("plugin.spring") version "2.0.0"
    id("com.google.cloud.tools.jib") version "3.4.3"
}

group = "io.github.artemptushkin"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation(
        platform("com.google.cloud:spring-cloud-gcp-dependencies:5.3.0")
    )
    implementation(
        platform("com.google.cloud:libraries-bom:26.40.0")
    )
    implementation("com.google.cloud:spring-cloud-gcp-starter-logging")
    implementation("com.google.cloud:spring-cloud-gcp-starter-data-firestore")

    implementation("io.github.artemptushkin:kotlin-telegram-bot:6.1.4")
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.github.lambdua:service:0.20.7")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.tngtech.archunit:archunit:1.3.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn("compileAotJava")
    dependsOn("processAotResources")

    from("build/classes/java/aot")
    from("build/generated/aotClasses")
    from("build/resources/aot")
}

jib {
    containerizingMode = "packaged"
    container {
        jvmFlags = listOf("-Dspring.aot.enabled=true")
        mainClass = "io.github.artemptushkin.ai.assistants.AiTelegramAssistantsApplicationKt"
    }
    from {
        image = "eclipse-temurin:21-jre"
        platforms {
            platform {
                os = "linux"
                architecture = "arm64"
            }
            platform {
                os = "linux"
                architecture = "amd64"
            }
        }
    }
    extraDirectories {
        paths {
            path {
                setFrom("build/libs/cds")
                into = "/cds"
            }
            path {
                setFrom("../config")
                into = "/config"
            }
        }
    }
}
