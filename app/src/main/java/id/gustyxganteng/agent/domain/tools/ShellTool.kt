package id.gustyxganteng.agent.domain.tools

import id.gustyxganteng.agent.core.model.FunctionSpec
import id.gustyxganteng.agent.core.model.PropertySchema
import id.gustyxganteng.agent.core.model.ToolDefinition
import id.gustyxganteng.agent.core.model.ToolExecutionResult
import id.gustyxganteng.agent.core.model.ToolParametersSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class ShellTool {

    companion object {
        const val MAX_OUTPUT_CHARS = 10000

        val DEFINITION = ToolDefinition(
            function = FunctionSpec(
                name = "execute_shell",
                description = "Menjalankan perintah shell/terminal di direktori workspace aktif dan mengembalikan stdout, stderr, serta exit code.",
                parameters = ToolParametersSchema(
                    properties = mapOf(
                        "command" to PropertySchema("string", "Perintah bash/sh yang akan dijalankan."),
                        "working_dir" to PropertySchema("string", "Sub-direktori opsional di dalam workspace."),
                        "timeout_ms" to PropertySchema("integer", "Batas waktu eksekusi dalam milidetik (default 30000).")
                    ),
                    required = listOf("command")
                )
            )
        )
    }

    suspend fun execute(
        toolCallId: String,
        workspaceRoot: String,
        args: JsonObject,
        useTermuxEnv: Boolean = false
    ): ToolExecutionResult = withContext(Dispatchers.IO) {
        val command = args["command"]?.jsonPrimitive?.content
            ?: return@withContext ToolExecutionResult(toolCallId, "execute_shell", stderr = "Perintah kosong.", isSuccess = false)
        val subDir = args["working_dir"]?.jsonPrimitive?.content
        val timeoutMs = args["timeout_ms"]?.jsonPrimitive?.longOrNull ?: 120000L

        val normalizedRoot = WorkspacePathResolver.normalizePath(workspaceRoot)
        val workingDirFile = if (!subDir.isNullOrEmpty()) {
            File(normalizedRoot, subDir).canonicalFile
        } else {
            File(normalizedRoot).canonicalFile
        }

        workingDirFile.mkdirs()

        // Tentukan shell binary
        val termuxBash = File("/data/data/com.termux/files/usr/bin/bash")
        val shellExecutable = if (useTermuxEnv && termuxBash.exists() && termuxBash.canExecute()) {
            termuxBash.absolutePath
        } else {
            "/system/bin/sh"
        }

        val processBuilder = ProcessBuilder(shellExecutable, "-c", command)
        processBuilder.directory(workingDirFile)

        // Environment variables
        val env = processBuilder.environment()
        if (useTermuxEnv) {
            env["PREFIX"] = "/data/data/com.termux/files/usr"
            val currentPath = System.getenv("PATH") ?: ""
            env["PATH"] = "/data/data/com.termux/files/usr/bin:/data/data/com.termux/files/usr/bin/applets:$currentPath"
            env["HOME"] = "/data/data/com.termux/files/home"
        }

        try {
            val process = processBuilder.start()

            val stdoutBuilder = StringBuilder()
            val stderrBuilder = StringBuilder()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdoutThread = Thread {
                try {
                    var line: String? = stdoutReader.readLine()
                    while (line != null) {
                        stdoutBuilder.appendLine(line)
                        line = stdoutReader.readLine()
                    }
                } catch (_: Exception) {}
            }
            val stderrThread = Thread {
                try {
                    var line: String? = stderrReader.readLine()
                    while (line != null) {
                        stderrBuilder.appendLine(line)
                        line = stderrReader.readLine()
                    }
                } catch (_: Exception) {}
            }

            stdoutThread.start()
            stderrThread.start()

            val finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                return@withContext ToolExecutionResult(
                    toolCallId = toolCallId,
                    toolName = "execute_shell",
                    stderr = "Perintah timeout setelah ${timeoutMs}ms.",
                    exitCode = 124,
                    isSuccess = false
                )
            }

            stdoutThread.join(1000)
            stderrThread.join(1000)

            val exitCode = process.exitValue()
            val stdout = truncateOutput(stdoutBuilder.toString())
            val stderr = truncateOutput(stderrBuilder.toString())

            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "execute_shell",
                stdout = stdout,
                stderr = stderr,
                exitCode = exitCode,
                isSuccess = exitCode == 0
            )
        } catch (e: Exception) {
            ToolExecutionResult(
                toolCallId = toolCallId,
                toolName = "execute_shell",
                stderr = e.message ?: "Gagal mengeksekusi shell.",
                exitCode = 1,
                isSuccess = false
            )
        }
    }

    private fun truncateOutput(output: String): String {
        return if (output.length > MAX_OUTPUT_CHARS) {
            val truncated = output.takeLast(MAX_OUTPUT_CHARS)
            "... [Output dipotong, menampilkan $MAX_OUTPUT_CHARS karakter terakhir] ...\n$truncated"
        } else {
            output
        }
    }
}
