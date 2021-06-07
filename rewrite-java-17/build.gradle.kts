import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val compiler = javaToolchains.compilerFor {
    languageVersion.set(JavaLanguageVersion.of(16))
}

val maybeExe = if(getCurrentOperatingSystem().isWindows) {
    ".exe"
} else {
    ""
}
val javac = compiler.get().metadata.installationPath.file("bin/javac${maybeExe}")

dependencies {
    compileOnly("org.slf4j:slf4j-api:1.7.+")

    implementation(project(":rewrite-java"))
    implementation("org.ow2.asm:asm:latest.release")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    testImplementation("org.junit.jupiter:junit-jupiter-api:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    testImplementation(project(":rewrite-test"))
    testRuntimeOnly("ch.qos.logback:logback-classic:1.0.13")
}

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jdkHome = compiler.get().metadata.installationPath.asFile.absolutePath
    }
}

tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_16.toString()
    targetCompatibility = JavaVersion.VERSION_16.toString()
    options.compilerArgs.clear() // remove `--release 8` set in root gradle build
    options.isFork = true
    options.forkOptions.executable = javac.toString()
    options.compilerArgs.addAll(listOf(
        "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
        "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED"
    ))
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs = listOf("-XX:+UnlockDiagnosticVMOptions", "-XX:+ShowHiddenFrames",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED")
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(16))
    })
}

tasks.withType<Javadoc>().configureEach {
    executable = javaToolchains.javadocToolFor {
        languageVersion.set(JavaLanguageVersion.of(16))
    }.get().executablePath.toString()

    exclude("**/Java17Parser**")
}
