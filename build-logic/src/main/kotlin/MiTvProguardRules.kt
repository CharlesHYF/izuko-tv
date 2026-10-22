/*
 * 为小米电视生成保留名称且允许代码优化的 R8 规则。
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

@CacheableTask
abstract class PrepareMiTvProguardRules : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val sourceRules: RegularFileProperty

    @get:OutputFile
    abstract val optimizedRules: RegularFileProperty

    @TaskAction
    fun prepare() {
        val rules = StringBuilder()
        for (line in sourceRules.get().asFile.readLines()) {
            val isGlobalNameRule = line.startsWith("-keepnames class me.him188.ani.**") ||
                line.startsWith("-keepnames class !com.google.common.**")
            rules.appendLine(
                if (isGlobalNameRule) line.replaceFirst("-keepnames class", "-keepnames,allowoptimization class") else line,
            )
        }
        val output = optimizedRules.get().asFile
        output.parentFile.mkdirs()
        output.writeText(rules.toString())
    }
}

fun Project.miTvAndroidProguardRules(): Array<Any> {
    val sharedRules = sharedAndroidProguardRules()
    val prepared = tasks.register<PrepareMiTvProguardRules>("prepareMiTvProguardRules") {
        sourceRules.set(project(":app:shared").layout.projectDirectory.file("proguard-rules.pro"))
        optimizedRules.set(layout.buildDirectory.file("generated/mitv/proguard-rules.pro"))
    }
    val result = mutableListOf<Any>()
    for (rule in sharedRules) {
        result.add(if (rule.name == "proguard-rules.pro") prepared.flatMap { it.optimizedRules } else rule)
    }
    return result.toTypedArray()
}
