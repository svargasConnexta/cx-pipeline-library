package com.connexta.ci.jenkins.pipeline.constants

import java.nio.file.Path
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Ref
import org.eclipse.jgit.storage.file.FileRepositoryBuilder

class GitHelper implements AutoCloseable {
    private final String authorName
    private final String authorEmail
    private final Git gitRepo

    GitHelper(String authorName, String authorEmail, Path gitRepoPath) {
        this.authorName = authorName
        this.authorEmail = authorEmail
        this.gitRepo = new Git(new FileRepositoryBuilder<>()
                .setWorkTree(gitRepoPath.toAbsolutePath().toFile())
                .build())

        def atLeastOneValidRef = this.gitRepo.getRepository().getRefDatabase().getRefs().any { it != null }
        def isBareDirectory = this.gitRepo.getRepository().isBare()
        if (!atLeastOneValidRef && !isBareDirectory) {
            throw new IllegalStateException("${gitRepoPath.toAbsolutePath().toString()} is NOT a valid git repo")
        }
    }


    List<SemanticVersionTag> getSortedSemanticTags() {
        final List<SemanticVersionTag> result = new ArrayList()
        for (final Ref ref : gitRepo.tagList().call())  {
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

        Collections.sort(result)
        return Collections.unmodifiableList(result)
    }

    @Override
    void close() {
        gitRepo.close()
    }
}
