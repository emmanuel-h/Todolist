plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.ksp)
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
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "fr.mandarine.todolist"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
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

        val androidJar = File(System.getenv("ANDROID_HOME") ?: "${System.getProperty("user.home")}/Android/Sdk")
            .resolve("platforms/android-36.1/android.jar")

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
            "--excludedClasses", "*Test,*Tests,*_Impl,*_Impl\$*," +
                "fr.mandarine.todolist.data.TodoDatabase,fr.mandarine.todolist.data.TodoDatabase\$*," +
                "fr.mandarine.todolist.data.AndroidListNotifier,fr.mandarine.todolist.data.AndroidListNotifier\$*," +
                "fr.mandarine.todolist.data.WorkManagerNotificationScheduler,fr.mandarine.todolist.data.WorkManagerNotificationScheduler\$*," +
                "fr.mandarine.todolist.data.TodoItemDao,fr.mandarine.todolist.data.TodoItemDao\$*," +
                "fr.mandarine.todolist.data.TodoListDao,fr.mandarine.todolist.data.TodoListDao\$*",
            "--excludedTestClasses", "fr.mandarine.todolist.ui.*," +
                "fr.mandarine.todolist.AppContainerTest," +
                "fr.mandarine.todolist.DailyNotificationWorkTest," +
                "fr.mandarine.todolist.data.AndroidListNotifierTest," +
                "fr.mandarine.todolist.data.RoomTodoListRepositoryTargetDateTest," +
                "fr.mandarine.todolist.data.RoomTodoListRepositoryTest," +
                "fr.mandarine.todolist.data.RoomTodoRepositoryTest," +
                "fr.mandarine.todolist.data.TodoDatabaseTest," +
                "fr.mandarine.todolist.data.TodoItemDaoPositionsTest," +
                "fr.mandarine.todolist.data.TodoListDaoIncrementTest," +
                "fr.mandarine.todolist.data.WorkManagerNotificationSchedulerTest",
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

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    implementation(libs.androidx.recyclerview)
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
    testImplementation(libs.androidx.espresso.core)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    pitestRuntime(libs.pitest.commandline)
}
