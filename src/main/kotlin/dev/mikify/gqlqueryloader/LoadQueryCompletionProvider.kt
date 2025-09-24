package dev.mikify.gqlqueryloader

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.InsertionContext
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findOrCreateDirectory
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.php.lang.psi.elements.FunctionReference


class LoadQueryCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        resultSet: CompletionResultSet
    ) {
        val project: Project = parameters.editor.project ?: return

        val position = parameters.position

        if (!isInsideLoadQueryCall(position)) {
            return; // Don't add any completions if not in loadQuery
        }

        // Locate the `resources/queries` directory
        val baseDir: VirtualFile = project.baseDir ?: return
        val queriesDir: VirtualFile = baseDir.findFileByRelativePath("resources/queries") ?: return

        val text = resultSet.prefixMatcher.prefix
            .trim('"', '\'', '(', ')')
            .replace('.', '/')

        // Determine the current directory based on the typed prefix
        val (currentDir, prefix) = resolveCurrentDirectoryAndPrefix(queriesDir, text)

        val files = currentDir.children
            ?.filter { it.isValid && canShowElement(it) && matchesPrefix(it, prefix) }

        if (files == null || files.isEmpty()) {
            addCreateOptions(resultSet, text)
        }

        files
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

    private fun addCreateOptions(result: CompletionResultSet, baseName: String) {
        // Create .graphql option
        val graphqlElement = LookupElementBuilder
            .create(baseName.replace('/', '.'))
            .withIcon(MyIcons.GRAPHQL) // or your custom icon
            .withInsertHandler { context, item ->
                createFileAndInsertPath(context, "$baseName.graphql", "graphql")
            }
            .withTypeText("Create Graphql query")

        // Create .php option
        val phpElement = LookupElementBuilder
            .create(baseName.replace('/', '.'))
            .withIcon(AllIcons.Language.Php) // or your custom icon
            .withInsertHandler { context, item ->
                createFileAndInsertPath(context, "$baseName.php", "php")
            }
            .withTypeText("Create PHP query")

        result.addElement(graphqlElement)
        result.addElement(phpElement)
    }

    private fun createFileAndInsertPath(
        context: InsertionContext,
        fileName: String,
        fileType: String
    ) {
        ApplicationManager.getApplication().invokeLater {
            val project = context.project
            val baseDir = project.baseDir // or determine appropriate directory

            // Create the file
            WriteCommandAction.runWriteCommandAction(project) {
                var targetDir = baseDir.findFileByRelativePath("resources/queries")
                if (fileName.contains('/')) {
                    targetDir = baseDir.findOrCreateDirectory("resources/queries/${fileName.substringBeforeLast('/')}")
                }

                if (targetDir != null) {
                    val newFile = targetDir.createChildData(this, fileName.substringAfterLast('/'))

                    // Add basic content based on file type
                    when (fileType) {
                        "graphql" -> newFile.setBinaryContent(("query {\n" +
                                "    \n" +
                                "}\n" +
                                "\n").toByteArray())
                        "php" -> newFile.setBinaryContent(("<?php\n\n" +
                                "return function (?array \$data = null) {\n" +
                                "\n" +
                                "    return <<<GQL\n" +
                                "        \n" +
                                "    GQL;\n" +
                                "};\n" +
                                "\n").toByteArray())
                    }

                    // Open the file in editor
                    FileEditorManager.getInstance(project).openFile(newFile, true)
                }
            }
        }
    }

    private fun isInsideLoadQueryCall(element: PsiElement): Boolean {
        var element: PsiElement? = element
        while (element != null) {
            // If element is a function call
            if (element is FunctionReference) {
                // Check if the method name is loadQuery
                val methodName = element.name
                if ("loadQuery" == methodName) {
                    return true
                }
            }
            element = element.parent
        }
        return false
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