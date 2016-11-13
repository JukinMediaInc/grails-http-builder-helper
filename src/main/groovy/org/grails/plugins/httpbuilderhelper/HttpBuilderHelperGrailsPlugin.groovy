/*
 * Copyright 2009-2015 the original author or authors.
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

import groovyx.net.http.AsyncHTTPBuilder
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.RESTClient
import org.apache.http.impl.client.CloseableHttpClient

import java.lang.reflect.InvocationTargetException

import org.apache.http.impl.conn.ProxySelectorRoutePlanner

import org.grails.plugins.httpbuilderhelper.ssl.HTTPBuilderSSLConstants
import org.grails.plugins.httpbuilderhelper.ssl.HTTPBuilderSSLHelper
import org.grails.plugins.httpbuilderhelper.ssl.SimpleHTTPBuilderSSLHelper

import grails.plugins.Plugin

class HttpBuilderHelperGrailsPlugin extends Plugin {
    def grailsVersion = "3.0.0 > *"
    def profiles = ['web']

    private HTTPBuilderSSLHelper sslHelper = new SimpleHTTPBuilderSSLHelper()

    def doWithDynamicMethods = { ctx -> processArtifacts(application) }
    def onChange = { event -> processArtifacts(application) }
    def onConfigChange = { event -> processArtifacts(application) }

    private void processArtifacts(application) {
        def config = application.config
        def types = config.grails?.rest?.injectInto ?: ["Controller", "Service"]
        types.each { type ->
            application.getArtefacts(type).each { klass -> addDynamicMethods(klass, application) }
        }
    }

    private void addDynamicMethods(klass, application) {
        klass.metaClass.withAsyncHttp = withClient.curry(AsyncHTTPBuilder, klass, application)
        klass.metaClass.withHttp = withClient.curry(HTTPBuilder, klass, application)
        klass.metaClass.withRest = withClient.curry(RESTClient, klass, application)
    }

    private withClient = { Class klass, Object target, application, Map params, Closure closure ->
        def client
        if (params.id) {
            String id = params.remove("id").toString()
            if (target.metaClass.hasProperty(target, id)) {
                client = target.metaClass.getProperty(target, id)
            } else {
                client = makeClient(klass, params, application)
                target.metaClass."$id" = client
            }
        } else {
            client = makeClient(klass, params, application)
        }

        setRoutePlanner(client)

        if (closure) {
            closure.delegate = client
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure()
        }
    }

    private makeClient(Class klass, Map params, application) {
        def client
        if (klass == AsyncHTTPBuilder) {
            client = makeAsyncClient(klass, params)

        } else {
            client = makeSyncClient(klass, params)
        }

        if (HTTPBuilderSSLConstants.HTTPS == client.uri.toURL().protocol) {
            addSSLSupport(client, application)
        }

        return client
    }

    private makeAsyncClient(Class klass, Map params){
        def client
        try {
            Map args = [:]
            [ "threadPool", "poolSize", "uri", "contentType", "timeout" ].each { arg ->
                if (params[(arg)] != null) args[(arg)] = params[(arg)]
            }
            client = klass.newInstance(args)

        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to create async http client reason: $e", e)
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to create async http client reason: $e", e)
        }
        return client
    }

    private makeSyncClient(Class klass, Map params){
        def client
        try {
            client = klass.newInstance()
            if (params.uri) client.uri = params.remove("uri")
            if (params.contentType) client.contentType = params.remove("contentType")
            if (params.charset) client.getEncoder().setCharset(params.remove("charset"))
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to create ${(klass == HTTPBuilder ? 'http' : 'rest')} client reason: $e", e)
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Failed to create ${(klass == HTTPBuilder ? 'http' : 'rest')} client reason: $e", e)
        }
        return client
    }

    private addSSLSupport(client, application){
        try {
            sslHelper.addSSLSupport(application.config?.rest, client)
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Failed to add ssl support : ${e.message}", e)
        } catch (IllegalStateException e) {
            throw new RuntimeException("Failed to add ssl support : ${e.message}", e)
        }
    }

    private setRoutePlanner(builder){
        builder.client.routePlanner = new ProxySelectorRoutePlanner(
            builder.client.connectionManager.schemeRegistry,
            ProxySelector.default
        )
    }

    Closure doWithSpring() {
        { ->
            restClientFactoryService(RestClientFactoryService) { bean ->
                httpClientPool = ref('httpClientPool')
            }

            httpBuilderFactoryService(HttpBuilderFactoryService) { bean ->
                httpClientPool = ref('httpClientPool')
            }

            // pools http clients
            httpClientPool(CloseableHttpClient) { bean ->
                bean.scope = 'singleton'
                bean.factoryMethod = 'createClient'
                bean.factoryBean = 'httpClientPoolFactory'
                bean.destroyMethod = 'close'
            }

            // sets reasonable default timeouts and a preference for TLSv1.2.
            httpClientPoolFactory(HttpClientFactory) { bean ->
                bean.scope = 'singleton'
                bean.initMethod = 'afterPropertiesSet'
            }
        }
    }
}
