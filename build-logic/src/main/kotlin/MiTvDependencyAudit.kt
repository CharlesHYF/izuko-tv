import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.UnresolvedDependencyResult
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory

private const val MITV_SDK = 23
private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
private const val MITV_ABI_PREFIX = "jni/armeabi-v7a/"

/**
 * 检查目标运行依赖的最低系统版本与 ARM32 原生库
 * 创建日期：2026-10-22
 * 修改日期：2026-10-22
 */
abstract class MiTvDependencyAudit : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val runtimeFiles: ConfigurableFileCollection

    @get:Input
    abstract val dependencyFailures: ListProperty<String>

    @TaskAction
    fun audit() {
        check(dependencyFailures.get().isEmpty()) { "Unresolved dependencies: ${dependencyFailures.get()}" }
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        var inspected = 0
        for (file in runtimeFiles.files) {
            if (file.extension != "aar") continue
            ZipFile(file).use { archive ->
                val manifest = archive.getEntry("AndroidManifest.xml")
                if (manifest != null) {
                    archive.getInputStream(manifest).use { input ->
                        val document = factory.newDocumentBuilder().parse(input)
                        val nodes = document.getElementsByTagName("uses-sdk")
                        if (nodes.length > 0) {
                            val sdk = nodes.item(0).attributes.getNamedItemNS(ANDROID_NAMESPACE, "minSdkVersion")?.nodeValue
                            check(sdk == null || (sdk.toIntOrNull() ?: Int.MAX_VALUE) <= MITV_SDK) {
                                "${file.name}: minSdk=$sdk exceeds MiTV API $MITV_SDK"
                            }
                        }
                    }
                }
                var hasNative = false
                var hasArm32 = false
                val entries = archive.entries()
                while (entries.hasMoreElements()) {
                    val name = entries.nextElement().name
                    if (!name.startsWith("jni/") || !name.endsWith(".so")) continue
                    hasNative = true
                    if (name.startsWith(MITV_ABI_PREFIX)) hasArm32 = true
                }
                check(!hasNative || hasArm32) { "${file.name}: missing armeabi-v7a native libraries" }
            }
            inspected++
        }
        check(inspected > 0) { "No AAR artifacts were inspected" }
        logger.lifecycle("MiTV dependency audit passed: $inspected AARs, API $MITV_SDK, armeabi-v7a")
    }
}

/** 注册兼容包运行依赖审计任务。 */
fun Project.registerMiTvDependencyAudit() {
    val enabled = buildMiTvApp
    val runtime = configurations.named("defaultTvReleaseRuntimeClasspath")
    tasks.register<MiTvDependencyAudit>("auditMiTvDependencies") {
        group = "verification"
        description = "Checks the MiTV release dependency SDK and native ABI requirements"
        runtimeFiles.from(runtime.map { configuration ->
            configuration.incoming.artifactView {
                componentFilter { component -> component is ModuleComponentIdentifier }
                attributes.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "aar")
                isLenient = true
            }.files
        })
        dependencyFailures.set(runtime.map { configuration ->
            val failures = mutableListOf<String>()
            for (dependency in configuration.incoming.resolutionResult.allDependencies) {
                if (dependency is UnresolvedDependencyResult) failures.add(dependency.attempted.displayName)
            }
            failures
        })
        onlyIf { enabled }
    }
}
