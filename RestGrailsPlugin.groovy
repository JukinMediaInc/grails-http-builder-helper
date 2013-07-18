/*
 * Copyright 2009-2010 the original author or authors.
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

import groovyx.net.http.AsyncHTTPBuilder
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.RESTClient
import org.apache.http.conn.scheme.Scheme
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.impl.conn.ProxySelectorRoutePlanner

import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import java.lang.reflect.InvocationTargetException

import org.grails.plugins.rest.ssl.HTTPBuilderSSLHelper
import org.grails.plugins.rest.ssl.HTTPBuilderSSLConstants
import org.grails.plugins.rest.ssl.SimpleHTTPBuilderSSLHelper

/**
 * @author Andres.Almiray
 */
class RestGrailsPlugin {
	// the plugin version
	def version = "0.8"
	// the version or versions of Grails the plugin is designed for
	def grailsVersion = "2.0 > *"
	// the other plugins this plugin depends on
	def dependsOn = [:]
	// resources that are excluded from plugin packaging
	def pluginExcludes = [
		"grails-app/views/error.gsp"
	]

	def author = "Andres Almiray, Bernardo Gomez-Palacio, Marco Vermeulen"
	def authorEmail = "aalmiray@users.sourceforge.net, bernardo.gomezpalacio@gmail.com, vermeulen.mp@gmail.com"
	def title = "REST client facilities"
	def description = '''
Adds REST client capabilities to your Grails application.
'''
	def observe = ['controllers', 'services']

	// URL to the plugin's documentation
	def documentation = "http://grails.org/Rest+Plugin"

	/**
	 */
	HTTPBuilderSSLHelper sslHelper = new SimpleHTTPBuilderSSLHelper()

	def doWithDynamicMethods = { ctx -> processArtifacts() }

	def onChange = { event -> processArtifacts() }

	def onConfigChange = { event -> processArtifacts() }

	private processArtifacts() {
		def config = ConfigurationHolder.config
		def application = ApplicationHolder.application
		def types = config.grails?.rest?.injectInto ?: ["Controller", "Service"]
		types.each { type ->
			application.getArtefacts(type).each { klass -> addDynamicMethods(klass) }
		}
	}

	private addDynamicMethods(klass) {
		klass.metaClass.withAsyncHttp = withClient.curry(AsyncHTTPBuilder, klass)
		klass.metaClass.withHttp = withClient.curry(HTTPBuilder, klass)
		klass.metaClass.withRest = withClient.curry(RESTClient, klass)
	}

	// ======================================================

	private withClient = { Class klass, Object target, Map params, Closure closure ->
		def client = null
		if (params.id) {
			String id = params.remove("id").toString()
			if (target.metaClass.hasProperty(target, id)) {
				client = target.metaClass.getProperty(target, id)
			} else {
				client = makeClient(klass, params)
				target.metaClass."$id" = client
			}
		} else {
			client = makeClient(klass, params)
		}

		setRoutePlanner(client)

		if (closure) {
			closure.delegate = client
			closure.resolveStrategy = Closure.DELEGATE_FIRST
			closure()
		}
	}

	private makeClient(Class klass, Map params) {
		def client
		if (klass == AsyncHTTPBuilder) {
			client = makeAsyncClient(klass, params)

		} else {
			client = makeSyncClient(klass, params)
		}

		if (HTTPBuilderSSLConstants.HTTPS == client.uri.toURL().protocol) {
			addSSLSupport(client)
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

		} catch (IllegalArgumentException e) {
			throw new RuntimeException("Failed to create ${(klass == HTTPBuilder ? 'http' : 'rest')} client reason: $e", e)
		} catch (InvocationTargetException e) {
			throw new RuntimeException("Failed to create ${(klass == HTTPBuilder ? 'http' : 'rest')} client reason: $e", e)
		}
		return client
	}

	private addSSLSupport(client){
		try {
			sslHelper.addSSLSupport(ConfigurationHolder.config?.rest, client)
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
}
