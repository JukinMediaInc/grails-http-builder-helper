/*
 * Copyright 2009-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.plugins.httpbuilderhelper

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

    HTTPBuilder getInstance() {
        HTTPBuilder builder = new HTTPBuilder()
        if (httpClientPool) {
            builder.client = httpClientPool
        }
        return builder
    }

    HTTPBuilder getInstance(String defaultURI) {
        HTTPBuilder builder = new HTTPBuilder(defaultURI)
        if (httpClientPool) {
            builder.client = httpClientPool
        }
        return builder
    }

    HTTPBuilder getInstance(String defaultURI, String defaultContentType) {
        HTTPBuilder builder = new HTTPBuilder(defaultURI, defaultContentType)
        if (httpClientPool) {
            builder.client = httpClientPool
        }
        return builder
    }
}

