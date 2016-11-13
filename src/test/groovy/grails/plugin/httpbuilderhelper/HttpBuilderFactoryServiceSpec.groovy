package grails.plugin.httpbuilderhelper

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.http.impl.client.CloseableHttpClient
import org.slf4j.LoggerFactory
import spock.lang.Specification

/**
 * See the API for {@link grails.test.mixin.services.ServiceUnitTestMixin} for usage instructions
 */
@TestFor(HttpBuilderFactoryService)
@TestMixin([GrailsUnitTestMixin, ServiceUnitTestMixin])
class HttpBuilderFactoryServiceSpec extends Specification {

    static def log = LoggerFactory.getLogger(this)

    void setupSpec() {
        defineBeans {
            // pools http clients
            httpClientPool(CloseableHttpClient) { bean ->
                bean.autowire = true
                bean.scope = 'singleton'
                bean.factoryMethod = 'createClient'
                bean.factoryBean = 'httpClientPoolFactory'
                bean.destroyMethod = 'close'
            }

            // sets reasonable default timeouts and a preference for TLSv1.2.
            httpClientPoolFactory(HttpClientFactory) { bean ->
                bean.autowire = true
                bean.scope = 'singleton'
                bean.initMethod = 'afterPropertiesSet'
            }
        }
        log.info "Logging is enabled for this test"
    }

    void "test jukinHttpClientPool instantiates"() {
        expect:
        service.httpClientPool instanceof CloseableHttpClient
    }

    void "test jukinHttpClientPool is singleton"() {
        when:
        def someInstanceIds = (1..5).collect { service.httpClientPool.toString() }.toSet()
        log.info someInstanceIds.toString()

        then:
        someInstanceIds.size() == 1
    }
}
