plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.asciidoctor.jvm.convert") version "3.3.2"
}

val snippetsDir = file("build/generated-snippets")

dependencies {
    implementation(project(":point-core"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    runtimeOnly("com.h2database:h2")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
}

tasks.test {
    outputs.dir(snippetsDir)
}

tasks.named("asciidoctor", org.asciidoctor.gradle.jvm.AsciidoctorTask::class) {
    inputs.dir(snippetsDir)
    dependsOn(tasks.test)
    attributes(mapOf("snippets" to snippetsDir.absolutePath))
    baseDirFollowsSourceFile()
}

tasks.register<Copy>("copyDocs") {
    dependsOn(tasks.named("asciidoctor"))
    from("build/docs/asciidoc")
    into("build/docs/static/docs")
}

tasks.register<Copy>("copyDocsToResources") {
    dependsOn(tasks.named("asciidoctor"))
    from("build/docs/asciidoc")
    into("src/main/resources/static/docs")
}

tasks.bootRun {
    dependsOn(tasks.named("copyDocs"))
    classpath += files("build/docs/static")
}

tasks.bootJar {
    dependsOn(tasks.named("asciidoctor"))
    from("build/docs/asciidoc") {
        into("static/docs")
    }
}
