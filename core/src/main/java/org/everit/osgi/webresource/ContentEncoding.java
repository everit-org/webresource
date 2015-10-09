/*
 * Copyright (C) 2011 Everit Kft. (http://www.everit.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.everit.osgi.webresource;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Content encodings that are supported by WebResource technology.
 */
public enum ContentEncoding {

  DEFLATE("deflate"),

  GZIP("gzip"),

  RAW("raw");

  /**
   * Resolves the best supported content encoding from the request.
   *
   * @param request
   *          The request that says which content encoding formats are supported by the client.
   * @return The best content encoding that should be applied in the response.
   */
  public static ContentEncoding resolveEncoding(final HttpServletRequest request) {
    String acceptEncodingHeader = request.getHeader("Accept-Encoding");
    if (acceptEncodingHeader == null) {
      return RAW;
    }
    String[] encodings = acceptEncodingHeader.split(",");
    List<String> encodingList = Arrays.asList(encodings);
    if (encodingList.contains(GZIP.getHeaderValue())) {
      return GZIP;
    }
    if (encodingList.contains(DEFLATE.getHeaderValue())) {
      return DEFLATE;
    }
    return RAW;
  }

  private final String headerValue;

  ContentEncoding(final String headerValue) {
    this.headerValue = headerValue;
  }

  public String getHeaderValue() {
    return headerValue;
  }
}
