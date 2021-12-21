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

import org.apache.http.HttpException;
import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

@Deprecated
/**
 * Use com.epam.dep.esp.common.web.Web instead
 */
class WebUtils {
    static final Logger logger = LoggerFactory.getLogger(WebUtils.class);
    protected String method;
    protected String login;
    protected String password;
    protected Object payload;
    Integer timeout = 120000;
    Map<String, String> params = null;
    Map<String, String> headers = null;
    private String path;

    WebUtils(String method, String path, Map<String, String> headers, Map<String, String> params, String login, String password, Object payload) {
        this.params = params;
        this.path = path;
        this.method = method;
        this.login = login;
        this.password = password;
        this.headers = headers;
        this.payload = payload;
    }

    private HttpURLConnection getConnection(int count) throws MalformedURLException, IOException, URISyntaxException, HttpException {
        URIBuilder builder = new URIBuilder(path);

        if (params != null) {
            for (Map.Entry<String, String> item : params.entrySet()) {
                builder.addParameter(item.getKey(), item.getValue());
            }
        }

        URL serverAddress = builder.build().toURL();


        HttpURLConnection conn = (HttpURLConnection) serverAddress.openConnection();
        conn.setConnectTimeout(timeout);
        conn.setReadTimeout(timeout);
        if (password != null && login != null && password.length() > 0 && login.length() > 0) {
            conn.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((login + ":" + password).getBytes()));
        }

        if (headers != null) {
            for (Map.Entry<String, String> item : headers.entrySet()) {
                conn.setRequestProperty(item.getKey(), item.getValue());
            }
        }

        conn.setRequestMethod(method);
        if ((method.equalsIgnoreCase("post")
                || method.equalsIgnoreCase("put"))
                && payload != null) {
            if (payload instanceof String) {
                conn.setDoOutput(true);
                OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream(), StandardCharsets.UTF_8);
                writer.write((String) payload);
                writer.close();
            }
        }

        if (count == 0) {
            throw new HttpException("Can't find path: " + serverAddress);
        }
        switch (conn.getResponseCode()) {
            case 302:
            case 301:
                String redirectTo = conn.getHeaderField("location");
                conn.disconnect();
                path = redirectTo;
                conn = getConnection(count - 1);
                break;
        }
        return conn;
    }

    public String perform() throws HttpException {
        return perform(1, 10);
    }

    public String perform(int i, int time) throws HttpException {
        HttpURLConnection conn = null;
        try {

            if (i == 0) throw new HttpException("Unable to perform operation");
            //TODO add multi address support
            //TODO add PORT support
            conn = getConnection(10);
            int responseCode = conn.getResponseCode();
            logger.info("{} \t Response {}", conn.getURL(), responseCode);

            switch (responseCode) {
                case 401:
                    throw new HttpException(conn.getURL().getPath());
                case 500:
                    logger.info("Retrying in {} sec...", time);
                    Thread.sleep(time * 1000);
                    conn.disconnect();
                    return perform(i - 1, time * 2);
            }
            String encoding = conn.getContentEncoding();
            // allow both GZip and Deflate (ZLib) encodings
            InputStream inStr = null;

            // create the appropriate stream wrapper based on
            // the encoding type

            if (responseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
                if (encoding != null && encoding.equalsIgnoreCase("gzip")) {
                    inStr = new GZIPInputStream(conn.getInputStream());
                } else if (encoding != null && encoding.equalsIgnoreCase("deflate")) {
                    inStr = new InflaterInputStream(conn.getInputStream(),
                            new Inflater(true));
                } else {
                    inStr = conn.getInputStream();
                }
            } else {
                /* error from server */
                inStr = conn.getErrorStream();
            }

            InputStreamReader isr = new InputStreamReader(inStr);
            int numCharsRead;
            char[] charArray = new char[1024];
            StringBuilder sb = new StringBuilder();
            while ((numCharsRead = isr.read(charArray)) > 0) {
                sb.append(charArray, 0, numCharsRead);
            }
            String result = sb.toString();
            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new HttpException("Response code is " + responseCode + " message: " + result);
            }
            return result;

        } catch (MalformedURLException | URISyntaxException e) {
            throw new HttpException("Can't find path: " + path);
        } catch (IOException e) {
            throw new HttpException("Server is offline: " + path + " " + e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new HttpException("InterruptedException", e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
}
