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
package org.everit.osgi.webresource.tests;

import java.util.Optional;
import java.util.Queue;

import javax.servlet.Servlet;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.everit.osgi.dev.testrunner.TestDuringDevelopment;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.webresource.WebResourceURIGenerator;
import org.everit.osgi.webresource.util.WebResourceUtil;
import org.junit.Assert;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

/**
 * Test component for WebResource.
 */
@Component(immediate = true, policy = ConfigurationPolicy.OPTIONAL)
@Properties({
    @Property(name = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE, value = "junit4"),
    @Property(name = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID, value = "WebResourceTest")
})
@Service(value = WebResourceTest.class)
public class WebResourceTest {

  private Server server;

  private ServletContextHandler servletContextHandler;

  @Reference(bind = "setWebResourceServlet", target = "(" + Constants.SERVICE_DESCRIPTION
      + "=Everit WebResource Servlet)")
  private Servlet webResourceServlet;

  /**
   * Activate method of the component that starts a jetty server and registers an instance of
   * WebResourceServlet on it.
   */
  @Activate
  public void activate(final BundleContext context) {
    server = new Server(0);
    ContextHandlerCollection contextCollection = new ContextHandlerCollection();
    server.setHandler(contextCollection);

    servletContextHandler = new ServletContextHandler();
    servletContextHandler.setContextPath("/testcontext");
    contextCollection.addHandler(servletContextHandler);
    servletContextHandler.addServlet(new ServletHolder("testServlet", webResourceServlet), "/*");

    try {
      server.start();
    } catch (Exception e) {
      try {
        server.stop();
      } catch (Exception e1) {
        e.addSuppressed(e1);
      }
      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }
      throw new RuntimeException(e);
    }
  }

  /**
   * Deactivate method of the component that stops the jetty server.
   */
  @Deactivate
  public void deactivate() {
    try {
      server.stop();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private WebResourceURIGenerator resolveURIGenerator() {
    Object uriGeneratorAttribute = WebResourceUtil
        .getUriGeneratorsOfServletContext(servletContextHandler.getServletContext());

    Assert.assertNotNull(uriGeneratorAttribute);

    @SuppressWarnings("unchecked")
    Queue<WebResourceURIGenerator> queue = (Queue<WebResourceURIGenerator>) uriGeneratorAttribute;

    return queue.peek();
  }

  public void setWebResourceServlet(final Servlet webResourceServlet) {
    this.webResourceServlet = webResourceServlet;
  }

  @Test
  public void testURIGeneratorForExistingWebResource() {
    WebResourceURIGenerator uriGenerator = resolveURIGenerator();
    Optional<String> uri = uriGenerator.generateURI("nonexistent", "nonexistent", Optional.empty());
    Assert.assertFalse(uri.isPresent());
  }

  @Test
  @TestDuringDevelopment
  public void testURIGeneratorForNonExistentWebResource() {
    WebResourceURIGenerator uriGenerator = resolveURIGenerator();
    Optional<String> uri = uriGenerator.generateURI("foo/bar/css", "main.css", Optional.empty());
    Assert.assertTrue(uri.isPresent());
  }
}
