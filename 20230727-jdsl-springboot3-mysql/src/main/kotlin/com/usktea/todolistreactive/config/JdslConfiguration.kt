package com.usktea.todolistreactive.config

import com.linecorp.kotlinjdsl.query.creator.SubqueryCreator
import com.linecorp.kotlinjdsl.query.creator.SubqueryCreatorImpl
import com.linecorp.kotlinjdsl.spring.data.reactive.query.SpringDataHibernateMutinyReactiveQueryFactory
import com.zaxxer.hikari.HikariDataSource
import jakarta.persistence.spi.PersistenceUnitInfo
import org.hibernate.reactive.mutiny.Mutiny
import org.hibernate.reactive.provider.ReactivePersistenceProvider
import org.hibernate.reactive.provider.Settings
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import java.util.*

@Configuration
class JdslConfig {

    @Bean
    fun subQueryCreator(): SubqueryCreator {
        return SubqueryCreatorImpl()
    }

    @Bean
    fun mutinySessionFactory(localSessionFactoryBean: LocalContainerEntityManagerFactoryBean): Mutiny.SessionFactory {
        val reactivePersistenceInfo = ReactivePersistenceInfo(
            localSessionFactoryBean.persistenceUnitInfo!!,
            localSessionFactoryBean.jpaPropertyMap
        )
        return ReactivePersistenceProvider()
            .createContainerEntityManagerFactory(
                reactivePersistenceInfo,
                reactivePersistenceInfo.properties
            )
            .unwrap(Mutiny.SessionFactory::class.java)
    }

    @Bean
    fun queryFactory(
        sessionFactory: Mutiny.SessionFactory,
        subQueryCreator: SubqueryCreator
    ): SpringDataHibernateMutinyReactiveQueryFactory {
        return SpringDataHibernateMutinyReactiveQueryFactory(
            sessionFactory = sessionFactory,
            subqueryCreator = subQueryCreator
        )
    }
}

class ReactivePersistenceInfo(
    persistenceUnitInfo: PersistenceUnitInfo,
    jpaPropertyMap: Map<String, Any>,
) :
    PersistenceUnitInfo by persistenceUnitInfo {

    private val internalProps = Properties(persistenceUnitInfo.properties)
        .apply {
            putAll(jpaPropertyMap)

            setProperty(Settings.SQL_CLIENT_POOL, MysqlConnectionPool::class.qualifiedName)

            setProperty(
                Settings.URL,
                persistenceUnitInfo.nonJtaDataSource.unwrap(HikariDataSource::class.java).jdbcUrl
            )
            setProperty(
                Settings.USER,
                persistenceUnitInfo.nonJtaDataSource.unwrap(HikariDataSource::class.java).username
            )
            setProperty(
                Settings.PASS,
                persistenceUnitInfo.nonJtaDataSource.unwrap(HikariDataSource::class.java).password ?: ""
            )
        }

    override fun getProperties(): Properties = internalProps

    override fun getPersistenceProviderClassName(): String =
        ReactivePersistenceProvider::class.qualifiedName!!
}
