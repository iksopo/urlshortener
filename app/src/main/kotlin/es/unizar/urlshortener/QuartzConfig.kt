package es.unizar.urlshortener

import es.unizar.urlshortener.core.ShortUrlRepositoryService
import org.quartz.*
import org.quartz.spi.TriggerFiredBundle
import org.springframework.beans.BeansException
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.AutowireCapableBeanFactory
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.scheduling.annotation.EnableScheduling

import javax.sql.DataSource

import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.scheduling.quartz.*
import org.springframework.stereotype.Component



/**
 * Adds auto-wiring support to quartz scheduler jobs.
 * Reference: "https://gist.github.com/jelies/5085593"
 */
class AutoWiringSpringBeanJobFactory : SpringBeanJobFactory(), ApplicationContextAware {
    @Transient
    private var beanFactory: AutowireCapableBeanFactory? = null

    @Throws(BeansException::class)
    override fun setApplicationContext(applicationContext: ApplicationContext) {
        beanFactory = applicationContext.autowireCapableBeanFactory
    }

    @Throws(Exception::class)
    override fun createJobInstance(bundle: TriggerFiredBundle): Any {
        val job = super.createJobInstance(bundle)
        beanFactory!!.autowireBean(job)
        return job
    }
}

/**
 * Job used by the scheduler that updates the general stats
 * in the cache by recalculating them
 */
@Component
class ExpiredsDeleter: Job {

    /**
     * Needs to be nullable but gets autowired correctly
     */
    @Autowired
    private val repo: ShortUrlRepositoryService? = null

    /**
     * Method to execute to delete expireds urls
     */
    override fun execute(context: JobExecutionContext) {
        repo?.deleteExpireds().let{
            println("Deleting all expired URLs: {expiration=" + it!!.first + ", leftUses=" + it!!.second + "}")
        }
    }
}

/**
 * Enables scheduling through Quartz, a robust scheduler.
 * Defines a job, trigger and schedules the job to be launched every X seconds.
 * Supports persistance (configured at quartz.properties)
 */
@Configuration
@EnableScheduling
class QuartzConfig(
    @Autowired val applicationContext: ApplicationContext
){

    /**
     * Get the periodic cleaner job and defines some meta-data
     */
    @Bean
    fun jobDetail(): JobDetailFactoryBean {
        val jobDetailFactory = JobDetailFactoryBean()
        jobDetailFactory.setJobClass(ExpiredsDeleter::class.java)
        jobDetailFactory.setName("ExpiredsDeleter")
        jobDetailFactory.setDescription("Delete Expireds URIs by date")
        jobDetailFactory.setDurability(true)
        return jobDetailFactory
    }

    /**
     * Creates a factory with the autowired context
     * @return jobFactory with application context
     */
    @Bean
    fun springBeanJobFactory(): SpringBeanJobFactory {
        val jobFactory = AutoWiringSpringBeanJobFactory()
        jobFactory.setApplicationContext(applicationContext)
        return jobFactory
    }

    /**
     * Schedule the deleter job with all of its components using
     * the configuration file and sets trigger, dataSource and job
     * details
     */
    @Bean
    fun scheduler(trigger: Trigger, job: JobDetail, quartzDataSource: DataSource): SchedulerFactoryBean {
        val schedulerFactory = SchedulerFactoryBean()
        schedulerFactory.setConfigLocation(
            ClassPathResource("quartz.properties")
        )
        schedulerFactory.setJobFactory(springBeanJobFactory())
        schedulerFactory.setJobDetails(job)
        schedulerFactory.setTriggers(trigger)
        schedulerFactory.setDataSource(quartzDataSource)
        return schedulerFactory
    }

    /**
     * Establish when to execute a job through CronTrigger
     * Misfire policy set to SMART, which implies not doing anything
     * with this trigger but it could be changed to REFIRE if needed
     * Launches the job every 10 seconds
     */
    @Bean
    fun trigger(job: JobDetail): CronTriggerFactoryBean {
        val trigger = CronTriggerFactoryBean()
        trigger.setJobDetail(job)
        trigger.setCronExpression("*/10 * * ? * * *")
        trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_SMART_POLICY)
        return trigger
    }
}

