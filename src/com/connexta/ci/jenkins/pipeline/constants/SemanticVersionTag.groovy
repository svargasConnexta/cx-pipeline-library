package com.connexta.ci.jenkins.pipeline.constants

import com.cloudbees.groovy.cps.NonCPS

class SemanticVersionTag implements Comparable<SemanticVersionTag>, Serializable {
    private static String XY_SEMVER = /^(\d+)\.(\d+)$/
    private static String XYZ_SEMVER = /^(\d+)\.(\d+)\.(\d+)$/
    private static String XYZW_SEMVER = /^(\d+)\.(\d+)\.(\d+)\.(\d+)$/
    @SuppressWarnings("GrFinalVariableAccess")
    final Integer x, y, z, w

    SemanticVersionTag(Integer x, Integer y) {
        this(x, y, null, null)
    }

    SemanticVersionTag(Integer x, Integer y, Integer z) {
        this(x, y, z, null)
    }

    SemanticVersionTag(Integer x, Integer y, Integer z, Integer w) {
        if (x == null) {
            throw new IllegalArgumentException("X is required.")
        }

        if (y == null) {
            throw new IllegalArgumentException("Y is required.")
        }

        if (x < 0 || y < 0 || (z != null && z < 0) || (w != null && w < 0)) {
            throw new IllegalArgumentException("X or Y or Z or W was < 0. Only positive integers allowed.")
        }

        this.x = x
        this.y = y
        this.z = z
        this.w = w
    }

    @NonCPS
    static SemanticVersionTag fromString(final String tag) throws IllegalArgumentException {
        Integer x, y, z, w

        def xyzwMatch = tag =~ XYZW_SEMVER
        if (xyzwMatch.matches()) {
            x = xyzwMatch.group(1).toInteger()
            y = xyzwMatch.group(2).toInteger()
            z = xyzwMatch.group(3).toInteger()
            w = xyzwMatch.group(4).toInteger()
            return new SemanticVersionTag(x, y, z, w)
        }

        def xyzMatch = tag =~ XYZ_SEMVER
        if (xyzMatch.matches()) {
            x = xyzMatch.group(1).toInteger()
            y = xyzMatch.group(2).toInteger()
            z = xyzMatch.group(3).toInteger()
            return new SemanticVersionTag(x, y, z, null)
        }

        def xyMatch = tag =~ XY_SEMVER
        if (xyMatch.matches()) {
            x = xyMatch.group(1).toInteger()
            y = xyMatch.group(2).toInteger()
            return new SemanticVersionTag(x, y, null, null)
        }

        throw new IllegalArgumentException("Invalid Semantic Tag: " + tag)
    }

    SemanticVersionTag incrementLeastSignificantDigit() {
        if (w != null) {
            return new SemanticVersionTag(x, y, z, Math.addExact(w, 1))
        }

        if (z != null) {
            return new SemanticVersionTag(x, y, Math.addExact(z, 1), w)
        }

        return new SemanticVersionTag(x, Math.addExact(y, 1), z, w)
    }

    SemanticVersionTag normalizeToXYZWTag() {
        if (w != null) {
            return this
        }

        if (z != null) {
            return new SemanticVersionTag(x, y, z, 0)
        }

        return new SemanticVersionTag(x, y, 0, 0)
    }

    SemanticVersionTag floorToXYTag() {
        if (z == null && w == null) {
            return this
        }
        return new SemanticVersionTag(x, y, null, null)
    }

    @SuppressWarnings("unused")
    boolean equals(final SemanticVersionTag other) {
        return compareTo(other) == 0
    }

    int compareTo(final SemanticVersionTag other) {
        return x <=> other.x ?:
               y <=> other.y ?:
               (z == null ? 0 : z) <=> (other.z == null ? 0 : other.z) ?:
               (w == null ? 0 : w) <=> (other.w == null ? 0 : other.w) ?:
               0
    }

    String toString() {
        final StringBuilder result = new StringBuilder("${x}.${y}")

        if (z != null) {
            result.append(".${z}")
        }

        if (w != null) {
            result.append(".${w}")
        }

        return result.toString()
    }
}
