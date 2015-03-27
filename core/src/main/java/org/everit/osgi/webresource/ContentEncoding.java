package org.everit.osgi.webresource;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

/**
 * Content encodings that are supported by WebResource technology.
 */
public enum ContentEncoding {

  DEFLATE("deflate"), GZIP("gzip"), RAW("raw");

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

  private ContentEncoding(final String headerValue) {
    this.headerValue = headerValue;
  }

  public String getHeaderValue() {
    return headerValue;
  }
}
