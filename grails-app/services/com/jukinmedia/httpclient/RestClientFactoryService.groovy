package com.jukinmedia.httpclient

import groovyx.net.http.RESTClient
import org.apache.http.impl.client.CloseableHttpClient

/**
 * Provides a Factory to create new RESTClient instances, which are not Threadsafe.
 * (http://stackoverflow.com/questions/27737175/is-grails-httpbuilder-thread-safe)
 *
 * These instances use httpClientPoolFactory to create their httpClient, so they come with Pool HTTP connections.
 * Additionally, the httpClient pool configures their clients with sensible things like Socket-level Connection
 * timeouts and preference for TLSv1.2.
 *
 * Intended to be a singleton, injected into Services and Controllers.
 */
class RestClientFactoryService {

    CloseableHttpClient httpClientPool
    Object defaultURI
    Object defaultContentType

    RESTClient getInstance() {
        RESTClient builder
        if (defaultURI && defaultContentType) {
            builder = new RESTClient(defaultURI, defaultContentType)
        } else if (defaultURI) {
            builder = new RESTClient(defaultURI)
        } else {
            builder = new RESTClient()
        }
        if (httpClientPool) {
            builder.client = httpClientPool
        }
        return builder
    }
}


