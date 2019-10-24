#!/usr/bin/env groovy
import com.cloudbees.groovy.cps.NonCPS
import com.connexta.ci.jenkins.pipeline.constants.GitHelper
import java.nio.file.Paths

@NonCPS
def call(final Map config, final Closure body) {
    final String username = config.getOrDefault("name", "Jenkins")
    final String email = config.getOrDefault("email", "<>")
    final String currentWorkingDirectory = new File(".").toPath().toAbsolutePath().toString()
    final String path = Paths.get(config.getOrDefault("path", currentWorkingDirectory)).toAbsolutePath().toString()
    final GitHelper helper = new GitHelper(username, email, path)
    body.call(helper)
}

