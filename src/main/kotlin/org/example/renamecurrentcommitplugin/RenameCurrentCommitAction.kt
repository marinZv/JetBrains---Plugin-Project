package org.example.renamecurrentcommitplugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import com.intellij.openapi.application.ApplicationManager

class RenameCurrentCommitAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        ApplicationManager.getApplication().executeOnPooledThread {
            val gitRepository = getGitRepository(project) ?: run {
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(project, "No git repository found.", "Error")
                }
                return@executeOnPooledThread
            }

            ApplicationManager.getApplication().invokeLater {
                val newCommitMessage = Messages.showInputDialog(
                        project,
                        "Enter the new commit message:",
                        "Rename Current Commit",
                        Messages.getQuestionIcon()
                )

                if (newCommitMessage.isNullOrBlank()) {
                    Messages.showErrorDialog(project, "Commit message cannot be empty", "Error")
                    return@invokeLater
                }

                renameCommit(gitRepository, newCommitMessage, project)
            }
        }
    }

    private fun getGitRepository(project: Project): GitRepository? {
        val gitRepositoryManager = GitRepositoryManager.getInstance(project)
        return gitRepositoryManager.getRepositoryForFile(project.baseDir)
    }

    private fun renameCommit(gitRepository: GitRepository, newCommitMessage: String, project: Project) {
        ApplicationManager.getApplication().executeOnPooledThread {
            val gitLineHandler = GitLineHandler(gitRepository.project, gitRepository.root, GitCommand.COMMIT)
            gitLineHandler.addParameters("--amend", "--no-edit", "-m", newCommitMessage)

            val result = Git.getInstance().runCommand(gitLineHandler)

            ApplicationManager.getApplication().invokeLater {
                if (result.success()) {
                    Messages.showInfoMessage(project, "Commit successfully renamed", "Success")
                } else {
                    Messages.showErrorDialog(project, "Failed to rename commit.", "Error")
                }
            }
        }
    }

}

