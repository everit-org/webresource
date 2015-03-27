package org.everit.osgi.webresource.internal;

import java.util.Collection;
import java.util.Objects;
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

/**
 * A Servlet that can serve {@link org.everit.osgi.webresource.WebResource}s and is registered as an
 * OSGi service. The scope of the OSGi service is prototype. Each time the {@link Servlet} is
 * initialized within a {@link ServletContext}, an attribute of
 * {@link org.everit.osgi.webresource.WebResourceURIResolver} with the key
 * {@value org.everit.osgi.webresource.WebResourceConstants#SERVLET_CONTEXT_ATTR_URI_RESOLVER} is
 * registered.
 */
public class WebResourceServlet implements Servlet {

  private static final int MIN_SERVLET_MAJOR_VERSION = 3;

  private static final int MIN_SERVLET_MINOR_VERSION_WITH_MAJOR_3 = 1;

  private final AtomicInteger initCount = new AtomicInteger();

  private ServletConfig servletConfig;

  private final WebResourceUtil webResourceUtil;

  public WebResourceServlet(final WebResourceUtil webResourceUtil) {
    this.webResourceUtil = webResourceUtil;
  }

  @Override
  public void destroy() {
    if (initCount.decrementAndGet() < 0) {
      initCount.incrementAndGet();
      throw new IllegalStateException(
          "WebResource servlet was destroyed more times than it was initialized");
    }
    // TODO remove the URI resolver from the servlet context attributes.
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
        String servletContextPath = servletContext.getContextPath();
        ServletRegistration servletRegistration = servletContext
            .getServletRegistration(servletName);
        Collection<String> mappings = servletRegistration.getMappings();

        // TODO

        if (servletContext.getMajorVersion() > MIN_SERVLET_MAJOR_VERSION
            || (servletContext.getMajorVersion() == MIN_SERVLET_MAJOR_VERSION && servletContext
                .getMinorVersion() > MIN_SERVLET_MINOR_VERSION_WITH_MAJOR_3)) {

          // TODO
        }
      }
    } catch (RuntimeException e) {
      initCount.decrementAndGet();
      throw e;
    }

  }

  @Override
  public void service(final ServletRequest req, final ServletResponse res) throws ServletException {
    HttpServletRequest httpReq = (HttpServletRequest) req;
    String pathInfo = httpReq.getPathInfo();
    webResourceUtil.findWebResourceAndWriteResponse(httpReq, (HttpServletResponse) res, pathInfo);
  }
}
