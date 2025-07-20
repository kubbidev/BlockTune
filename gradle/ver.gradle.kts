import java.io.ByteArrayOutputStream
import org.gradle.kotlin.dsl.support.serviceOf

val execOps: ExecOperations = serviceOf()

fun git(vararg args: String): String {
    val out = ByteArrayOutputStream()
    execOps.exec {
        commandLine("git", *args)
        standardOutput = out
        errorOutput = ByteArrayOutputStream()
        isIgnoreExitValue = true
    }
    return out.toString().trim()
}

val computedVersion: String = run {
    val lastTagRaw = git("describe", "--tags", "--abbrev=0")
    val tagRegex = Regex("^v?(\\d+)\\.(\\d+)\\.(\\d+)$")
    val lastTag = tagRegex.matchEntire(lastTagRaw)
        ?.groupValues
        ?.let { (_, maj, min, pat) -> "$maj.$min.$pat" }
        ?: "0.0.0"

    val commitsSince = if (lastTagRaw.isBlank()) {
        git("rev-list", "--count", "HEAD")
    } else {
        git("rev-list", "$lastTagRaw..HEAD", "--count")
    }.ifBlank { "0" }

    val hash = git("rev-parse", "--short", "HEAD").ifBlank { "unknown" }
    if (commitsSince == "0") lastTag else "$lastTag+$commitsSince.$hash"
}

// This actually *sets* the project.version
project.version = computedVersion