/*
 * Copyright (C) 2016 - present Juergen Zimmermann, Hochschule Karlsruhe
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.hska.kunde

import de.hska.kunde.config.Settings.banner
import de.hska.kunde.config.Settings.props
import org.springframework.boot.WebApplicationType.REACTIVE
import org.springframework.boot.actuate.autoconfigure.cache.CachesEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.condition.ConditionsReportEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.logging.LoggersEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.management.HeapDumpWebEndpointAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.JvmMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.KafkaMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.LogbackMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.SystemMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.metrics.web.tomcat.TomcatMetricsAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.scheduling.ScheduledTasksEndpointAutoConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration
import org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
import org.springframework.boot.autoconfigure.hateoas.HypermediaAutoConfiguration
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration
import org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration
import org.springframework.boot.autoconfigure.web.client.RestTemplateAutoConfiguration
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration
import org.springframework.boot.context.ApplicationPidFileWriter
import org.springframework.boot.runApplication
import org.springframework.cloud.autoconfigure.LifecycleMvcEndpointAutoConfiguration
import org.springframework.cloud.autoconfigure.RefreshEndpointAutoConfiguration
import org.springframework.cloud.sleuth.instrument.rxjava.RxJavaAutoConfiguration
import reactor.tools.agent.ReactorDebugAgent

/**
 * Die Klasse, die beim Start des Hauptprogramms verwendet wird, um zu
 * konfigurieren, dass es sich um eine Anwendung mit _Spring Boot_ handelt.
 * Dadurch werden auch viele von Spring Boot gelieferte Konfigurationsklassen
 * automatisch konfiguriert.
 *
 * [Use Cases](../../../../docs/images/kunde.uc.png)
 *
 * [Komponentendiagramm](../../../../docs/images/kunde.comp.png)
 *
 * @author [Jürgen Zimmermann](mailto:Juergen.Zimmermann@HS-Karlsruhe.de)
 */
@SpringBootApplication(
    exclude = [
        AopAutoConfiguration::class,
        CachesEndpointAutoConfiguration::class,
        CompositeMeterRegistryAutoConfiguration::class,
        ConditionsReportEndpointAutoConfiguration::class,
        EmbeddedWebServerFactoryCustomizerAutoConfiguration::class,
        ErrorMvcAutoConfiguration::class,
        GsonAutoConfiguration::class,
        HeapDumpWebEndpointAutoConfiguration::class,
        HypermediaAutoConfiguration::class,
        JvmMetricsAutoConfiguration::class,
        KafkaMetricsAutoConfiguration::class,
        LifecycleMvcEndpointAutoConfiguration::class,
        LogbackMetricsAutoConfiguration::class,
        LoggersEndpointAutoConfiguration::class,
        MetricsAutoConfiguration::class,
        MongoRepositoriesAutoConfiguration::class,
        PersistenceExceptionTranslationAutoConfiguration::class,
        RefreshEndpointAutoConfiguration::class,
        RestTemplateAutoConfiguration::class,
        RxJavaAutoConfiguration::class,
        SimpleMetricsExportAutoConfiguration::class,
        ScheduledTasksEndpointAutoConfiguration::class,
        SystemMetricsAutoConfiguration::class,
        TaskExecutionAutoConfiguration::class,
        TaskSchedulingAutoConfiguration::class,
        TomcatMetricsAutoConfiguration::class,
        TransactionAutoConfiguration::class,
        WebMvcAutoConfiguration::class

        // ConnectionFactoryAutoConfiguration::class,
        // R2dbcDataAutoConfiguration::class,
        // R2dbcRepositoriesAutoConfiguration::class,
        // R2dbcTransactionManagerAutoConfiguration::class
    ]
)
class Application

/**
 * Hauptprogramm, um den Microservice zu starten.
 *
 * @param args Evtl. zusätzliche Argumente für den Start des Microservice
 */
fun main(args: Array<String>) {
    ReactorDebugAgent.init()
    // FIXME https://github.com/reactor/BlockHound/issues/33 fuer JDK 13
    // Zipkin (fuer Spring Cloud Sleuth) nutzt das blockierende RestTemplate als Web-Client
    // BlockHound.install()

    @Suppress("SpreadOperator")
    runApplication<Application>(*args) {
        webApplicationType = REACTIVE
        setBanner(banner)
        setDefaultProperties(props)
        addListeners(ApplicationPidFileWriter())
    }
}
