Grails HTTP Builder Helper
==========================

Grails 3 plugin which extends the http-builder plugin by upgrading HTTPBuilder to 0.7.1, 
upgrading Apache HTTP to 4.5.x and adding new factory services for pooling http clients 
with sensible defaults. 

To use, inject either a RestClientFactoryService or HttpBuilderFactoryService into your
controller or service, and fetch a new HTTP client instance via the getInstance method.

To override the new default settings for all client connections, copy and create your own HttpClientFactory,
and register it as "httpClientPoolFactory" in your resources.groovy file, like this but with your own class.

    httpClientPoolFactory(HttpClientFactory) { bean ->
        bean.scope = 'singleton'
        bean.initMethod = 'afterPropertiesSet'
    }

Future Vision:

- allow configuration of client connection without forking a new class based on HttpClientFactory.
- allow configuration of client connections to not be global to all factories.

Developers: to build and distribute this plugin, run

    ./grailsw -i assemble publish
    
with appropriate credentials already set in ~/.gradle/gradle.properties for the target repository.
