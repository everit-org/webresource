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
package org.everit.osgi.webresource.internal;

import java.io.IOException;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.osgi.webresource.WebResourceContainer;
import org.everit.osgi.webresource.WebResourceURIGenerator;
import org.everit.osgi.webresource.util.WebResourceUtil;

/**
 * A Servlet that can serve {@link org.everit.osgi.webresource.WebResource}s and is registered as an
 * OSGi service. The scope of the OSGi service is prototype. Each time the {@link Servlet} is
 * initialized within a {@link ServletContext}, an attribute of
 * {@link org.everit.osgi.webresource.WebResourceURIGenerator} with the key
 * {@value org.everit.osgi.webresource.WebResourceConstants#SERVLET_CONTEXT_ATTR_URI_RESOLVER} is
 * registered.
 */
public class WebResourceServlet implements Servlet {

  private final AtomicInteger initCount = new AtomicInteger();

  private ServletConfig servletConfig;

  private WebResourceURIGenerator uriGenerator;

  private final WebResourceContainer webResourceContainer;

  public WebResourceServlet(final WebResourceContainer webResourceContainer) {
    this.webResourceContainer = webResourceContainer;
  }

  @Override
  public void destroy() {
    if (initCount.decrementAndGet() < 0) {
      initCount.incrementAndGet();
      throw new IllegalStateException(
          "WebResource servlet was destroyed more times than it was initialized");
    }

    ConcurrentLinkedQueue<WebResourceURIGenerator> uriGenerators = getOrCreateURIGeneratorQueue();
    uriGenerators.remove(uriGenerator);
  }

  private ConcurrentLinkedQueue<WebResourceURIGenerator> getOrCreateURIGeneratorQueue() {
    ServletContext servletContext = getServletConfig().getServletContext();
    String attributeName = WebResourceURIGenerator.class.getName();

    @SuppressWarnings("unchecked")
    ConcurrentLinkedQueue<WebResourceURIGenerator> uriGeneratorQueue =
        (ConcurrentLinkedQueue<WebResourceURIGenerator>) servletContext.getAttribute(attributeName);

    if (uriGeneratorQueue == null) {
      uriGeneratorQueue = new ConcurrentLinkedQueue<>();
      servletContext.setAttribute(attributeName, uriGeneratorQueue);
    }
    return uriGeneratorQueue;
  }

  @Override
  public ServletConfig getServletConfig() {
    return servletConfig;
  }

  @Override
  public String getServletInfo() {
    return "Everit WebResource";
  }

  @Override
  public void init(final ServletConfig config) throws ServletException {
    if (initCount.incrementAndGet() > 1) {
      initCount.decrementAndGet();
      throw new IllegalStateException(
          "WebResource servlet instance cannot be initialized more than once.");
    }

    this.servletConfig = config;
    try {

      String servletName = config.getServletName();
      Objects.requireNonNull(servletName, "Servlet name must not be null!");
      ServletContext servletContext = config.getServletContext();
      if (servletContext != null) {
        String contextPath = servletContext.getContextPath();
        ServletRegistration servletRegistration = servletContext
            .getServletRegistration(servletName);
        Collection<String> mappings = servletRegistration.getMappings();
        if (mappings.size() > 0) {
          String mapping = mappings.iterator().next();
          uriGenerator = new WebResourceServletURIGenerator(webResourceContainer, contextPath,
              mapping);

          ConcurrentLinkedQueue<WebResourceURIGenerator> uriGeneratorQueue =
              getOrCreateURIGeneratorQueue();

          uriGeneratorQueue.add(uriGenerator);
        }
      }
    } catch (RuntimeException e) {
      initCount.decrementAndGet();
      throw e;
    }

  }

  @Override
  public void service(final ServletRequest req, final ServletResponse res) throws ServletException,
      IOException {
    HttpServletRequest httpReq = (HttpServletRequest) req;
    WebResourceUtil.findWebResourceAndWriteResponse(webResourceContainer, httpReq,
        (HttpServletResponse) res);
  }
}
