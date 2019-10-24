#!/usr/bin/env groovy
import com.connexta.ci.jenkins.pipeline.constants.GitHelper

import java.nio.file.Path
import java.nio.file.Paths

def call(final Map config, final Closure body) {
    final String username = config.getOrDefault("name", "Jenkins")
    final String email = config.getOrDefault("email", "<>")
    final Path path = Paths.get(config.getOrDefault("path", new File(".").getAbsolutePath().toString()))
    final GitHelper helper = new GitHelper(username, email, path)
    body.call(helper)
    helper.close()
}

