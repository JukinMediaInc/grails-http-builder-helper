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

import org.apache.http.client.config.RequestConfig
import org.apache.http.config.SocketConfig
import org.apache.http.conn.HttpClientConnectionManager
import org.apache.http.conn.ssl.DefaultHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.impl.client.HttpClients
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager
import org.apache.http.ssl.SSLContexts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.BeanNameAware
import org.springframework.beans.factory.InitializingBean

import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext

/**
 * Provides a ThreadSafe HttpClient pool, for use with HttpBuilder.
 * Intended to be a singleton, injected into a prototype scope HTTPBuilder instance
 * (because it might not be threadsafe: http://stackoverflow.com/questions/27737175/is-grails-httpbuilder-thread-safe)
 */
class HttpClientFactory implements InitializingBean, BeanNameAware {

    private def log = LoggerFactory.getLogger(this.class)

    String beanName

    // Usage settings
    Boolean pooling = true
    Integer connectionManagerMaxTotal = 200
    Integer connectionManagerDefaultMaxPerRoute = 20
    Boolean followRedirects = true
    Integer socketTimeout = 20000
    Integer connectionTimeout = 10000
    Boolean useSsl = true
    String[] supportedProtocols = ["TLSv1.2", "TLSv1.1", "TLSv1"] as String[]
    // Prefer TLS v1.2 to work around bug where our JDK prefers v1.1 but it doesn't work with some https server configurations
    Boolean tcpNoDelay = false
    // Written data to the network is not buffered pending acknowledgement of previously written data.

    // Override any of these objects in resources.groovy to further customize HttpClient
    HttpClientBuilder hcb
    SocketConfig.Builder scb
    RequestConfig.Builder rcb
    HostnameVerifier sslHostnameVerifier
    SSLContext sslContext
    HttpClientConnectionManager cm

    @Override
    void afterPropertiesSet() {
        initializeSettings()
    }

    void initializeSettings() {
        // Override any of these objects to further customize HttpClient
        hcb = HttpClients.custom()
        scb = SocketConfig.custom()
        rcb = RequestConfig.custom()

        sslContext = SSLContexts.createDefault()
        sslHostnameVerifier = new DefaultHostnameVerifier()
    }

    /**
     * HTTPBuilder is built with deprecated Apache Client 4.2 dependencies, but we're using 4.3 APIs inside the 4.5
     * library distribution here.
     *
     * @return configured httpclient
     */
    CloseableHttpClient createClient() {
        if (socketTimeout != null) {
            log.info "${beanName}: Setting socketTimeout to ${socketTimeout / 1000}s for all requests and socket operations"
            rcb.setSocketTimeout(socketTimeout)
            scb.setSoTimeout(socketTimeout)
        }

        if (connectionTimeout != null) {
            log.info "${beanName}: Setting connectionTimeout and connectionRequestTimeout to ${connectionTimeout / 1000}s for all requests"
            rcb.setConnectTimeout(connectionTimeout)
            rcb.setConnectionRequestTimeout(connectionTimeout)
        }

        if (!followRedirects) {
            log.info "${beanName}: Redirects will NOT be follwed"
            hcb.disableRedirectHandling()
        } else {
            log.info "${beanName}: Redirects will be follwed"
        }

        if (useSsl) {
            log.info "${beanName}: Enabling SSL"
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslContext, supportedProtocols, null, sslHostnameVerifier)
            hcb.setSSLSocketFactory(sslsf)
        } else {
            log.info "${beanName}: SSL disabled"
        }

        if (pooling) {
            log.info "${beanName}: Enabling pooled httpClients; maxPerRoute=${connectionManagerDefaultMaxPerRoute}, max=${connectionManagerMaxTotal}"
            PoolingHttpClientConnectionManager pcm = cm && cm instanceof PoolingHttpClientConnectionManager ? cm : new PoolingHttpClientConnectionManager()
            pcm.setMaxTotal(connectionManagerMaxTotal)
            pcm.setDefaultMaxPerRoute(connectionManagerDefaultMaxPerRoute)
            hcb.setConnectionManager(pcm)

        } else if (cm) {
            log.info "${beanName}: Enabling Connection Manager override"
            hcb.setConnectionManager(cm)
        }

        if (tcpNoDelay) {
            log.info "${beanName}: Enabling TCP NoDelay to stream IO without checking previous writes"
            scb.setTcpNoDelay(true)
        }

        SocketConfig sc = scb.build()
        RequestConfig rc = rcb.build()
        hcb.setDefaultSocketConfig(sc).setDefaultRequestConfig(rc)

        CloseableHttpClient hc = hcb.build()
        return hc
    }

    @Override
    void setBeanName(String s) {
        this.beanName = s
    }
}
