package dev.mikify.gqlqueryloader

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ProcessingContext

class LoadQueryCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet
    ) {
        val project: Project = parameters.editor.project ?: return

        // Locate the `resources/queries` directory
        val baseDir: VirtualFile = project.baseDir ?: return
        val queriesDir: VirtualFile = baseDir.findFileByRelativePath("resources/queries") ?: return

        val text = resultSet.prefixMatcher.prefix
            .trim('"', '\'', '(', ')')
            .replace('.', '/')

        // Determine the current directory based on the typed prefix
        val (currentDir, prefix) = resolveCurrentDirectoryAndPrefix(queriesDir, text)

        // Filter files/directories by the prefix
        currentDir.children
            ?.filter { it.isValid && canShowElement(it) && matchesPrefix(it, prefix) }
            ?.forEach { child ->
                var displayName = if (child.isDirectory)
                    child.name
                else
                    child.nameWithoutExtension

                var dir = child
                    .path
                    .replace(queriesDir.path, "")
                    .trim('/')
                    .split('/')
                    .dropLast(1)
                    .joinToString("/")

                if (dir.isNotEmpty()) {
                    displayName =  "$dir/$displayName"
                }

                resultSet.addElement(LookupElementBuilder.create(displayName.replace('/', '.')))
            }
    }

    private fun canShowElement(element: VirtualFile): Boolean {
        return element.isDirectory || element.extension == "graphql" || element.extension == "php"
    }

    /**
     * Resolve the current directory and the prefix based on the typed input.
     */
    private fun resolveCurrentDirectoryAndPrefix(
        rootDir: VirtualFile,
        input: String
    ): Pair<VirtualFile, String> {
        val parts = input.split('/')
        val prefix = parts.lastOrNull() ?: ""
        var currentDir = rootDir

        // Navigate through the directories in the input
        for (part in parts.dropLast(1)) {
            currentDir = currentDir.findChild(part) ?: break
            if (!currentDir.isDirectory) break
        }

        return Pair(currentDir, prefix)
    }

    /**
     * Check if a file or directory matches the prefix.
     */
    private fun matchesPrefix(file: VirtualFile, prefix: String): Boolean {
        if (prefix.isEmpty()) return true

        return if (file.isDirectory) {
            file.name.startsWith(prefix, ignoreCase = true)
        } else {
            file.nameWithoutExtension.startsWith(prefix, ignoreCase = true)
        }
    }
}