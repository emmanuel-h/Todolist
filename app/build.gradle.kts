import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.androidx.baselineprofile)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.compose)
}

val releaseKeystoreFile: String? =
    findProperty("RELEASE_KEYSTORE_FILE")?.toString()
        ?: System.getenv("RELEASE_KEYSTORE_FILE")
val releaseKeystorePassword: String? =
    findProperty("RELEASE_KEYSTORE_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
val releaseKeyAlias: String? =
    findProperty("RELEASE_KEY_ALIAS")?.toString()
        ?: System.getenv("RELEASE_KEY_ALIAS")
val releaseKeyPassword: String? =
    findProperty("RELEASE_KEY_PASSWORD")?.toString()
        ?: System.getenv("RELEASE_KEY_PASSWORD")

val hasSigningConfig = listOf(
    releaseKeystoreFile,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).none { it.isNullOrBlank() }

android {
    namespace = "fr.mandarine.todolist"
    // Compiled against 37 because compose-ui 1.12.0 (Compose BOM 2026.08.00)
    // refuses to be consumed by anything older. targetSdk stays at 36 on purpose:
    // compiling against a newer SDK is not the same as opting in to its runtime
    // behaviour, and only the first of those is being asked for here.
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "fr.mandarine.todolist"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "2.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
    }

    sourceSets {
        getByName("test").assets.srcDir("$projectDir/schemas")
    }

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystoreFile!!)
                storePassword = releaseKeystorePassword!!
                keyAlias = releaseKeyAlias!!
                keyPassword = releaseKeyPassword!!
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        // The whole app dates its rows with java.time, which arrived in API 26.
        // minSdk is 24, so the library is backported into the dex rather than
        // taken from the platform.
        isCoreLibraryDesugaringEnabled = true
    }
    androidResources {
        // Without a generated locale config the French copy is unreachable from
        // the per-app language picker, and an icon-only app has no settings
        // screen to offer instead.
        generateLocaleConfig = true
    }
    lint {
        // NewApi is what caught java.time running under minSdk 24; it stays fatal
        // so the floor can never silently drop again.
        abortOnError = true
        checkReleaseBuilds = true
        fatal += "NewApi"
        warningsAsErrors = false
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.extensions.configure(JacocoTaskExtension::class.java) {
                    isIncludeNoLocationClasses = true
                    excludes = listOf("jdk.internal.*")
                }
            }
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Standalone configuration for running the Pitest CLI — avoids the AGP/JavaPlugin
// detection issue that the info.solidsoft.pitest Gradle plugin suffers from (it uses
// project.plugins.withType(JavaPlugin.class) which AGP 9.x doesn't trigger).
val pitestRuntime by configurations.creating {
    isTransitive = true
}

tasks.register<JavaExec>("pitest") {
    group = "verification"
    description = "Runs Pitest mutation testing against unit-test classes"

    notCompatibleWithConfigurationCache("pitest accesses project state at execution time")
    dependsOn("compileDebugKotlin", "compileDebugUnitTestKotlin", "testDebugUnitTest")

    // Run the Pitest CLI using its own fat-jar classpath.
    classpath = pitestRuntime

    mainClass.set("org.pitest.mutationtest.commandline.MutationCoverageReport")

    doFirst {
        val mainClasses = layout.buildDirectory
            .dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes").get().asFile
        val testClasses = layout.buildDirectory
            .dir("intermediates/built_in_kotlinc/debugUnitTest/compileDebugUnitTestKotlin/classes").get().asFile

        // Pitest needs an android.jar only to resolve the framework references it
        // meets while analysing bytecode, so any installed platform will do — but
        // naming one by hand meant the build depended on that exact platform being
        // installed. Take the newest one the SDK actually has.
        val platformsDir = File(
            System.getenv("ANDROID_HOME")
                ?: System.getenv("ANDROID_SDK_ROOT")
                ?: "${System.getProperty("user.home")}/Android/Sdk"
        ).resolve("platforms")
        val androidJar = platformsDir.listFiles().orEmpty()
            .filter { it.resolve("android.jar").isFile }
            .maxByOrNull { platform ->
                val version = platform.name.removePrefix("android-")
                val major = version.substringBefore('.').toIntOrNull() ?: -1
                val minor = version.substringAfter('.', "0").toIntOrNull() ?: 0
                major * 100 + minor
            }
            ?.resolve("android.jar")
            ?: error("No Android platform holding an android.jar under $platformsDir. Set ANDROID_HOME.")

        val rcp = configurations["debugUnitTestRuntimeClasspath"]
            .incoming
            .artifactView {
                attributes {
                    attribute(Attribute.of("artifactType", String::class.java), "android-classes-jar")
                }
                lenient(true)
            }
            .files
            .filter { it.extension == "jar" }

        val byteBuddyAgent = rcp.first { it.name.startsWith("byte-buddy-agent") }

        val classpathFile = layout.buildDirectory.file("pitest-classpath.txt").get().asFile
        classpathFile.parentFile.mkdirs()
        classpathFile.writeText(
            (listOf(mainClasses, testClasses, androidJar) + rcp)
                .joinToString("\n") { it.absolutePath }
        )

        args(
            "--reportDir", layout.buildDirectory.dir("reports/pitest").get().asFile.absolutePath,
            "--targetClasses", "fr.mandarine.todolist.domain.*,fr.mandarine.todolist.data.*,fr.mandarine.todolist.presentation.*",
            // *Test$* / *Tests$* also cover the synthetic classes Kotlin generates for
            // lambdas inside test methods (e.g. runTest { ... }), which plain *Test misses.
            "--excludedClasses", "*Test,*Tests,*Test\$*,*Tests\$*,*_Impl,*_Impl\$*," +
                "fr.mandarine.todolist.data.TodoDatabase,fr.mandarine.todolist.data.TodoDatabase\$*," +
                "fr.mandarine.todolist.data.AndroidListNotifier,fr.mandarine.todolist.data.AndroidListNotifier\$*," +
                "fr.mandarine.todolist.data.WorkManagerNotificationScheduler,fr.mandarine.todolist.data.WorkManagerNotificationScheduler\$*," +
                "fr.mandarine.todolist.data.TodoItemDao,fr.mandarine.todolist.data.TodoItemDao\$*," +
                "fr.mandarine.todolist.data.TodoListDao,fr.mandarine.todolist.data.TodoListDao\$*," +
                "fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository,fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository\$*",
            "--excludedTestClasses", "fr.mandarine.todolist.ui.*," +
                "fr.mandarine.todolist.AppContainerTest," +
                "fr.mandarine.todolist.DailyNotificationWorkTest," +
                "fr.mandarine.todolist.data.AndroidListNotifierTest," +
                "fr.mandarine.todolist.data.RoomTodoListRepositoryTargetDateTest," +
                "fr.mandarine.todolist.data.RoomTodoListRepositoryTest," +
                "fr.mandarine.todolist.data.RoomTodoRepositoryTest," +
                "fr.mandarine.todolist.data.TodoDatabaseTest," +
                "fr.mandarine.todolist.data.TodoDatabaseMigrationTest," +
                "fr.mandarine.todolist.data.TodoItemDaoPositionsTest," +
                "fr.mandarine.todolist.data.TodoListDaoIncrementTest," +
                "fr.mandarine.todolist.data.WorkManagerNotificationSchedulerTest," +
                "fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepositoryTest",
            "--targetTests", "fr.mandarine.todolist.*",
            "--mutators", "CONDITIONALS_BOUNDARY,INCREMENTS,INVERT_NEGS,MATH,NEGATE_CONDITIONALS," +
                "VOID_METHOD_CALLS,EMPTY_RETURNS,FALSE_RETURNS,TRUE_RETURNS,PRIMITIVE_RETURNS",
            "--avoidCallsTo", "java.util.logging,org.apache.log4j,org.slf4j,org.apache.commons.logging,kotlin.jvm.internal,kotlin.ResultKt,kotlin.collections.CollectionsKt",
            "--sourceDirs", "${projectDir}/src/main/java",
            "--classPathFile", classpathFile.absolutePath,
            "--jvmArgs", "-javaagent:${byteBuddyAgent.absolutePath}",
            "--outputFormats", "HTML,XML",
            "--mutationThreshold", "100",
            "--threads", "4",
            "--timestampedReports", "false",
            "--failWhenNoMutations", "false"
        )
    }
}

// The gate CLAUDE.md describes, made mechanical. `createDebugUnitTestCoverageReport`
// only writes a report; nothing failed when the numbers in it fell. This reads the
// report back and fails the build, so "100% line and branch" stops being something
// a person has to remember to look at.
//
// The exclusion list is deliberately the same shape as pitest's: Room's generated
// `_Impl` classes, the Kotlin `DefaultImpls` holders for interface bodies, and the
// handful of `data` classes whose behaviour is only reachable through Robolectric.
// Everything else in domain/, data/ and presentation/ must be fully covered.
val coverageGatePackages = listOf(
    "fr/mandarine/todolist/domain",
    "fr/mandarine/todolist/data",
    "fr/mandarine/todolist/presentation"
)

val coverageGateExcludedClasses = listOf(
    "*_Impl",
    "*_Impl\$*",
    "*\$DefaultImpls",
    "fr.mandarine.todolist.data.TodoDatabase",
    "fr.mandarine.todolist.data.TodoDatabase\$*",
    "fr.mandarine.todolist.data.AndroidListNotifier",
    "fr.mandarine.todolist.data.AndroidListNotifier\$*",
    "fr.mandarine.todolist.data.WorkManagerNotificationScheduler",
    "fr.mandarine.todolist.data.WorkManagerNotificationScheduler\$*",
    "fr.mandarine.todolist.data.TodoItemDao",
    "fr.mandarine.todolist.data.TodoItemDao\$*",
    "fr.mandarine.todolist.data.TodoListDao",
    "fr.mandarine.todolist.data.TodoListDao\$*",
    "fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository",
    "fr.mandarine.todolist.data.SharedPreferencesReminderTimeRepository\$*"
)

fun matchesGlob(name: String, pattern: String): Boolean {
    val regex = Regex(
        pattern.split("*").joinToString(".*") { Regex.escape(it) }
    )
    return regex.matches(name)
}

tasks.register("verifyGatedCoverage") {
    group = "verification"
    description = "Fails unless domain/, data/ and presentation/ are at 100% line and branch coverage"
    notCompatibleWithConfigurationCache("reads the coverage report at execution time")
    dependsOn("createDebugUnitTestCoverageReport")

    doLast {
        val report = layout.buildDirectory
            .file("reports/coverage/test/debug/report.xml").get().asFile
        if (!report.isFile) error("No coverage report at $report")

        val factory = DocumentBuilderFactory.newInstance()
        factory.isValidating = false
        val builder = factory.newDocumentBuilder()
        // The report carries a DOCTYPE pointing at report.dtd, which is not next to
        // it; resolving to nothing keeps the parse local and offline.
        builder.setEntityResolver { _, _ ->
            InputSource(StringReader(""))
        }
        val document = builder.parse(report)

        fun childrenNamed(node: Node, tag: String): List<Element> {
            val children = node.childNodes
            return (0 until children.length)
                .map { children.item(it) }
                .filterIsInstance<Element>()
                .filter { it.tagName == tag }
        }

        val shortfalls = mutableListOf<String>()
        val packages = document.documentElement.let { childrenNamed(it, "package") }
        val gated = packages.filter { it.getAttribute("name") in coverageGatePackages }
        if (gated.size != coverageGatePackages.size) {
            val seen = gated.map { it.getAttribute("name") }
            error("Coverage report is missing gated packages: ${coverageGatePackages - seen.toSet()}")
        }

        gated.forEach { pkg ->
            childrenNamed(pkg, "class").forEach { klass ->
                val binaryName = klass.getAttribute("name").replace('/', '.')
                val excluded = coverageGateExcludedClasses.any { matchesGlob(binaryName, it) }
                if (!excluded) {
                    childrenNamed(klass, "counter")
                        .filter { it.getAttribute("type") in setOf("LINE", "BRANCH") }
                        .forEach { counter ->
                            val missed = counter.getAttribute("missed").toInt()
                            if (missed > 0) {
                                shortfalls += "$binaryName: ${counter.getAttribute("type")} missed=$missed"
                            }
                        }
                }
            }
        }

        if (shortfalls.isNotEmpty()) {
            error(
                "Coverage gate failed — 100% line and branch is required in " +
                    "domain/, data/ and presentation/:\n" + shortfalls.joinToString("\n") { "  $it" }
            )
        }
    }
}

// The baseline profile is generated on a device and committed, so nothing in an
// ordinary build ever looks at it again. It went stale exactly that way: it still
// named the tutorial classes months after they were deleted. This fails the build
// when a rule names a class the app no longer ships.
tasks.register("verifyBaselineProfile") {
    group = "verification"
    description = "Fails when the committed baseline profile names classes the app no longer has"
    notCompatibleWithConfigurationCache("reads compiled classes at execution time")
    dependsOn("compileDebugKotlin")

    val profile = layout.projectDirectory
        .file("src/release/generated/baselineProfiles/baseline-prof.txt").asFile

    doLast {
        if (!profile.isFile) return@doLast
        val classesDir = layout.buildDirectory
            .dir("intermediates/built_in_kotlinc/debug/compileDebugKotlin/classes").get().asFile
        // Compare outer classes only. A profile rule can name a lambda D8 synthesised
        // (`Foo$$ExternalSyntheticLambda0`) or a resource class AAPT generated, and
        // neither has a .class file here; what staleness looks like is the *outer*
        // type having been deleted, which is what the tutorial rules are.
        fun outerOf(name: String) = name.substringBefore('$')

        val shipped = classesDir.walkTopDown()
            .filter { it.extension == "class" }
            .map { outerOf(it.relativeTo(classesDir).path.removeSuffix(".class")) }
            .toSet()

        val generated = setOf("R", "BuildConfig")
        val ours = Regex("^\\[?[HSPL]*L(fr/mandarine/todolist/[^;]*);")
        val stale = profile.readLines()
            .mapNotNull { line -> ours.find(line)?.groupValues?.get(1) }
            .map { outerOf(it) }
            .filter { it.substringAfterLast('/') !in generated }
            .filter { it !in shipped }
            .distinct()
            .sorted()

        if (stale.isNotEmpty()) {
            error(
                "Baseline profile names ${stale.size} class(es) the app no longer ships. " +
                    "Regenerate it with `./gradlew :app:generateBaselineProfile` on a " +
                    "connected device, or drop the dead rules:\n" +
                    stale.take(10).joinToString("\n") { "  $it" } +
                    if (stale.size > 10) "\n  … and ${stale.size - 10} more" else ""
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    testImplementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.androidx.compose.ui.test.junit4)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    // Not imported by any test. It is declared to pull espresso up to the version
    // in the catalogue: compose ui-test-junit4 drags in 3.5.0 transitively, and
    // dropping this line silently downgrades it.
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    pitestRuntime(libs.pitest.commandline)
}
