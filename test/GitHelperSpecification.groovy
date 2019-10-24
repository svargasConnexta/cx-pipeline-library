import com.connexta.ci.jenkins.pipeline.constants.GitHelper
import support.BasePipelineSpecification

import org.eclipse.jgit.api.Git

import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

class GitHelperSpecification extends BasePipelineSpecification {
    static Path TEMP_GIT_DIRECTORY
    static Path TEMP_NON_GIT_DIRECTORY
    static List<String> VALID_SORTED_TAGS = ["1.0", "2.0", "4.1.1", "5.2.3.4", "5.2.5"]
    static List<String> INVALID_TAGS = ["CAKE", "3.0-SNAPSHOT", "1995"]
    static List<String> ALL_TAGS = new ArrayList<>();

    def setupSpec() {
        TEMP_GIT_DIRECTORY = Files.createTempDirectory("git_helper_test")
        TEMP_NON_GIT_DIRECTORY = Files.createTempDirectory("not_git")


        ALL_TAGS.addAll(VALID_SORTED_TAGS)
        ALL_TAGS.addAll(INVALID_TAGS)
        Collections.shuffle(ALL_TAGS, new Random(0x12345))

        def git = Git.init()
                .setDirectory(TEMP_GIT_DIRECTORY.toFile())
                .call()

        git.commit()
                .setAuthor("Spock Unittest", "<>")
                .setCommitter("Spock Unittest", "<>")
                .setSign(false)
                .setAllowEmpty(true)
                .setMessage("Empty commit for unittesting")
                .call()

        ALL_TAGS.each {
            git.tag().setName(it).call()
        }
    }

    def "withGitHelper(...) passes GitHelper object to provided closure"() {
        setup:
        def script = loadScript("vars/withGitHelper.groovy")

        when:
        GitHelper capturedHelper = null
        script.call(name: "Jenkins", email: "<>", path: TEMP_GIT_DIRECTORY.toAbsolutePath().toString()) {
            GitHelper gitHelper ->
                capturedHelper = gitHelper
        }

        then:
        capturedHelper instanceof GitHelper
    }

    def "GitHelper.getSortedSemanticTags() only returns the valid semantic tags in a git repo"() {
        setup:
        def script = loadScript("vars/withGitHelper.groovy")
        List<String> actualTagsAsStrings = new ArrayList<>();

        when:
        GitHelper capturedHelper = null
        script.call(name: "Jenkins", email: "<>", path: TEMP_GIT_DIRECTORY.toAbsolutePath().toString()) {
            GitHelper gitHelper ->
                actualTagsAsStrings.addAll(gitHelper
                        .semanticTagsInRepo
                        .stream()
                        .map { tag -> tag.toString() }
                        .collect(Collectors.toList())
                )
        }

        then:
        actualTagsAsStrings.containsAll(VALID_SORTED_TAGS)
        Collections.disjoint(actualTagsAsStrings, INVALID_TAGS)
    }

    def "GitHelper.getSortedSemanticTags() throws exception if instantiated with non git directory"() {
        setup:
        def script = loadScript("vars/withGitHelper.groovy")

        when:
        script.call(name: "Jenkins", email: "<>", path: TEMP_NON_GIT_DIRECTORY.toAbsolutePath().toString()) {
            GitHelper gitHelper -> gitHelper.semanticTagsInRepo
        }

        then:
        thrown(IllegalStateException)
    }

    def cleanupSpec() {
        final def destroyEverything = new DestroyEverythingFileVisitor()
        Files.walkFileTree(TEMP_GIT_DIRECTORY, destroyEverything)
        Files.walkFileTree(TEMP_NON_GIT_DIRECTORY, destroyEverything)
    }

    static class DestroyEverythingFileVisitor extends SimpleFileVisitor<Path> {
        @Override
        FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            try {
                Files.delete(file)
            } catch (IOException ignored) {
                file.toFile().deleteOnExit()
            }
            return FileVisitResult.CONTINUE
        }

        @Override
        FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
            try {
                Files.delete(dir)
            } catch (IOException ignored) {
                dir.toFile().deleteOnExit()
            }
            return FileVisitResult.CONTINUE
        }
    }
}
