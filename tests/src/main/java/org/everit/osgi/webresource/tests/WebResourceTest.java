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

import javax.servlet.ServletContext;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.everit.osgi.dev.testrunner.TestRunnerConstants;
import org.everit.osgi.ecm.annotation.Component;
import org.everit.osgi.ecm.annotation.ConfigurationPolicy;
import org.everit.osgi.ecm.annotation.Service;
import org.everit.osgi.ecm.annotation.ServiceRef;
import org.everit.osgi.ecm.annotation.attribute.StringAttribute;
import org.everit.osgi.ecm.annotation.attribute.StringAttributes;
import org.everit.osgi.ecm.extender.ECMExtenderConstants;
import org.everit.osgi.webresource.WebResourceURIGenerator;
import org.everit.osgi.webresource.util.WebResourceUtil;
import org.junit.Assert;
import org.junit.Test;

import aQute.bnd.annotation.headers.ProvideCapability;

/**
 * Test component for WebResource.
 */
@Component(configurationPolicy = ConfigurationPolicy.OPTIONAL)
@ProvideCapability(ns = ECMExtenderConstants.CAPABILITY_NS_COMPONENT,
    value = ECMExtenderConstants.CAPABILITY_ATTR_CLASS + "=${@class}")
@StringAttributes({
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TESTRUNNER_ENGINE_TYPE,
        defaultValue = "junit4"),
    @StringAttribute(attributeId = TestRunnerConstants.SERVICE_PROPERTY_TEST_ID,
        defaultValue = "FormAuthenticationServletTest") })
@Service(value = WebResourceTest.class)
public class WebResourceTest {

  private ServletContext servletContext;

  private WebResourceURIGenerator resolveURIGenerator() {
    Object uriGeneratorAttribute = WebResourceUtil
        .getUriGeneratorsOfServletContext(this.servletContext);

    Assert.assertNotNull(uriGeneratorAttribute);

    @SuppressWarnings("unchecked")
    Queue<WebResourceURIGenerator> queue = (Queue<WebResourceURIGenerator>) uriGeneratorAttribute;

    return queue.peek();
  }

  /**
   * Setter for the {@link #servletContext} retrieved from the Jetty Server.
   */
  @ServiceRef(defaultValue = "")
  public void setServer(final Server server) {

    ContextHandlerCollection contextHandlerCollection =
        (ContextHandlerCollection) server.getHandler();

    Handler[] handlers = contextHandlerCollection.getHandlers();
    for (Handler handler : handlers) {
      if (handler instanceof ServletContextHandler) {
        this.servletContext = ((ServletContextHandler) handler).getServletContext();
        return;
      }
    }
    throw new IllegalArgumentException("Failed to retrieve the ServletContext");
  }

  @Test
  public void testURIGeneratorForExistingWebResource() {
    WebResourceURIGenerator uriGenerator = resolveURIGenerator();
    Optional<String> uri = uriGenerator.generateURI("foo/bar/css", "main.css", Optional.empty());
    Assert.assertTrue(uri.isPresent());
  }

  @Test
  public void testURIGeneratorForNonExistentWebResource() {
    WebResourceURIGenerator uriGenerator = resolveURIGenerator();
    Optional<String> uri = uriGenerator.generateURI("nonexistent", "nonexistent", Optional.empty());
    Assert.assertFalse(uri.isPresent());
  }
}
