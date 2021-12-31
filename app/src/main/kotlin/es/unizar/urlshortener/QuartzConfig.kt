package es.unizar.urlshortener

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.quartz.*

import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.context.ApplicationContext

import org.springframework.scheduling.quartz.SpringBeanJobFactory
import org.springframework.core.io.ClassPathResource

import org.springframework.scheduling.quartz.SchedulerFactoryBean

import org.quartz.JobDetail
import org.quartz.SimpleTrigger

import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean

import org.springframework.scheduling.quartz.JobDetailFactoryBean

@Configuration
@EnableScheduling
public QuartzScheduler(
    @Autowired val applicationContext: ApplicationContext
){

    @Bean
    fun springBeanJobFactory(): SpringBeanJobFactory {
        val jobFactory = AutoWiringSpringBeanJobFactory()
        jobFactory.setApplicationContext(applicationContext)
        return jobFactory
    }

    /**
     * Schedule a job with all of its components and using
     * a configuration file to establish some of the behaviour
     * of the scheduler
     */
    @Bean
    fun scheduler(trigger: Trigger, job: JobDetail): SchedulerFactoryBean {
        val schedulerFactory = SchedulerFactoryBean()
        schedulerFactory.setConfigLocation(
            ClassPathResource("quartz.properties")
        )
        schedulerFactory.setJobFactory(springBeanJobFactory())
        schedulerFactory.setJobDetails(job)
        schedulerFactory.setTriggers(trigger)
        return schedulerFactory
    }

    /**
     * Establish when to execute a job
     */
    @Bean
    fun trigger(job: JobDetail): SimpleTriggerFactoryBean {
        val trigger = SimpleTriggerFactoryBean()
        trigger.setJobDetail(job)
        val frequencyInSec = 1000
        trigger.setRepeatInterval((frequencyInSec * 60).toLong())
        trigger.setRepeatCount(SimpleTrigger.REPEAT_INDEFINITELY)
        trigger.setName("GeneralStats_Trigger")
        return trigger
    }
}