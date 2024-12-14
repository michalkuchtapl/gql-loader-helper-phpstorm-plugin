package dev.mikify.gqlqueryloader;

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.php.lang.psi.elements.FunctionReference
import com.jetbrains.php.lang.psi.elements.StringLiteralExpression

class LoadQueryGotoDeclarationHandler : GotoDeclarationHandler {

    override fun getGotoDeclarationTargets(
        element: PsiElement?,
        offset: Int,
        editor: Editor
    ): Array<PsiElement>? {
        // Ensure element exists and is part of a function reference
        val functionReference = PsiTreeUtil.getParentOfType(element, FunctionReference::class.java) ?: return null

        // Verify the function being called is `loadQuery`
        if (functionReference.name != "loadQuery") return null

        // Retrieve the argument passed to `loadQuery`
        val argument = functionReference.parameters.firstOrNull() as? StringLiteralExpression ?: return null
        val stringArgument = argument.contents

        // Resolve the file path from the argument
        val queryPath = resolveQueryPath(stringArgument, editor.project ?: return null) ?: return null

        // Open the file and return the target element
        return arrayOf(queryPath)
    }

    private fun resolveQueryPath(argument: String, project: Project): PsiElement? {
        val basePath = project.basePath ?: return null
        val relativePath = "resources/queries/${argument.replace('.', '/')}"

        // Check for .graphql file first
        val graphqlFile = LocalFileSystem.getInstance().findFileByPath("$basePath/$relativePath.graphql")
        if (graphqlFile != null) {
            return PsiManager.getInstance(project).findFile(graphqlFile)
        }

        // Check for .php file
        val phpFile = LocalFileSystem.getInstance().findFileByPath("$basePath/$relativePath.php")
        if (phpFile != null) {
            return PsiManager.getInstance(project).findFile(phpFile)
        }

        return null
    }

    override fun getActionText(context: DataContext): String? = null
}
