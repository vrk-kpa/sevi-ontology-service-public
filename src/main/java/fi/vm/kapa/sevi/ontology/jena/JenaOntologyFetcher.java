/**
 * The MIT License
 * Copyright (c) 2015 Population Register Centre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package fi.vm.kapa.sevi.ontology.jena;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.atlas.web.HttpException;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import fi.vm.kapa.sevi.ontology.exception.ConceptParserException;
import fi.vm.kapa.sevi.ontology.exception.DatasetCreateException;
import fi.vm.kapa.sevi.ontology.exception.DatasetDeleteException;
import fi.vm.kapa.sevi.ontology.exception.OntologyServerException;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Component
public class JenaOntologyFetcher implements ResourceLoaderAware{
    private static final Logger LOGGER = LoggerFactory.getLogger(JenaOntologyFetcher.class);

    // Fuseki settings
    @Value("${environment.fuseki.host}")
    protected String fusekiHost;
    @Value("${environment.fuseki.port}")
    protected String fusekiPort;

    // Outbound proxy settings
    @Value("${outbound.httpProxy.enabled:false}")
    protected Boolean httpProxyEnabled;
    @Value("${outbound.httpProxy.host:localhost}")
    private String httpProxyHost;
    @Value("${outbound.httpProxy.port:8080}")
    private Integer httpProxyPort;

    private ResourceLoader resourceLoader;

    private List<String> ontologyResources;

    private InputStream getInputStreamForResource(String resource) throws IOException {
        InputStream inputStream;
        if (resource.startsWith("http://")) {
            inputStream = openInputStreamResource(resource);
        } else {
            LOGGER.info("Loading {} with resource loader", resource);
            inputStream = resourceLoader.getResource(resource).getInputStream();
        }
        return inputStream;
    }
    
    /**
     * @param ontologyType Either "RDF/XML" or "TURTLE" for example. The format in which the inputstream is in.
     * @throws IOException 
     * @throws ClientProtocolException 
     * @throws DatasetCreateException
     * @throws ConceptParserException
     */
    private void createFusekiTDBDatasetForURI(String ontologyResource, String ontologyType, String serviceURI) throws IOException {
        LOGGER.info("Creating Fuseki dataset. Fuseki url: {}", serviceURI);
        InputStream inputStream = getInputStreamForResource(ontologyResource);
        
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpEntityEnclosingRequest uploadFile = new HttpPost(serviceURI + "/upload");

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        String filename;
        // If the source is RDF the file extension must be changed here too.
        if ("TURTLE".equals(ontologyType)) {
            filename = "file.ttl";
        } else {
            filename = "file.rdf";
        }
        builder.addBinaryBody("file", inputStream, ContentType.MULTIPART_FORM_DATA, filename);
        HttpEntity multipart = builder.build();

        uploadFile.setEntity(multipart);

        httpClient.execute((HttpUriRequest) uploadFile);
        
        LOGGER.info("Dataset created successfully");
    }
    
    /**
     * @param ontologyType Either "RDF/XML" or "TURTLE" for example. The format in which the inputstream is in.
     * @throws DatasetCreateException
     * @throws ConceptParserException
     */
    protected void createFusekiTDBDataset(String ontologyResource, String ontologyType) {
        List<String> serviceURIs = getFusekiServiceUris();
        for (String serviceURI : serviceURIs) {
            try {
                createFusekiTDBDatasetForURI(ontologyResource, ontologyType, serviceURI);
            } catch (HttpException httpException) {
                if (httpException.getResponseCode() == 405) {
                    LOGGER.info("Dataset was not found. Please create " + serviceURI + " dataset");
                }
                throw new DatasetCreateException("Error creating dataset", httpException);
            } catch (Exception e) {
                throw new DatasetCreateException("Error creating dataset", e);
            }
        }
    }

    public void deleteFusekiTDBDataset() {
        List<String> serviceURIs = getFusekiServiceUris();
        for (String serviceURI : serviceURIs) {
            try {
                LOGGER.info("Deleting Fuseki dataset. Fuseki url: " + serviceURI);
                UpdateRequest request = UpdateFactory.create(buildQueryDeleteAll());
                UpdateProcessor processor = UpdateExecutionFactory.createRemote(request, serviceURI + "/update");
                processor.execute();
                LOGGER.info("Dataset deleted successfully");
            } catch (HttpException httpException) {
                if (httpException.getResponseCode() == 404) {
                    LOGGER.info("Dataset was not found. Continuing...");
                } else {
                    throw new DatasetDeleteException("Delete dataset error", httpException);
                }
            } catch (RuntimeException e) {
                throw new DatasetDeleteException("Delete dataset error", e);
            }
        }
    }

    @Value("#{'${ontology.finto.ontology-urls}'.split(',')}")
    public void setOntologyResources(List<String> ontologyResources) {
        this.ontologyResources = ontologyResources;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void createFusekiTDBDataset() {
        addOrCreateFusekiTDBDataset();
    }

    private void addOrCreateFusekiTDBDataset() {
        for (String ontologyResource : ontologyResources) {
            try {
                String ontologyType = "TURTLE";
                if (ontologyResource.endsWith(".rdf")) {
                    ontologyType = "RDF/XML";
                }
                createFusekiTDBDataset(ontologyResource, ontologyType);
            } catch (OntologyServerException e) {
                LOGGER.error(e.getLocalizedMessage(), e);
            }
        }
    }
    private String buildQueryDeleteAll() {
        return "CLEAR DEFAULT";
    }

    private InputStream openInputStreamResource(String resource) throws IOException {
        URL url = new URL(resource);
        LOGGER.info("{} (Proxy: {} via {}:{})", url.toString(), httpProxyEnabled, httpProxyHost, httpProxyPort);
        InputStream in = httpProxyEnabled ? url.openConnection(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort))).getInputStream() : url.openStream();
        return in;
    }

    protected List<String> getFusekiServiceUris() {
        return Arrays.asList("http://" + fusekiHost + ":" + fusekiPort + "/all", "http://" + fusekiHost + ":" + fusekiPort + "/all_no_inference");
    }
}
