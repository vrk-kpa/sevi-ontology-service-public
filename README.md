# Ontology service

This application is part of the [National Architecture for Digital Services programme](http://vm.fi/en/national-architecture-for-digital-services)
(Kansallinen palveluarkkitehtuuri, also known as KaPA) by [The Ministry of Finance of Finland](http://vm.fi/en/ministry) and its Service Views (Palvelunäkymät) project.

## Description
A microservice that provides ontology related services and refreshes ontology related indexes in search engine by sending the concepts to be indexed to certain JMS topic.

Ontology service provides services for microservices handling content and tags (for example content production, ptv, etc... )

Behind the scenes, this service uses [Apache Jena Fuseki](https://jena.apache.org/documentation/serving_data/), served as Docker image [sevi-fuseki](https://github.com/vrk-kpa/sevi-docker-fuseki-public), for storing the data.

The concepts provided by this service are divided into different concept schemes from [FINTO](http://finto.fi) ontologies. In practice, the schemes provided are:

| concept type | scheme | ontology |
| --- | --- | --- |
| jupo | http://www.yso.fi/onto/jupo/ | http://www.yso.fi/onto/jupo/ |
| yso | http://www.yso.fi/onto/yso/ | http://www.yso.fi/onto/yso/ |
| liito | http://www.yso.fi/onto/liito/ | http://www.yso.fi/onto/liito/ |
| juho | http://www.yso.fi/onto/juho/ | http://www.yso.fi/onto/juho/ |
| tero | http://www.yso.fi/onto/tero/ | http://www.yso.fi/onto/tero/ |
| tsr | http://www.yso.fi/onto/tsr/ | http://www.yso.fi/onto/tsr/ |
| targetgroup | http://urn.fi/URN:NBN:fi:au:ptvl:KR | http://finto.fi/ptvl/fi/ |
| lifesituation | http://urn.fi/URN:NBN:fi:au:ptvl:KE | http://finto.fi/ptvl/fi/ |
| ptvl | http://urn.fi/URN:NBN:fi:au:ptvl: | http://finto.fi/ptvl/fi/ |

You can make queries to the sevi-ontology-service based with semantic concept URIs and concept schemes.

Use [sevi-search-service](https://github.com/vrk-kpa/sevi-search-service-public) for getting URIs for plain text labels.

See the provided REST API in: [/src/main/java/fi/vm/kapa/sevi/ontology/resource](https://github.com/vrk-kpa/sevi-ontology-service-public/tree/master/src/main/java/fi/vm/kapa/sevi/ontology/resource)

### Example queries:

Configuring the mappings to Elasticsearch - Do a HTTP PUT to:
`http://localhost:9089/sevi-ontology-service/ontology/v1/mappings`

Indexing all the concepts to sevi-search-service (takes a while) - Do a HTTP PUT to:
`http://localhost:9089/sevi-ontology-service/ontology/v1/index?indexAllConcepts=false`

Return all the JUPO concepts - Do a HTTP GET to:
`http://localhost:9089/sevi-ontology-service/ontology/v1/concepts/jupo`

## Interface Documentation

When the microservice is running, you can get the Swagger REST API documentation from:
[http://localhost:9089/sevi-ontology-service/swagger/index.html](http://localhost:9089/sevi-ontology-service/swagger/index.html)

## Prerequisities

### Building
- Java 8+
- Maven 3.3+
- Docker 

### Running
- [sevi-config-public](https://github.com/vrk-kpa/sevi-config-public) - Default configuration for development use
- [sevi-fuseki](https://github.com/vrk-kpa/sevi-docker-fuseki-public) - Build and run sevi-fuseki Docker image


## Starting service on local development environment

Install the module into an adjanced directory with sevi-config.

Then use: `mvn jetty:run`

## Building the Docker Image

```bash
$ mvn clean package docker:build
```

## Running the Docker Image

```bash
$ docker run -p 9089:9089 -p 19089:19089 -v /path/to/sevi-config-public:/config --name sevi-ontology-service sevi-ontology-service -a --spring.config.location=/config/application.yml,/config/sevi-ontology-service.yml
```

.. or in [sevi-compose-public](https://github.com/vrk-kpa/sevi-compose-public) run 

```bash
$ docker-compose up ontologyservice
```

##Testing

Running the tests:

  - `mvn verify`

## Issues and Improvements

Please notify kapa@vrk.fi for any defects, deficiencies and security issues if discovered.
