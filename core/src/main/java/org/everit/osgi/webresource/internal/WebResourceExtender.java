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

import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.servlet.Servlet;

import org.everit.osgi.webresource.WebResourceConstants;
import org.everit.osgi.webresource.WebResourceContainer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.framework.Version;
import org.osgi.framework.wiring.BundleCapability;
import org.osgi.framework.wiring.BundleWiring;
import org.osgi.util.tracker.BundleTracker;

/**
 * The extender tracks all of the bundles and process them if they offer the
 * {@link WebResourceConstants#CAPABILITY_NAMESPACE} capability.
 */
public class WebResourceExtender implements BundleActivator {

  /**
   * The {@link BundleTracker} implementation of the extender.
   */
  private class WebResourceBundleTracker extends BundleTracker<Bundle> {

    public WebResourceBundleTracker(final BundleContext context) {
      super(context, Bundle.ACTIVE, null);
    }

    @Override
    public Bundle addingBundle(final Bundle bundle, final BundleEvent event) {
      BundleWiring bundleWiring = bundle.adapt(BundleWiring.class);
      List<BundleCapability> capabilities = bundleWiring
          .getCapabilities(WebResourceConstants.CAPABILITY_NAMESPACE);

      boolean webResourceAdded = false;
      for (BundleCapability capability : capabilities) {
        Map<String, Object> attributes = capability.getAttributes();
        String libraryPrefix = resolveNormalizedLibraryPrefix(capability, attributes);

        String resourceFolder = (String) attributes
            .get(WebResourceConstants.CAPABILITY_ATTRIBUTE_RESOURCE_FOLDER);
        String versionString = (String) attributes
            .get(WebResourceConstants.CAPABILITY_ATTRIBUTE_VERSION);
        Version version = (versionString != null) ? new Version(versionString) : bundle
            .getVersion();

        if (resourceFolder == null) {
          System.err.print("WARNING: Capability attribute "
              + WebResourceConstants.CAPABILITY_ATTRIBUTE_RESOURCE_FOLDER
              + " is missing in bundle " + bundle.toString() + ": " + capability.toString());
        } else {
          Collection<String> entries = bundleWiring.listResources(resourceFolder, "*",
              BundleWiring.LISTRESOURCES_RECURSE);

          ClassLoader classLoader = bundleWiring.getClassLoader();

          for (String entry : entries) {
            if (!entry.endsWith("/")) {
              URL resourceURL = classLoader.getResource(entry);

              String fileName = resolveFileName(resourceURL);
              String library = entry.substring(resourceFolder.length(),
                  entry.length() - fileName.length());
              library = normalizeLibraryName(libraryPrefix, library);

              String contentType = ContentTypeUtil.resolveContentType(resourceURL);
              WebResourceImpl webResource = new WebResourceImpl(bundle, library, fileName,
                  resourceURL,
                  version, contentType);
              resourceContainer.addWebResource(webResource);
              webResourceAdded = true;
            }
          }
        }
      }

      if (webResourceAdded) {
        return bundle;
      } else {
        return null;
      }
    }

    private String normalizeLibraryName(final String libraryPrefix, final String library) {
      String result = library;
      if (result.endsWith("/")) {
        result = result.substring(0, result.length() - 1);
      }
      if (result.startsWith("/")) {
        result = libraryPrefix + result;
      } else {
        result = libraryPrefix + "/" + result;
      }
      if (result.startsWith("/")) {
        result = result.substring(1);
      }
      if (result.endsWith("/")) {
        result = result.substring(0, result.length() - 1);
      }
      return result;
    }

    @Override
    public void removedBundle(final Bundle bundle, final BundleEvent event, final Bundle object) {
      resourceContainer.removeBundle(bundle);
    }

    private String resolveNormalizedLibraryPrefix(final BundleCapability capability,
        final Map<String, Object> attributes) {
      String libraryPrefix = (String) attributes
          .get(WebResourceConstants.CAPABILITY_ATTRIBUTE_LIBRARY_PREFIX);

      if (libraryPrefix == null) {
        libraryPrefix = "";
      } else if (libraryPrefix.endsWith("/")) {
        System.err.print("WARNING: '" + WebResourceConstants.CAPABILITY_ATTRIBUTE_LIBRARY_PREFIX
            + "' attribute of capability "
            + WebResourceConstants.CAPABILITY_NAMESPACE + " should not end with '/' character: "
            + capability.toString());
        libraryPrefix = libraryPrefix.substring(0, libraryPrefix.length() - 1);
      }
      return libraryPrefix;
    }
  }

  private BundleContext bundleContext;

  private ServiceRegistration<Servlet> pluginSR;

  private final WebResourceContainerImpl resourceContainer = new WebResourceContainerImpl();

  private ServiceRegistration<WebResourceContainer> resourceContainerSR;

  private ServiceRegistration<Servlet> servletFactorySR;

  private BundleTracker<Bundle> webResourceTracker;

  @SuppressWarnings("unchecked")
  private void registerServletFactory() {
    WebResourceServletPrototypeServiceFactory webResourceServletFactory =
        new WebResourceServletPrototypeServiceFactory(resourceContainer);

    Dictionary<String, Object> serviceProps = new Hashtable<>();
    serviceProps.put(Constants.SERVICE_DESCRIPTION, "Everit WebResource Servlet");
    serviceProps.put("async-supported", true);

    servletFactorySR = (ServiceRegistration<Servlet>) bundleContext.registerService(
        new String[] { Servlet.class.getName(), WebResourceServlet.class.getName() },
        webResourceServletFactory, serviceProps);
  }

  private void registerWebConsolePlugin() {
    WebResourceWebConsolePlugin webConsolePlugin = new WebResourceWebConsolePlugin(
        resourceContainer, resourceContainer);
    Dictionary<String, Object> serviceProps = new Hashtable<>();
    serviceProps.put("felix.webconsole.label", "everit-webresources");
    serviceProps.put("felix.webconsole.category", "Everit");
    serviceProps.put("felix.webconsole.title", "Webresources");
    serviceProps.put(Constants.SERVICE_DESCRIPTION, "Everit WebResource WebConsole plugin");
    pluginSR = bundleContext.registerService(Servlet.class, webConsolePlugin, serviceProps);
  }

  private void registerWebResourceContainer() {
    Dictionary<String, Object> serviceProps = new Hashtable<>();
    serviceProps.put(Constants.SERVICE_DESCRIPTION, "Everit WebResource Container (read-only)");
    resourceContainerSR = bundleContext
        .registerService(WebResourceContainer.class, resourceContainer, serviceProps);
  }

  private String resolveFileName(final URL resourceURL) {
    String externalForm = resourceURL.toExternalForm();

    int indexOfLastSlash = externalForm.lastIndexOf('/');
    if (indexOfLastSlash >= 0) {
      return externalForm.substring(indexOfLastSlash + 1);
    } else {
      return externalForm;
    }

  }

  @Override
  public void start(final BundleContext context) throws Exception {
    this.bundleContext = context;

    registerWebResourceContainer();

    webResourceTracker = new WebResourceBundleTracker(context);
    webResourceTracker.open();

    registerServletFactory();

    registerWebConsolePlugin();
  }

  @Override
  public void stop(final BundleContext context) throws Exception {
    webResourceTracker.close();
    if (resourceContainerSR != null) {
      resourceContainerSR.unregister();
    }
    if (pluginSR != null) {
      pluginSR.unregister();
    }
    if (servletFactorySR != null) {
      servletFactorySR.unregister();
    }
  }
}
