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
package org.everit.osgi.webresource.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.osgi.webresource.ContentEncoding;
import org.everit.osgi.webresource.WebResource;
import org.everit.osgi.webresource.WebResourceConstants;
import org.everit.osgi.webresource.WebResourceContainer;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 * Internal class that holds a {@link WebResourceContainer} and give utility methods to process
 * {@link WebResource} requests.
 */
public class WebResourceUtil {

  /**
   * Asynchronous {@link WriteListener} that writes an {@link InputStream} to the OutputStream of
   * the response.
   */
  private static final class InputStreamBasedWriteListener implements WriteListener {

    private final AsyncContext async;

    byte[] buf = new byte[BUFFER_SIZE];

    private final InputStream in;

    private InputStreamBasedWriteListener(final AsyncContext async, final InputStream in) {
      this.in = in;
      this.async = async;
    }

    @Override
    public void onError(final Throwable t) {
      ServletContext servletContext = async.getRequest().getServletContext();
      if (servletContext != null) {
        servletContext.log("Async exception", t);
      } else {
        t.printStackTrace(System.err);
      }
      async.complete();
    }

    @Override
    public void onWritePossible() throws IOException {
      ServletOutputStream out = async.getResponse().getOutputStream();

      while (out.isReady()) {
        int r = in.read(buf);
        if (r < 0) {
          async.complete();
          return;
        }
        out.write(buf, 0, r);
      }
    }
  }

  private static final int BUFFER_SIZE = 1024;

  private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat
      .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
      .withLocale(Locale.US).withZone(DateTimeZone.forID("GMT"));

  private static final Properties DEFAULT_CONTENT_TYPES;

  private static final int HTTP_NOT_FOUND = 404;

  private static final int HTTP_NOT_MODIFIED = 304;

  private static final String UNKNOWN_CONTENT_TYPE = "application/octet-stream";

  static {
    DEFAULT_CONTENT_TYPES = new Properties();
    try (InputStream inputStream = WebResourceUtil.class
        .getResourceAsStream("/META-INF/default-content-types.properties")) {
      DEFAULT_CONTENT_TYPES.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final WebResourceContainer resourceContainer;

  public WebResourceUtil(final WebResourceContainer resourceContainer) {
    this.resourceContainer = resourceContainer;
  }

  private boolean etagMatchFound(final HttpServletRequest request, final WebResource webResource) {
    String ifNoneMatchHeader = request.getHeader("If-None-Match");
    if (ifNoneMatchHeader == null) {
      return false;
    }
    String[] etags = ifNoneMatchHeader.split(",");
    int i = 0;
    int n = etags.length;
    boolean matchFound = false;
    while (!matchFound && (i < n)) {
      String etag = etags[i].trim();
      if (etag.equals('"' + webResource.getETag() + '"')) {
        matchFound = true;
      } else {
        i++;
      }
    }
    return matchFound;

  }

  /**
   * Finds the {@link WebResource} based on the pathInfo and writes it to the output stream of the
   * response. If the {@link WebResource} is not found, HTTP 404 is thrown.
   *
   * @param req
   *          The Servlet request.
   * @param resp
   *          The Servlet Response which headers will be set and if the request is a GET request,
   *          the {@link WebResource} will be written to the OutputStream of the response.
   *
   * @param pathInfo
   *          The pathInfo that should be parsed to find the {@link WebResource}.
   * @throws IOException
   *           if the content of the {@link WebResource} cannot be written to the output stream.
   */
  public void findWebResourceAndWriteResponse(final HttpServletRequest req,
      final HttpServletResponse resp, final String pathInfo) throws ServletException, IOException {
    int lastIndexOfSlash = pathInfo.lastIndexOf('/');

    if (lastIndexOfSlash == (pathInfo.length() - 1)) {
      http404(resp);
      return;
    }

    String resourceName = pathInfo.substring(lastIndexOfSlash + 1);

    String lib = "";
    if (lastIndexOfSlash > 0) {
      lib = pathInfo.substring(1, lastIndexOfSlash);
    }

    String version = req.getParameter(WebResourceConstants.REQUEST_PARAM_VERSION_RANGE);

    Optional<WebResource> optionalWebResource = resourceContainer.findWebResource(lib,
        resourceName, version);

    if (!optionalWebResource.isPresent()) {
      http404(resp);
      return;
    }

    writeWebResourceToResponse(req, resp, optionalWebResource);
  }

  private void http404(final HttpServletResponse resp) throws IOException {
    resp.sendError(HTTP_NOT_FOUND, "Resource cannot found");
  }

  /**
   * Resolve the content type of the file that is available on the URL.
   *
   * @param url
   *          The URL of the file.
   * @return The content type of the file if known, otherwise {@value #UNKNOWN_CONTENT_TYPE}.
   */
  public String resolveContentType(final URL url) {
    String extension = url.toExternalForm();
    int lastIndexOfSlash = extension.lastIndexOf('/');

    if (lastIndexOfSlash > 0) {
      if (lastIndexOfSlash < (extension.length() - 1)) {
        extension = extension.substring(lastIndexOfSlash + 1);
      } else {
        return UNKNOWN_CONTENT_TYPE;
      }
    }

    int indexOfExtensionSeparator = extension.indexOf('.');
    String contentType = null;
    while ((indexOfExtensionSeparator >= 0) && (contentType == null)) {
      if (indexOfExtensionSeparator == (extension.length() - 1)) {
        contentType = UNKNOWN_CONTENT_TYPE;
      } else {
        extension = extension.substring(indexOfExtensionSeparator + 1);
        contentType = DEFAULT_CONTENT_TYPES.getProperty(extension);
        if (contentType == null) {
          indexOfExtensionSeparator = extension.indexOf('.');
        }
      }
    }

    if (contentType == null) {
      return UNKNOWN_CONTENT_TYPE;
    } else {
      return contentType;
    }
  }

  private ContentEncoding writeResponseHead(final HttpServletRequest req,
      final HttpServletResponse resp,
      final WebResource webResource) {
    resp.setContentType(webResource.getContentType());
    resp.setHeader("Last-Modified", DATE_TIME_FORMATTER.print(webResource.getLastModified()));
    resp.setHeader("ETag", "\"" + webResource.getETag() + "\"");

    ContentEncoding contentEncoding = ContentEncoding.resolveEncoding(req);
    resp.setContentLength((int) webResource.getContentLength(contentEncoding));

    if (!ContentEncoding.RAW.equals(contentEncoding)) {
      resp.setHeader("Content-Encoding", contentEncoding.getHeaderValue());
    }
    return contentEncoding;
  }

  private void writeToOutputStreamFromInputStream(final InputStream in,
      final ServletOutputStream out) throws IOException {

    byte[] buffer = new byte[BUFFER_SIZE];
    int r = in.read(buffer);
    while (r >= 0) {
      out.write(buffer, 0, r);
      r = in.read(buffer);
    }

  }

  private void writeWebResourceToResponse(final HttpServletRequest req,
      final HttpServletResponse resp, final Optional<WebResource> optionalwebResource)
      throws IOException {

    WebResource webResource = optionalwebResource.get();
    ContentEncoding contentEncoding = writeResponseHead(req, resp, webResource);

    if (etagMatchFound(req, webResource)) {
      resp.setStatus(HTTP_NOT_MODIFIED);
      return;
    }

    if ("HEAD".equals(req.getMethod())) {
      return;
    }

    InputStream in = webResource.getInputStream(contentEncoding, 0);
    if (req.isAsyncSupported()) {
      AsyncContext async = req.startAsync();
      ServletOutputStream out = resp.getOutputStream();
      out.setWriteListener(new InputStreamBasedWriteListener(async, in));

      // TODO check if this has to be called due to a Jetty bug
      out.isReady();
    } else {
      ServletOutputStream out = resp.getOutputStream();
      writeToOutputStreamFromInputStream(in, out);
    }
  }
}
