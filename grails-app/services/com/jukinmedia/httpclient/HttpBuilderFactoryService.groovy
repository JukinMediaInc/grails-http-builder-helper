package com.jukinmedia.httpclient

import groovyx.net.http.HTTPBuilder
import org.apache.http.impl.client.CloseableHttpClient

/**
 * Provides a Factory to create new HTTPBuilder instances, which are not Threadsafe.
 * (http://stackoverflow.com/questions/27737175/is-grails-httpbuilder-thread-safe)
 *
 * These instances use httpClientPoolFactory to create their httpClient, so they come with Pool HTTP connections.
 * Additionally, the httpClient pool configures their clients with sensible things like Socket-level Connection
 * timeouts and preference for TLSv1.2.
 *
 * Intended to be a singleton, injected into Services and Controllers.
 */
class HttpBuilderFactoryService {

    CloseableHttpClient httpClientPool
    Object defaultURI
    Object defaultContentType

    HTTPBuilder getInstance() {
        HTTPBuilder builder
        if (defaultURI && defaultContentType) {
            builder = new HTTPBuilder(defaultURI, defaultContentType)
        } else if (defaultURI) {
            builder = new HTTPBuilder(defaultURI)
        } else {
            builder = new HTTPBuilder()
        }
        if (httpClientPool) {
            builder.client = httpClientPool
        }
        return builder
    }
}

