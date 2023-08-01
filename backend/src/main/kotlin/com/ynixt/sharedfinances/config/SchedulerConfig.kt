package com.ynixt.sharedfinances.config

import com.ynixt.sharedfinances.jobs.ExpireGroupInviteJob
import org.quartz.JobBuilder
import org.quartz.JobDetail
import org.quartz.spi.TriggerFiredBundle
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.boot.autoconfigure.quartz.QuartzProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.CronTriggerFactoryBean
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import org.springframework.scheduling.quartz.SpringBeanJobFactory
import java.time.ZoneId
import java.util.*
import javax.sql.DataSource


@Configuration
class SchedulerConfig(
    @Value("\${sf-app.job-timezone}") private val timeZone: String,
    @Value("\${sf-app.expires-invite-cron}") private val expiresGroupInviteCron: String,
    private val applicationContext: ApplicationContext,
    private val quartzProperties: QuartzProperties,
    private val dataSource: DataSource
) {
    @Bean
    fun schedulerFactoryBean(): SchedulerFactoryBean {
        val jobFactory = AutoWiringSpringBeanJobFactory()
        jobFactory.setApplicationContext(applicationContext)

        val schedulerFactory = SchedulerFactoryBean()
        schedulerFactory.setQuartzProperties(getProperties(quartzProperties))
        schedulerFactory.setJobFactory(jobFactory)
        schedulerFactory.setTriggers(
            triggerExpireGroupInviteJob(expireGroupInviteJobDetail()).`object`
        )
        schedulerFactory.setDataSource(dataSource)


        return schedulerFactory
    }

    @Bean("expireGroupInvite")
    fun expireGroupInviteJobDetail(): JobDetail {
        return JobBuilder.newJob().ofType(ExpireGroupInviteJob::class.java)
            .storeDurably()
            .withIdentity("expire_group_invite")
            .withDescription("Expire all group invites that need to be expired")
            .build()
    }

    @Bean
    fun triggerExpireGroupInviteJob(@Qualifier("expireGroupInvite") job: JobDetail): CronTriggerFactoryBean {
        val trigger = CronTriggerFactoryBean()

        trigger.setJobDetail(job)
        trigger.setCronExpression(expiresGroupInviteCron)
        trigger.setTimeZone(TimeZone.getTimeZone(ZoneId.of(timeZone)))

        return trigger
    }

    private fun getProperties(quartzProperties: QuartzProperties): Properties {
        val properties = Properties()

        properties.putAll(quartzProperties.properties)

        return properties
    }
}

class AutoWiringSpringBeanJobFactory : SpringBeanJobFactory(), ApplicationContextAware {
    @Transient
    private var beanFactory: AutowireCapableBeanFactory? = null

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        beanFactory = applicationContext.autowireCapableBeanFactory
    }

    override fun createJobInstance(bundle: TriggerFiredBundle): Any {
        val job = super.createJobInstance(bundle)
        beanFactory!!.autowireBean(job)
        return job
    }
}
