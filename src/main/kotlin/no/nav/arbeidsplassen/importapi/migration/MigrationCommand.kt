package no.nav.arbeidsplassen.importapi.migration

import io.micronaut.context.annotation.Value
import org.slf4j.LoggerFactory
import picocli.CommandLine
import java.time.LocalDateTime
import javax.inject.Inject


@CommandLine.Command(name = "migrate", description = arrayOf("migrate from feed to postgresql"),
        mixinStandardHelpOptions = true)
class MigrationCommand: Runnable {

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
    @Value("\${MIGRATION_URL:http://localhost/stillingsimport/internal}") lateinit var migrationUrl: String

    companion object {
        private val LOG = LoggerFactory.getLogger(MigrationCommand::class.java)
    }

    override fun run() {
        val providerUrl = migrationUrl + "/providers"
        val adstateUrl = migrationUrl + "/adstates"
        val transferUrl = migrationUrl +"/transfers"
        LOG.info("migrating providers")
        val feedtask = feedtaskRepository.findByName("PROVIDER_MIGRATION_TASK").orElseGet{
            feedtaskRepository.save(Feedtask(name="PROVIDER_MIGRATION_TASK", lastrun = LocalDateTime.now().minusYears(1)))}
        val providerList = feedConnector.fetchContentList(providerUrl, feedtask.lastrun, Provider::class.java)
        providerList.forEach {
            LOG.info("migrating provider {}", it.id)
        }
        LOG.info("migrating transferlogs")
        LOG.info("migrating adstates")
    }
}
