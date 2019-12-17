import com.google.protobuf.gradle.*
import com.google.protobuf.gradle.protoc

val grpcVersion = "1.23.0"
val protoVer = "3.9.1"
val grpcStarterVer = "2.5.1.RELEASE"


plugins {
    java
    id("com.google.protobuf") version "0.8.10"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protoVer"
    }
    plugins {
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.23.0"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                id("grpc")
            }
        }
    }
}

dependencies {
    implementation("io.grpc", "grpc-netty-shaded", grpcVersion)
    implementation("io.grpc", "grpc-protobuf", grpcVersion)
    implementation("io.grpc", "grpc-stub", grpcVersion)

    implementation("net.devh:grpc-server-spring-boot-starter:2.5.1.RELEASE")

    compile("org.springframework.boot:spring-boot-starter-web:2.2.2.RELEASE")
    testCompile("org.springframework.boot:spring-boot-starter-test:2.2.2.RELEASE")
}

val protoJavaDir:String = protobuf.protobuf.generatedFilesBaseDir

sourceSets["main"].java.srcDirs(
        protoJavaDir.plus("/main/java"),
        protoJavaDir.plus("/main/grpc")
)

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_11
}

tasks {
    test {
        useJUnitPlatform()
        testLogging.showStackTraces = true
        testLogging.showCauses = true
        testLogging.showExceptions = true
        testLogging.showStandardStreams = true
    }
}