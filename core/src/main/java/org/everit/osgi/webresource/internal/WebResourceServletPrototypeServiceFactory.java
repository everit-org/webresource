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

import javax.servlet.Servlet;

import org.everit.osgi.webresource.WebResourceContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.PrototypeServiceFactory;
import org.osgi.framework.ServiceRegistration;

/**
 * {@link PrototypeServiceFactory} for {@link WebResourceServlet}.
 */
public class WebResourceServletPrototypeServiceFactory implements PrototypeServiceFactory<Servlet> {

  private final WebResourceContainer webResourceContainer;

  public WebResourceServletPrototypeServiceFactory(
      final WebResourceContainer webResourceContainer) {

    this.webResourceContainer = webResourceContainer;
  }

  @Override
  public Servlet getService(final Bundle bundle, final ServiceRegistration<Servlet> registration) {
    return new WebResourceServlet(this.webResourceContainer);
  }

  @Override
  public void ungetService(final Bundle bundle, final ServiceRegistration<Servlet> registration,
      final Servlet service) {
    // Do nothing as the lifecycle of the servlet is handled in its init and destroy method.
  }

}
