/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.handler.codec.http;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.UnsupportedCharsetException;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates an URL-encoded URI from a path string and key-value parameter pairs.
 * This encoder is for one time use only.  Create a new instance for each URI.
 *
 * <pre>
 * QueryStringEncoder encoder = new QueryStringDecoder("/hello");
 * encoder.addParam("recipient", "world");
 * assert encoder.toString().equals("/hello?recipient=world");
 * </pre>
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 *
 * @see QueryStringDecoder
 *
 * @apiviz.stereotype utility
 * @apiviz.has        org.jboss.netty.handler.codec.http.HttpRequest oneway - - encodes
 */
public class QueryStringEncoder {

    private final String charset;
    private final String uri;
    private final List<Param> params = new ArrayList<Param>();

    /**
     * Creates a new encoder that encodes a URI that starts with the specified
     * path string.  The encoder will encode the URI in UTF-8.
     */
    public QueryStringEncoder(String uri) {
        this(uri, HttpCodecUtil.DEFAULT_CHARSET);
    }

    /**
     * Creates a new encoder that encodes a URI that starts with the specified
     * path string in the specified charset.
     */
    public QueryStringEncoder(String uri, String charset) {
        if (uri == null) {
            throw new NullPointerException("uri");
        }
        if (charset == null) {
            throw new NullPointerException("charset");
        }

        this.uri = uri;
        this.charset = charset;
    }

    /**
     * Adds a parameter with the specified name and value to this encoder.
     */
    public void addParam(String name, String value) {
        if (name == null) {
            throw new NullPointerException("name");
        }
        if (value == null) {
            throw new NullPointerException("value");
        }
        params.add(new Param(name, value));
    }

    /**
     * Returns the URL-encoded URI object which was created from the path string
     * specified in the constructor and the parameters added by
     * {@link #addParam(String, String)} method.
     */
    public URI toUri() throws URISyntaxException {
        return new URI(toString());
    }

    /**
     * Returns the URL-encoded URI which was created from the path string
     * specified in the constructor and the parameters added by
     * {@link #addParam(String, String)} method.
     */
    @Override
    public String toString() {
        if (params.isEmpty()) {
            return uri;
        } else {
            StringBuilder sb = new StringBuilder(uri).append("?");
            for (int i = 0; i < params.size(); i++) {
                Param param = params.get(i);
                sb.append(encodeComponent(param.name, charset));
                sb.append("=");
                sb.append(encodeComponent(param.value, charset));
                if(i != params.size() - 1) {
                    sb.append("&");
                }
            }
            return sb.toString();
        }
    }

    private static String encodeComponent(String s, String charset) {
        try {
            return URLEncoder.encode(s, charset).replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedCharsetException(charset);
        }
    }

    private static final class Param {

        final String name;
        final String value;

        Param(String name, String value) {
            this.value = value;
            this.name = name;
        }
    }
}
