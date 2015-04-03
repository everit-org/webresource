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
import java.util.Objects;
import java.util.Optional;

import javax.servlet.AsyncContext;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.osgi.webresource.ContentEncoding;
import org.everit.osgi.webresource.WebResource;
import org.everit.osgi.webresource.WebResourceConstants;
import org.everit.osgi.webresource.WebResourceContainer;

/**
 * Internal class that holds a {@link WebResourceContainer} and give utility methods to process
 * {@link WebResource} requests.
 */
public final class WebResourceUtil {

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

  private static final int HTTP_NOT_MODIFIED = 304;

  private static boolean etagMatchFound(final HttpServletRequest request,
      final WebResource webResource) {
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
   * @param webResourceContainer
   *          The container that is used to find the webResource.
   * @param req
   *          The Servlet request.
   * @param resp
   *          The Servlet Response which headers will be set and if the request is a GET request,
   *          the {@link WebResource} will be written to the OutputStream of the response.
   * @throws IOException
   *           if the content of the {@link WebResource} cannot be written to the output stream.
   */
  public static void findWebResourceAndWriteResponse(
      final WebResourceContainer webResourceContainer, final HttpServletRequest req,
      final HttpServletResponse resp) throws IOException {

    String pathInfo = req.getPathInfo();
    if (pathInfo == null) {
      // Happens when the servlet is a default servlet in the servlet context
      pathInfo = req.getServletPath();
    }

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

    Optional<WebResource> optionalWebResource = webResourceContainer.findWebResource(lib,
        resourceName, Optional.ofNullable(version));

    if (!optionalWebResource.isPresent()) {
      http404(resp);
      return;
    }

    writeWebResourceToResponse(optionalWebResource.get(), req, resp);
  }

  private static void http404(final HttpServletResponse resp) throws IOException {
    resp.sendError(WebResourceConstants.HTTP_NOT_FOUND, "Resource cannot found");
  }

  private static ContentEncoding writeResponseHead(final HttpServletRequest req,
      final HttpServletResponse resp,
      final WebResource webResource) {
    resp.setContentType(webResource.getContentType());
    resp.setHeader("Last-Modified", webResource.getLastModifiedRFC1123GMT());
    resp.setHeader("ETag", '"' + webResource.getETag() + '"');

    ContentEncoding contentEncoding = ContentEncoding.resolveEncoding(req);
    resp.setContentLength((int) webResource.getContentLength(contentEncoding));

    if (!ContentEncoding.RAW.equals(contentEncoding)) {
      resp.setHeader("Content-Encoding", contentEncoding.getHeaderValue());
    }
    return contentEncoding;
  }

  private static void writeToOutputStreamFromInputStream(final InputStream in,
      final ServletOutputStream out) throws IOException {

    byte[] buffer = new byte[BUFFER_SIZE];
    int r = in.read(buffer);
    while (r >= 0) {
      out.write(buffer, 0, r);
      r = in.read(buffer);
    }

  }

  /**
   * Writes a {@link WebResource} to the response stream, asynchronously if it is supported.
   *
   * @param webResource
   *          An optional {@link WebResource}. In case the WebResource is not provided, 404 error
   *          code will be sent to the response.
   * @param req
   *          The http request that can tell if async is supported or not. The function also handles
   *          if this is a HEAD request.
   * @param resp
   *          The response that will be used to write the content of the webResource.
   * @throws IOException
   *           If there is an issue during reading the content of the {@link WebResource} or writing
   *           to the response stream.
   * @throws NullPointerException
   *           if any of the parameters is null.
   */
  public static void writeWebResourceToResponse(final WebResource webResource,
      final HttpServletRequest req, final HttpServletResponse resp)
      throws IOException {

    Objects.requireNonNull(req);
    Objects.requireNonNull(resp);
    Objects.requireNonNull(webResource);

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
    } else {
      ServletOutputStream out = resp.getOutputStream();
      writeToOutputStreamFromInputStream(in, out);
    }
  }

  private WebResourceUtil() {
  }
}
