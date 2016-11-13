package org.grails.plugins.httpbuilderhelper

import grails.test.mixin.TestMixin
import grails.test.mixin.services.ServiceUnitTestMixin
import grails.test.mixin.support.GrailsUnitTestMixin
import org.apache.http.impl.client.CloseableHttpClient
import org.slf4j.LoggerFactory
import spock.lang.Specification

@TestMixin([GrailsUnitTestMixin, ServiceUnitTestMixin])
class HttpClientFactorySpec extends Specification {

    static def log = LoggerFactory.getLogger(this)

    HttpClientFactory httpClientPoolFactory

    void setup() {
        httpClientPoolFactory = new HttpClientFactory()
        httpClientPoolFactory.afterPropertiesSet()
    }

    void "test jukinHttpClientPool instantiates"() {
        expect:
        httpClientPoolFactory.createClient() instanceof CloseableHttpClient
    }

    void "test jukinHttpClientPool is singleton"() {
        when:
        def someInstanceIds = (1..5).collect { httpClientPoolFactory.toString() }.toSet()
        log.info someInstanceIds.toString()

        then:
        someInstanceIds.size() == 1
    }
}
