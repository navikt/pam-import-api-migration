package no.nav.arbeidsplassen.importapi.migration

import io.micronaut.configuration.picocli.PicocliRunner
import io.micronaut.context.annotation.Value
import io.micronaut.http.client.RxHttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.runtime.Micronaut
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.time.LocalDateTime
import javax.inject.Inject


@Command(name = "migration-app", description = arrayOf("..."),
        mixinStandardHelpOptions = true)
class Application: Runnable {

    @Inject
    lateinit var adStateRepository: AdStateRepository
    @Inject
    lateinit var providerRepository: ProviderRepository
    @Inject
    lateinit var transferLogRepository: TransferLogRepository
    @Inject
    lateinit var feedtaskRepository: FeedtaskRepository
    @Inject
    lateinit var feedConnector: FeedConnector
    @Value("\${MIGRATION_URL") lateinit var migrationUrl: String

    override fun run() {
        val providerUrl = migrationUrl + "/providers/entities"
        val adstateUrl = migrationUrl + "/adstates/entities"
        val transferUrl = migrationUrl +"/transfers/entities"
        LOG.info("migrating providers")
        var feedtask = feedtaskRepository.findByName("PROVIDER_MIGRATION_TASK").orElseGet{
            feedtaskRepository.save(Feedtask(name="PROVIDER_MIGRATION_TASK", lastrun = LocalDateTime.now().minusYears(1)))}
        var providerList = feedConnector.fetchContentList(providerUrl, feedtask.lastrun, Provider::class.java)
        var lastUpdated = feedtask.lastrun

        while (providerList.isNotEmpty() && lastUpdated.isBefore(providerList.last().updated)) {
            LOG.info("migrating {} providers ", providerList.size)
            providerRepository.saveAll(providerList)
            lastUpdated = providerList.last().updated
            LOG.info("saving provider last updated {}", lastUpdated)
            feedtaskRepository.update(feedtask.copy(lastrun = lastUpdated))
            providerList = feedConnector.fetchContentList(providerUrl, lastUpdated, Provider::class.java)
        }

        LOG.info("migrating transferlogs")
        feedtask = feedtaskRepository.findByName("TRANSFERLOG_MIGRATION_TASK").orElseGet{
            feedtaskRepository.save(Feedtask(name="TRANSFERLOG_MIGRATION_TASK", lastrun = LocalDateTime.now().minusYears(1)))}
        var transferLogList = feedConnector.fetchContentList(transferUrl, feedtask.lastrun, TransferLog::class.java)
        lastUpdated = feedtask.lastrun

        while (transferLogList.isNotEmpty() && lastUpdated.isBefore(transferLogList.last().updated)) {
            LOG.info("migrating {} transferlogs", transferLogList.size)
            transferLogRepository.saveAll(transferLogList)
            lastUpdated = transferLogList.last().updated
            LOG.info("saving transfer last updated {}", lastUpdated)
            feedtaskRepository.update(feedtask.copy( lastrun = lastUpdated))
            transferLogList = feedConnector.fetchContentList(transferUrl, lastUpdated, TransferLog::class.java)
        }

        LOG.info("migrating adstates")
        feedtask = feedtaskRepository.findByName("ADSTATE_MIGRATION_TASK").orElseGet{
            feedtaskRepository.save(Feedtask(name="ADSTATE_MIGRATION_TASK", lastrun = LocalDateTime.now().minusYears(1)))}
        var adstates = feedConnector.fetchContentList(adstateUrl, feedtask.lastrun, AdState::class.java)
        lastUpdated = feedtask.lastrun

        while (adstates.isNotEmpty() && lastUpdated.isBefore(adstates.last().updated)) {
            LOG.info("migrating {} adstates", adstates.size)
            adStateRepository.saveAll(adstates)
            lastUpdated = adstates.last().updated
            LOG.info("saving adstates last updated {}", lastUpdated)
            feedtaskRepository.update(feedtask.copy(lastrun = lastUpdated))
            adstates = feedConnector.fetchContentList(adstateUrl, lastUpdated, AdState::class.java)
        }
        LOG.info("finished!")
    }

    companion object {
        private val LOG: Logger = LoggerFactory.getLogger(Application::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            PicocliRunner.execute(Application::class.java, *args)
        }
    }

}
