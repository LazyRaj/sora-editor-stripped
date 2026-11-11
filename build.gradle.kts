/*******************************************************************************
 *    sora-editor - the awesome code editor for Android
 *    https://github.com/Rosemoe/sora-editor
 *    Copyright (C) 2020-2024  Rosemoe
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 2.1 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 *
 *     Please contact Rosemoe by email 2073412493@qq.com if you need
 *     additional information or have any questions
 ******************************************************************************/

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidExtension

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
}

object Versions {
    const val buildToolsVersion = "37.0.0"
    const val compileSdkVersion = 37
    const val minSdkVersion = 30
    const val targetSdkVersion = 37
    const val versionCode = 96
    const val appVersionName = "0.24.7"
    const val artifactVersion = "0.24.7"
}

fun Project.configureAndroidAndKotlin() {
    extensions.findByType<CommonExtension>()?.apply {
        compileSdk { version = release(Versions.compileSdkVersion) }
        buildToolsVersion = Versions.buildToolsVersion

        defaultConfig.apply {
            minSdk = Versions.minSdkVersion
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
        }
    }

    extensions.findByType<ApplicationExtension>()?.apply {
        defaultConfig {
            targetSdk = Versions.targetSdkVersion
            versionCode = Versions.versionCode
            versionName = Versions.appVersionName
        }
    }

    extensions.findByType<KotlinAndroidExtension>()?.apply {
        compilerOptions {
            languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_4
            jvmTarget = JvmTarget.JVM_25
        }
    }
}

subprojects {
    group = "io.github.rosemoe"
    version = Versions.artifactVersion

    plugins.withId("com.android.application") {
        configureAndroidAndKotlin()
    }
    plugins.withId("com.android.library") {
        configureAndroidAndKotlin()
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.layout.buildDirectory)
}
