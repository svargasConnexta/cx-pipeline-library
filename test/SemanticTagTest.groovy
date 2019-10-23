import com.connexta.ci.jenkins.pipeline.constants.SemanticVersionTag
import support.BasePipelineSpecification

class SemanticVersionTagSpecification extends BasePipelineSpecification {
    def static final MIN = Integer.MIN_VALUE
    def static final MAX = Integer.MAX_VALUE

    def "Assertion thrown if constructor invoked with NULL major minor or any negative arguments"() {
        when:
        new SemanticVersionTag(x, y, z, w)

        then:
        thrown(IllegalArgumentException)

        where:
        x                 | y    | z  | w
        null              | 0    | 0  | 0
        0                 | null | 0  | 0
        null              | null | 0  | 0
        -1                | 0    | 0  | 0
        Integer.MIN_VALUE | -2   | 0  | 0
        0                 | -2   | -3 | 0
        0                 | 0    | 0  | -4
        -1                | -9   | -9 | -5
    }

    def "Empty Optional<SemanticVersionTag> returned if SemanticVersionTag::fromString invoked with invalid input"() {
        when:
        SemanticVersionTag.fromString(tag)

        then:
        thrown(IllegalArgumentException)

        where:
        tag                           | _
        "0"                           | _
        "0.0.0.0.0"                   | _
        "1995"                        | _
        "1.áéíóú.2.3.4"               | _
        "1.²"                         | _
        "1,2,3,4"                     | _
        "1|2|3|4"                     | _
        "1.2.3-SNAPSHOT"              | _
        "${MIN}.${MIN}"               | _
        "${MIN}.${MIN}.${MIN}"        | _
        "${MIN}.${MIN}.${MIN}.${MIN}" | _
    }

    def "SemanticTagVersion::fromString supports tags in the range (0, ...) to (INT_MAX, ...)"() {
        when:
        def t = SemanticVersionTag.fromString(tag)

        then:
        noExceptionThrown()
        t == result

        where:
        tag                          || result
        "${MAX}.${MAX}"               | new SemanticVersionTag(MAX, MAX)
        "${MAX}.${MAX}.${MAX}"        | new SemanticVersionTag(MAX, MAX, MAX)
        "${MAX}.${MAX}.${MAX}.${MAX}" | new SemanticVersionTag(MAX, MAX, MAX, MAX)
        "0.0"                         | new SemanticVersionTag(0, 0)
        "0.0.0"                       | new SemanticVersionTag(0, 0, 0)
        "0.0.0.0"                     | new SemanticVersionTag(0, 0, 0, 0)
    }

    def "SemanticTagVersion::fromString returns empty Optional instead of exception on negative numbers"() {

    }

    def "SemanticVersionTag increments successfully if in correct range"() {
        when:
        def t = new SemanticVersionTag(x, y, z, w).incrementLeastSignificantDigit()

        then:
        t == result

        where:
        x | y | z    | w   || result
        1 | 0 | null | null | new SemanticVersionTag(1, 1)
        1 | 1 | 0    | null | new SemanticVersionTag(1, 1, 1)
        1 | 1 | 1    | 0    | new SemanticVersionTag(1, 1, 1, 1)
        1 | 1 | 1    | 1    | new SemanticVersionTag(1, 1, 1, 2)
    }

    def "Arithmetic Exception triggered if tag would overflow"() {
        when:
        //noinspection GroovyAssignabilityCheck
        new SemanticVersionTag(x, y, z, w).incrementLeastSignificantDigit()

        then:
        thrown(ArithmeticException)

        where:
        x   | y   | z    | w
        MAX | MAX | MAX  | MAX
        MAX | MAX | MAX  | null
        MAX | MAX | null | null
    }

    def "Tag normalization adds expected zeros"() {
        when:
        final def t = new SemanticVersionTag(x, y, z, w)
                .normalizeToXYZWTag()
                .toString()

        then:
        noExceptionThrown()
        result == t

        where:
        x | y | z    | w    || result
        1 | 9 | 3    | 4    || "1.9.3.4"
        1 | 9 | null | null || "1.9.0.0"
        0 | 0 | null | null || "0.0.0.0"
        6 | 5 | 1    | null || "6.5.1.0"

    }

    def "Tag flooring truncates z and w build numbers"() {
        when:
        final def t = new SemanticVersionTag(x, y, z, w)
                .floorToXYTag()
                .toString()

        then:
        noExceptionThrown()
        result == t

        where:
        x | y | z    | w    || result
        1 | 2 | null | null || "1.2"
        1 | 2 | 3    | null || "1.2"
        1 | 2 | 3    | 4    || "1.2"
        0 | 0 | 3    | 4    || "0.0"
        0 | 0 | 0    | 0    || "0.0"
        1 | 0 | 0    | 1    || "1.0"
        1 | 0 | 999  | 999  || "1.0"
    }
}