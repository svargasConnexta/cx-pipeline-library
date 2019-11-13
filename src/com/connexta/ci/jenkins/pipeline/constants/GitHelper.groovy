package com.connexta.ci.jenkins.pipeline.constants

import com.cloudbees.groovy.cps.NonCPS
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class GitHelper {
    final String authorName
    final String authorEmail
    final String pathToLocalGitRepo

    GitHelper(String authorName, String authorEmail, String pathToLocalGitRepo) {
        this.authorName = authorName
        this.authorEmail = authorEmail
        this.pathToLocalGitRepo = pathToLocalGitRepo
        if (!isValidGitRepo(pathToLocalGitRepo)) {
            throw new IllegalStateException("Path: ${pathToLocalGitRepo} is NOT a valid git repo!")
        }
    }

    @NonCPS
    static boolean isValidGitRepo(final String pathToLocalGitRepo)  {
        final def gitRepo = new Git(new FileRepositoryBuilder<>().setWorkTree(new File(pathToLocalGitRepo)).build())
        final def atLeastOneValidRef = gitRepo.getRepository().getRefDatabase().getRefs().any { it != null }
        final def isBareDirectory = gitRepo.getRepository().isBare()
        final def isValidGitRepo = isBareDirectory || atLeastOneValidRef
        try {
            return isValidGitRepo
        } finally {
            gitRepo.close()
        }
    }

    @NonCPS
    List<SemanticVersionTag> getSemanticTagsInRepo() {
        if (!isValidGitRepo(pathToLocalGitRepo)) {
            return []
        }

        final List<SemanticVersionTag> result = new ArrayList()
        final def gitRepo = new Git(new FileRepositoryBuilder<>().setWorkTree(new File(pathToLocalGitRepo)).build())
        try {
            for (final Ref ref : gitRepo.tagList().call()) {
                final String name = ref.getName().replace("refs/tags/", "")
                SemanticVersionTag maybeTag = null
                try {
                    maybeTag = SemanticVersionTag.fromString(name)
                } catch (IllegalArgumentException ignored) {
                }
                if (maybeTag != null) {
                    result.add(maybeTag)
                }
            }
        } finally {
            gitRepo.close()
        }

        return result
    }
}
