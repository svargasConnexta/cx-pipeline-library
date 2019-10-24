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
        assertIsValidGitRepo(pathToLocalGitRepo)
    }

    @NonCPS
    static void assertIsValidGitRepo(final String pathToLocalGitRepo)  {
        final def gitRepo = new Git(new FileRepositoryBuilder<>().setWorkTree(new File(pathToLocalGitRepo)).build())
        final def atLeastOneValidRef = gitRepo.getRepository().getRefDatabase().getRefs().any { it != null }
        final def isBareDirectory = gitRepo.getRepository().isBare()
        final def isInvalidGitRepo = !isBareDirectory && !atLeastOneValidRef
        if (isInvalidGitRepo) {
            throw new IllegalStateException("${pathToLocalGitRepo} is NOT a valid git repo")
        }
        gitRepo.close()
    }

    /**
     * TODO: Tags are NOT returned in any guaranteed order because calling .sort() or Collections.sort()
     *       breaks Groovy the Jenkins Unittest Pipeline for some reason
     */
    @NonCPS
    List<SemanticVersionTag> getSemanticTagsInRepo() {
        assertIsValidGitRepo(pathToLocalGitRepo)
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
