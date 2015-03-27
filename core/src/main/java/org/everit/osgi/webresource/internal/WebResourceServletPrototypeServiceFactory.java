package org.everit.osgi.webresource.internal;

import javax.servlet.Servlet;

import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link PrototypeServiceFactory} for {@link WebResourceServlet}.
 */
public class WebResourceServletPrototypeServiceFactory implements PrototypeServiceFactory<Servlet> {

  private final WebResourceUtil webResourceUtil;

  public WebResourceServletPrototypeServiceFactory(final WebResourceUtil webResourceUtil) {
    this.webResourceUtil = webResourceUtil;
  }

  @Override
  public Servlet getService(final Bundle bundle, final ServiceRegistration<Servlet> registration) {
    return new WebResourceServlet(webResourceUtil);
  }

  @Override
  public void ungetService(final Bundle bundle, final ServiceRegistration<Servlet> registration,
      final Servlet service) {
    // Do nothing as the lifecycle of the servlet is handled in its init and destroy method.
  }

}
