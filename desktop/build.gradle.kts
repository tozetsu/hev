import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import javax.inject.Inject

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.components.resources)
    implementation(libs.kotlinx.coroutines.swing)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.dbus.java.core)
    implementation(libs.dbus.java.transport)
    runtimeOnly(libs.slf4j.nop)
}

compose.resources {
    packageOfResClass = "ai.hev.desktop.resources"
}

compose.desktop {
    application {
        mainClass = "hev"
        nativeDistributions {
            packageName = "hev"
            packageVersion = version.toString()
            // From suggestRuntimeModules, plus the EC provider TLS needs on JDK 21.
            modules("java.instrument", "java.management", "jdk.security.auth", "jdk.unsupported", "jdk.crypto.ec")
        }
    }
}

/** Downloads [url] to [output] and keeps it only if its SHA-256 is [sha256]. */
abstract class VerifiedDownload : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun download() {
        val bytes = URI(url.get()).toURL().openStream().use { it.readBytes() }
        val actual = "%064x".format(BigInteger(1, MessageDigest.getInstance("SHA-256").digest(bytes)))
        check(actual == sha256.get()) { "${url.get()} has SHA-256 $actual, expected ${sha256.get()}" }
        output.get().asFile.apply {
            writeBytes(bytes)
            setExecutable(true)
        }
    }
}

/** Builds [output] from [appDir] with appimagetool and the given type 2 runtime, without needing FUSE. */
abstract class AppImage : DefaultTask() {
    @get:InputFile
    abstract val tool: RegularFileProperty

    @get:InputFile
    abstract val runtime: RegularFileProperty

    @get:InputDirectory
    abstract val appDir: DirectoryProperty

    @get:OutputFile
    abstract val output: RegularFileProperty

    @get:Inject
    abstract val exec: ExecOperations

    @TaskAction
    fun build() {
        exec.exec {
            executable(tool.get().asFile)
            environment("APPIMAGE_EXTRACT_AND_RUN", "1")
            environment("ARCH", "x86_64")
            args("--no-appstream", "--runtime-file", runtime.get().asFile, appDir.get().asFile, output.get().asFile)
        }
    }
}

val appImageTools = layout.buildDirectory.dir("appimage/tools")

val downloadAppImageTool = tasks.register<VerifiedDownload>("downloadAppImageTool") {
    url = "https://github.com/AppImage/appimagetool/releases/download/1.9.1/appimagetool-x86_64.AppImage"
    sha256 = "ed4ce84f0d9caff66f50bcca6ff6f35aae54ce8135408b3fa33abfc3cb384eb0"
    output = appImageTools.map { it.file("appimagetool-1.9.1-x86_64.AppImage") }
}

val downloadAppImageRuntime = tasks.register<VerifiedDownload>("downloadAppImageRuntime") {
    url = "https://github.com/AppImage/type2-runtime/releases/download/20251108/runtime-x86_64"
    sha256 = "2fca8b443c92510f1483a883f60061ad09b46b978b2631c807cd873a47ec260d"
    output = appImageTools.map { it.file("runtime-20251108-x86_64") }
}

/** Uses the file the Gradle property [name] points at, if given, instead of downloading it. */
fun RegularFileProperty.setLocalOr(name: String, download: TaskProvider<VerifiedDownload>) {
    val local = providers.gradleProperty(name).orNull
    if (local != null) set(layout.projectDirectory.file(local)) else set(download.flatMap { it.output })
}

val appImageDir = tasks.register<Sync>("appImageDir") {
    from(tasks.named("createDistributable").map { layout.buildDirectory.dir("compose/binaries/main/app/hev") })
    from("appimage")
    from("src/main/composeResources/drawable/hev.png")
    into(layout.buildDirectory.dir("appimage/hev.AppDir"))
}

tasks.register<AppImage>("packageAppImage") {
    group = "compose desktop"
    description = "Packages the app as a single x86_64 AppImage file."
    tool.setLocalOr("appimagetool", downloadAppImageTool)
    runtime.setLocalOr("appimageRuntime", downloadAppImageRuntime)
    appDir = layout.dir(appImageDir.map { it.destinationDir })
    output = layout.buildDirectory.file("appimage/hev-$version-x86_64.AppImage")
}
