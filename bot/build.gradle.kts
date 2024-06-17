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
    sourceCompatibility = JavaVersion.VERSION_21
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
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// https://github.com/GoogleContainerTools/jib/tree/master/jib-gradle-plugin
jib {
    container {
        jvmFlags = listOf("-Dspring.aot.enabled=true")
    }
    from {
        image = "eclipse-temurin:21-jre"
    }
    extraDirectories {
        paths {
            path {
                setFrom("../config")
                into = "/config"
            }
        }
    }
}
