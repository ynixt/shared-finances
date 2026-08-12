package com.ynixt.sharedfinances.application.config

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules
import com.ynixt.sharedfinances.application.web.jobs.ExpireInvitesJob
import com.ynixt.sharedfinances.application.web.jobs.ExportJobsMaintenanceJob
import com.ynixt.sharedfinances.application.web.jobs.ExportPurgeJobs
import com.ynixt.sharedfinances.application.web.jobs.GenerateEntryRecurrenceJob
import com.ynixt.sharedfinances.application.web.jobs.ImportJobsMaintenanceJob
import com.ynixt.sharedfinances.application.web.jobs.InactiveAccountDeletionJob
import com.ynixt.sharedfinances.application.web.jobs.MaterializeGoalContributionScheduleJob
import com.ynixt.sharedfinances.application.web.jobs.SimulationJobsMaintenanceJob
import com.ynixt.sharedfinances.application.web.jobs.SyncExchangeRatesJob
import com.ynixt.sharedfinances.application.web.jobs.UnconfirmedAccountCleanupJob
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.DependsOn
import java.time.Duration
import javax.sql.DataSource

/**
 * Configures db-scheduler for distributed, cluster-safe job execution.
 *
 * How it works:
 * - A dedicated JDBC DataSource (HikariCP) is created using the same connection
 *   properties as Flyway. This is necessary because the main application uses R2DBC,
 *   while db-scheduler requires a blocking JDBC DataSource.
 * - Each recurring task is registered with a unique name and a cron schedule.
 * - db-scheduler persists execution state in the `scheduled_tasks` table and uses
 *   optimistic locking (`version` column) + `SELECT FOR UPDATE SKIP LOCKED` to
 *   guarantee that only ONE node in the cluster picks up each execution.
 * - The scheduler polls the database every 10 seconds for due tasks.
 * - Heartbeats are sent every 5 minutes so dead executions can be detected and
 *   rescheduled by another node.
 */
@Configuration
@ConditionalOnProperty(name = ["db-scheduler.enabled"], havingValue = "true", matchIfMissing = true)
class DbSchedulerConfig(
    @param:Value("\${spring.flyway.url}") private val jdbcUrl: String,
    @param:Value("\${spring.flyway.user}") private val jdbcUser: String,
    @param:Value("\${spring.flyway.password}") private val jdbcPassword: String,
    @param:Value("\${app.jobs.expireInvites.cron}") private val expireInvitesCron: String,
    @param:Value("\${app.jobs.generateEntry.cron}") private val generateEntryCron: String,
    @param:Value("\${app.jobs.materializeGoal.cron}") private val materializeGoalCron: String,
    @param:Value("\${app.jobs.refreshExchangeRates.cron}") private val syncExchangeRatesCron: String,
    @param:Value("\${app.jobs.unconfirmedAccountCleanup.cron}") private val unconfirmedCleanupCron: String,
    private val inactiveAccountDeletionProperties: InactiveAccountDeletionProperties,
    @param:Value("\${app.jobs.simulation.reconcile.cron-enabled:false}") private val simulationReconcileEnabled: Boolean,
    @param:Value("\${app.jobs.simulation.reconcile.cron}") private val simulationReconcileCron: String,
    @param:Value("\${app.jobs.simulation.purge.cron}") private val simulationPurgeCron: String,
    @param:Value("\${app.jobs.imports.reconcile.cron-enabled:true}") private val importReconcileEnabled: Boolean,
    @param:Value("\${app.jobs.imports.reconcile.cron:0 */1 * * * *}") private val importReconcileCron: String,
    @param:Value("\${app.jobs.exports.reconcile.cron-enabled:true}") private val exportReconcileEnabled: Boolean,
    @param:Value("\${app.jobs.exports.reconcile.cron:0 */1 * * * *}") private val exportReconcileCron: String,
    private val exportRetentionProperties: ExportRetentionProperties,
) {
    private val logger = LoggerFactory.getLogger(DbSchedulerConfig::class.java)

    /**
     * Dedicated JDBC connection pool for db-scheduler.
     * Small pool (max 5) — the scheduler only needs connections for polling and heartbeats.
     */
    @Bean(destroyMethod = "close")
    fun schedulerDataSource(): HikariDataSource {
        val config =
            HikariConfig().apply {
                jdbcUrl = this@DbSchedulerConfig.jdbcUrl
                username = this@DbSchedulerConfig.jdbcUser
                password = this@DbSchedulerConfig.jdbcPassword
                maximumPoolSize = 5
                minimumIdle = 1
                poolName = "db-scheduler-pool"
                connectionTimeout = 5_000
                idleTimeout = 60_000
            }

        return HikariDataSource(config)
    }

    @Bean
    @DependsOn("flyway")
    fun dbSchedulerStarter(scheduler: Scheduler) =
        ApplicationRunner {
            scheduler.start()
        }

    /**
     * The db-scheduler Scheduler instance.
     *
     * - `threads(4)`: up to 4 tasks can execute in parallel on this node.
     * - `pollingInterval(10s)`: how often the scheduler checks the DB for due tasks.
     * - `heartbeatInterval(5min)`: heartbeat frequency for detecting dead executions.
     * - `deleteUnresolvedAfter(14d)`: auto-clean tasks removed from code after 14 days.
     */
    @Bean(destroyMethod = "stop")
    fun dbScheduler(
        @Qualifier("schedulerDataSource") schedulerDataSource: DataSource,
        expireInvitesJob: ExpireInvitesJob,
        generateEntryRecurrenceJob: GenerateEntryRecurrenceJob,
        materializeGoalJob: MaterializeGoalContributionScheduleJob,
        syncExchangeRatesJob: SyncExchangeRatesJob,
        unconfirmedAccountCleanupJob: UnconfirmedAccountCleanupJob,
        inactiveAccountDeletionJob: InactiveAccountDeletionJob,
        simulationJobsMaintenanceJob: SimulationJobsMaintenanceJob,
        importJobsMaintenanceJob: ImportJobsMaintenanceJob,
        exportJobsMaintenanceJob: ExportJobsMaintenanceJob,
        exportPurgeJobs: ExportPurgeJobs,
    ): Scheduler {
        val tasks = mutableListOf<RecurringTask<*>>()

        tasks +=
            Tasks
                .recurring("expire-invites", Schedules.cron(expireInvitesCron))
                .execute { _, _ -> runBlocking { expireInvitesJob.execute() } }

        tasks +=
            Tasks
                .recurring("generate-entry-recurrence", Schedules.cron(generateEntryCron))
                .execute { _, _ -> runBlocking { generateEntryRecurrenceJob.execute() } }

        tasks +=
            Tasks
                .recurring("materialize-goal-contribution", Schedules.cron(materializeGoalCron))
                .execute { _, _ -> runBlocking { materializeGoalJob.execute() } }

        tasks +=
            Tasks
                .recurring("sync-exchange-rates", Schedules.cron(syncExchangeRatesCron))
                .execute { _, _ -> runBlocking { syncExchangeRatesJob.execute() } }

        tasks +=
            Tasks
                .recurring("unconfirmed-account-cleanup", Schedules.cron(unconfirmedCleanupCron))
                .execute { _, _ -> runBlocking { unconfirmedAccountCleanupJob.execute() } }

        tasks +=
            Tasks
                .recurring("inactive-account-deletion", Schedules.cron(inactiveAccountDeletionProperties.cron))
                .execute { _, _ -> runBlocking { inactiveAccountDeletionJob.execute() } }

        tasks +=
            Tasks
                .recurring("simulation-purge", Schedules.cron(simulationPurgeCron))
                .execute { _, _ -> runBlocking { simulationJobsMaintenanceJob.executePurge() } }

        if (simulationReconcileEnabled) {
            tasks +=
                Tasks
                    .recurring("simulation-reconcile", Schedules.cron(simulationReconcileCron))
                    .execute { _, _ -> runBlocking { simulationJobsMaintenanceJob.executeReconcile() } }
            logger.info("Simulation reconcile task registered with cron: {}", simulationReconcileCron)
        } else {
            logger.info("Simulation reconcile task is disabled (app.jobs.simulation.reconcile.cron-enabled=false)")
        }

        if (importReconcileEnabled) {
            tasks +=
                Tasks
                    .recurring("import-reconcile", Schedules.cron(importReconcileCron))
                    .execute { _, _ -> runBlocking { importJobsMaintenanceJob.executeReconcile() } }
            logger.info("Import reconcile task registered with cron: {}", importReconcileCron)
        } else {
            logger.info("Import reconcile task is disabled (app.jobs.imports.reconcile.cron-enabled=false)")
        }

        if (exportReconcileEnabled) {
            tasks +=
                Tasks
                    .recurring("export-reconcile", Schedules.cron(exportReconcileCron))
                    .execute { _, _ -> runBlocking { exportJobsMaintenanceJob.executeReconcile() } }
            logger.info("Export reconcile task registered with cron: {}", exportReconcileCron)
        } else {
            logger.info("Export reconcile task is disabled (app.jobs.exports.reconcile.cron-enabled=false)")
        }

        if (exportRetentionProperties.afterDownload.enabled) {
            tasks +=
                Tasks
                    .recurring("export-purge-after-download", Schedules.cron(exportRetentionProperties.afterDownload.cron))
                    .execute { _, _ -> runBlocking { exportPurgeJobs.purgeAfterDownload() } }
        } else {
            logger.info("Export purge after download is disabled")
        }

        if (exportRetentionProperties.absoluteAge.enabled) {
            tasks +=
                Tasks
                    .recurring("export-purge-absolute-age", Schedules.cron(exportRetentionProperties.absoluteAge.cron))
                    .execute { _, _ -> runBlocking { exportPurgeJobs.purgeByAbsoluteAge() } }
        } else {
            logger.info("Export purge by absolute age is disabled")
        }

        return Scheduler
            .create(schedulerDataSource)
            .startTasks(tasks)
            .registerShutdownHook()
            .threads(4)
            .pollingInterval(Duration.ofMinutes(1))
            .heartbeatInterval(Duration.ofMinutes(5))
            .deleteUnresolvedAfter(Duration.ofDays(14))
            .build()
    }
}
