package grails.plugin.httpbuilderhelper

import grails.test.mixin.TestFor
import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import groovyx.net.http.RESTClient
import org.apache.http.impl.client.CloseableHttpClient
import org.slf4j.LoggerFactory
import spock.lang.Specification

@TestFor(RestClientFactoryService)
@TestMixin([GrailsUnitTestMixin, ServiceUnitTestMixin])
class RestClientFactoryServiceSpec extends Specification {

    static def log = LoggerFactory.getLogger(this)

    void setupSpec() {
        defineBeans {
            // pools http clients
            //noinspection GrUnresolvedAccess
            httpClientPool(CloseableHttpClient) { bean ->
                bean.autowire = true
                bean.scope = 'singleton'
                bean.factoryMethod = 'createClient'
                bean.factoryBean = 'httpClientPoolFactory'
                bean.destroyMethod = 'close'
            }

            // sets reasonable default timeouts and a preference for TLSv1.2.
            //noinspection GrUnresolvedAccess
            httpClientPoolFactory(HttpClientFactory) { bean ->
                bean.autowire = true
                bean.scope = 'singleton'
                bean.initMethod = 'afterPropertiesSet'
            }
        }
        log.info "Logging is enabled for this test"
    }

    void "test httpClientPool instantiates"() {
        expect:
        service.httpClientPool instanceof CloseableHttpClient
    }

    void "test httpClientPool serves instance of RESTClient"() {
        expect:
        service.instance instanceof RESTClient
    }

    void "test httpClientPool is singleton"() {
        when:
        def someInstanceIds = (1..5).collect { service.httpClientPool.toString() }.toSet()
        log.info someInstanceIds.toString()

        then:
        someInstanceIds.size() == 1
    }

    void "test RESTClient is not singleton"() {
        when:
        def someInstanceIds = (1..5).collect { service.instance.toString() }.toSet()
        log.info someInstanceIds.toString()

        then:
        someInstanceIds.size() == 5
    }
}
