package com.beust.kobalt.maven

import com.beust.kobalt.misc.Version

open class SimpleDep(open val mavenId: MavenId) : UnversionedDep(mavenId.groupId, mavenId.artifactId) {
    companion object {
        fun create(id: String) = MavenId.create(id).let {
            SimpleDep(it)
        }
    }

    override fun toString() = mavenId.toId

    val version: String get() =
        if (mavenId.version == null) {
            ""
//            throw AssertionError("Version should have been resolved: ${mavenId.toId}")
        } else {
            mavenId.version!!
        }

    private fun toFile(version: Version, suffix: String): String {
        val dir = toDirectory(version.version, false, trailingSlash = false)
        val list =
            if (version.snapshotTimestamp != null) {
                listOf(dir, artifactId + "-" + version.noSnapshotVersion + "-" + version.snapshotTimestamp + suffix)
            } else {
                listOf(dir, artifactId + "-" + version.version + suffix)
            }
        return list.joinToString("/").replace("//", "/")
    }

    fun toPomFile(v: String) = toFile(Version.of(v), ".pom")

    fun toJarFile(v: String = version) = toFile(Version.of(v), suffix)
    fun toAarFile(v: String = version) = toFile(Version.of(v), ".aar")

    fun toPomFileName() = "$artifactId-$version.pom"

    val suffix : String
        get() {
            val packaging = mavenId.packaging
            return if (packaging != null && ! packaging.isNullOrBlank()) ".$packaging" else ".jar"
        }
}
