import org.gradle.api.Project
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

fun Project.getCommitCount(): String {
    return try {
        val res = runCommand("git rev-list --count HEAD")
        if (res.isNotBlank()) res else "1"
    } catch (e: Exception) {
        "1"
    }
}

fun Project.getGitSha(): String {
    return try {
        val res = runCommand("git rev-parse --short HEAD")
        if (res.isNotBlank()) res else "brennan"
    } catch (e: Exception) {
        "brennan"
    }
}

fun Project.getBuildTime(): String {
    val df = SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'")
    df.timeZone = TimeZone.getTimeZone("UTC")
    return df.format(Date())
}

fun Project.runCommand(command: String): String {
    return try {
        val byteOut = ByteArrayOutputStream()
        project.exec {
            commandLine = command.split(" ")
            standardOutput = byteOut
            isIgnoreExitValue = true
        }
        String(byteOut.toByteArray()).trim()
    } catch (e: Exception) {
        ""
    }
}
