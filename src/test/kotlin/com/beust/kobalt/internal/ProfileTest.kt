package com.beust.kobalt.internal

import com.beust.kobalt.Args
import com.beust.kobalt.BaseTest
import com.beust.kobalt.TestModule
import com.beust.kobalt.api.Kobalt
import com.beust.kobalt.api.Project
import com.beust.kobalt.app.BuildFileCompiler
import com.google.inject.Inject
import org.assertj.core.api.Assertions.assertThat
import org.testng.annotations.DataProvider
import org.testng.annotations.Guice
import org.testng.annotations.Test
import java.util.*

@Guice(modules = arrayOf(TestModule::class))
class ProfileTest @Inject constructor(compilerFactory: BuildFileCompiler.IFactory) : BaseTest(compilerFactory) {

    private fun runTestWithProfile(enabled: Boolean) : Project {
        val projectVal = "p" + Math.abs(Random().nextInt())

        fun buildFileString(): String {
            return """
                    import com.beust.kobalt.*
                    import com.beust.kobalt.api.*
                    val profile = false
                    val $projectVal = project {
                        name = if (profile) "profileOn" else "profileOff"
                    }
                """
        }

        val args = Args()
        if (enabled) args.profiles = "profile"
        val projectName = if (enabled) "profileOn" else "profileOff"
        val compileResult = compileBuildFile(buildFileString(), args)
        try {
            return compileResult.projects.first { it.name == projectName }
        } catch(ex: Exception) {
            println("PROBLEM")
            throw ex
        }
    }

    @DataProvider
    fun dp() = arrayOf(
            arrayOf(false, "profileOff"),
            arrayOf(true, "profileOn"))

    @Test(dataProvider = "dp")
    fun profilesShouldWork(enabled: Boolean, expected: String) {
        Kobalt.init(TestModule())
        assertThat(runTestWithProfile(enabled).name).isEqualTo(expected)
    }
}


