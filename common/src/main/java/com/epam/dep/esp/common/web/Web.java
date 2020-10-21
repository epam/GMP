/*
 *  /***************************************************************************
 *  Copyright (c) 2017, EPAM SYSTEMS INC
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  ***************************************************************************
 */

package com.epam.dep.esp.common.web;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.*;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.client.methods.*;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;


public class Web {
    public static final int DEFAULT_TIMEOUT = 120000;
    static final Logger logger = LoggerFactory.getLogger(Web.class);
    private RequestConfig requestConfig;
    private PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    private CloseableHttpClient httpClient;

    public Web() {
        this(DEFAULT_TIMEOUT);
    }

    public Web(int timeout) {
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);

        //Follow redirects
        LaxRedirectStrategy redirectStrategy = new LaxRedirectStrategy();

        httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setRedirectStrategy(redirectStrategy)
                .addInterceptorFirst(new HttpRequestInterceptor() {
                    public void process(
                            final HttpRequest request,
                            final HttpContext context) throws HttpException, IOException {
                        request.setHeader(HttpHeaders.ACCEPT_ENCODING, "gzip");
                        request.addHeader(HttpHeaders.ACCEPT_ENCODING, "deflate");
                    }
                })
                .addInterceptorFirst(new HttpResponseInterceptor() {
                    public void process(
                            final HttpResponse response,
                            final HttpContext context) throws HttpException, IOException {
                        HttpEntity entity = response.getEntity();
                        if (entity != null) {
                            Header ceHeader = entity.getContentEncoding();
                            if (ceHeader != null) {
                                HeaderElement[] codecs = ceHeader.getElements();
                                for (int i = 0; i < codecs.length; i++) {
                                    if (codecs[i].getName().equalsIgnoreCase("gzip")) {
                                        response.setEntity(
                                                new GzipDecompressingEntity(response.getEntity()));
                                        return;
                                    }

                                    if (codecs[i].getName().equalsIgnoreCase("deflate")) {
                                        response.setEntity(
                                                new DeflateDecompressingEntity(response.getEntity()));
                                        return;
                                    }
                                }
                            }
                        }
                    }
                })
                .build();

        requestConfig = RequestConfig.custom()
                .setSocketTimeout(timeout)
                .setConnectTimeout(timeout)
                .setConnectionRequestTimeout(timeout)
                .build();
    }

    /**
     * @param path - URL
     * @return String result for GET request
     * @throws WebToolsException
     */
    public String get(String path) throws WebToolsException {
        return get(path, null, null, null);
    }

    /**
     * @param path
     * @param headers
     * @param params
     * @param credentials
     * @param payload
     * @return String result for POST request with given parameters
     * @throws WebToolsException
     */
    public String post(String path, Map<String, String> headers, Map<String, String> params, Credentials credentials, HttpEntity payload) throws WebToolsException {
        try {
            URI uri = buildURI(path, params);
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setConfig(requestConfig);
            setRequestEntity(httpPost, payload);
            addHeaders(httpPost, headers);
            addCredentials(httpPost, credentials);
            HttpClientContext context = HttpClientContext.create();
            return performRequest(context, httpPost);
        } catch (URISyntaxException | IOException ex) {
            throw new WebToolsException("Unable to perform POST. " + ex.getMessage(), ex);
        }
    }

    protected void setRequestEntity(HttpEntityEnclosingRequestBase request, HttpEntity payload) {
        if (payload != null) {
            request.setEntity(payload);
        }
    }

    /**
     * @param path        path
     * @param headers     http headers
     * @param params      http params
     * @param credentials credentials
     * @return String result for GET request with given parameters
     * @throws WebToolsException
     */
    public String get(String path, Map<String, String> headers, Map<String, String> params, Credentials credentials) throws WebToolsException {
        try {
            URI uri = buildURI(path, params);
            HttpGet httpGet = new HttpGet(uri);
            httpGet.setConfig(requestConfig);
            addHeaders(httpGet, headers);
            addCredentials(httpGet, credentials);
            HttpClientContext context = HttpClientContext.create();
            return performRequest(context, httpGet);
        } catch (URISyntaxException | IOException ex) {
            throw new WebToolsException("Unable to perform GET. " + ex.getMessage(), ex);
        }
    }

    protected void addCredentials(HttpRequestBase httpRequest, Credentials credentials) {
        if (credentials == null) return;
        if (credentials instanceof UsernamePasswordCredentials) {
            //Add base http credentials
            String auth = credentials.getUserPrincipal().getName() + ":" + credentials.getPassword();
            byte[] encodedAuth = Base64.encodeBase64(auth.getBytes());
            String authHeader = "Basic " + new String(encodedAuth);
            httpRequest.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
        }
    }

    protected void addHeaders(HttpRequestBase httpRequest, Map<String, String> headers) {
        if (httpRequest != null && headers != null) {
            for (Map.Entry<String, String> item : headers.entrySet()) {
                httpRequest.setHeader(item.getKey(), item.getValue());
            }
        }
    }

    protected URI buildURI(String path, Map<String, String> params) throws URISyntaxException {
        URIBuilder builder = new URIBuilder(path);
        if (params != null) {
            for (Map.Entry<String, String> item : params.entrySet()) {
                builder.addParameter(item.getKey(), item.getValue());
            }
        }
        return builder.build();
    }

    protected String performRequest(HttpClientContext context, HttpRequestBase httpRequest) throws IOException {
        CloseableHttpResponse response = httpClient.execute(httpRequest, context);
        if (logger.isInfoEnabled()) {
            logger.info("Request to {}", httpRequest);
            logger.info("Response {}", response.getStatusLine());
        }
        try {
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                InputStream inStream = entity.getContent();
                try (InputStreamReader isr = new InputStreamReader(inStream)) {
                    int numCharsRead;
                    char[] charArray = new char[1024];
                    StringBuilder sb = new StringBuilder();
                    while ((numCharsRead = isr.read(charArray)) > 0) {
                        sb.append(charArray, 0, numCharsRead);
                    }
                    int statusCode = response.getStatusLine().getStatusCode();
                    if (statusCode >= 300) {
                        throw new WebToolsException("Response code is " + statusCode + " result: " + sb.toString());
                    }
                    return sb.toString();
                } finally {
                    inStream.close();
                }
            }
            return null;
        } finally {
            response.close();
        }
    }

    public void setRequestConfig(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
    }

    public void close() {
        logger.info("Closing Web Client...");
        if (httpClient != null) {
            try {
                httpClient.close();
            } catch (IOException ex) {
                logger.error("Unable to shutdown httClient ", ex);
            }
        }
    }
}
