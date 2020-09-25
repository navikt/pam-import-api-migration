package no.nav.arbeidsplassen.importapi.migration

import io.micronaut.configuration.picocli.PicocliRunner
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import javax.inject.Inject


@Command(name = "migration-app", description = arrayOf("..."),
        mixinStandardHelpOptions = true, subcommands = arrayOf(MigrationCommand::class))
object Application: Runnable {

    override fun run() {
    }

    @Throws(Exception::class)
    @JvmStatic
    fun main(args: Array<String>)  {
        PicocliRunner.run(Application::class.java, *args)
    }

}
