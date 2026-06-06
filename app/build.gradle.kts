plugins {
    alias(libs.plugins.android.application)
}

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
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
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

        val rcp = configurations["debugUnitTestRuntimeClasspath"].files
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
            "--excludedClasses", "*Test,*Tests",
            "--targetTests", "fr.mandarine.todolist.*",
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
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    pitestRuntime(libs.pitest.commandline)
}
