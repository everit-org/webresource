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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.felix.utils.version.VersionRange;
import org.everit.osgi.webresource.WebResource;
import org.everit.osgi.webresource.WebResourceContainer;
import org.osgi.framework.Bundle;

/**
 * The container that manages all the libraries and {@link WebResource}s.
 */
public class WebResourceContainerImpl implements WebResourceContainer {

  private final Map<String, LibContainer> libContainersByName = new ConcurrentSkipListMap<>();

  private final Map<Bundle, Set<WebResource>> webResourcesByBundles = new ConcurrentHashMap<>();

  /**
   * Adds a new {@link WebResource} to the container.
   *
   * @param webResource
   *          The {@link WebResource}.
   */
  public synchronized void addWebResource(final WebResourceImpl webResource) {
    Bundle bundle = webResource.getBundle();
    Set<WebResource> resources = webResourcesByBundles.get(bundle);
    if (resources == null) {
      resources = Collections.newSetFromMap(new ConcurrentHashMap<WebResource, Boolean>());
      webResourcesByBundles.put(bundle, resources);
    }
    resources.add(webResource);

    String library = webResource.getLibrary();
    LibContainer libContainer = libContainersByName.get(library);
    if (libContainer == null) {
      libContainer = new LibContainer();
      libContainersByName.put(library, libContainer);
    }
    libContainer.addWebResource(webResource);
  }

  @Override
  public Optional<WebResource> findWebResource(final String lib, final String resourceName,
      final String version) {
    Objects.requireNonNull(lib, "WebResource library must not be null");
    Objects.requireNonNull(lib, "WebResource name must not be null");

    LibContainer libContainer = libContainersByName.get(lib);
    if (libContainer == null) {
      return Optional.empty();
    }

    VersionRange versionRange = VersionRange.parseVersionRange(version);
    return libContainer.findWebResource(resourceName, versionRange);

  }

  Map<String, LibContainer> getLibContainersByName() {
    return libContainersByName;
  }

  /**
   * Removing all of the {@link WebResource}s that belong to a specific {@link Bundle} from the
   * container.
   *
   * @param bundle
   *          The {@link Bundle} whose {@link WebResource}s should be removed from the container.
   */
  public synchronized void removeBundle(final Bundle bundle) {
    Set<WebResource> webResources = webResourcesByBundles.remove(bundle);
    for (WebResource webResource : webResources) {
      String library = webResource.getLibrary();
      LibContainer libContainer = libContainersByName.get(library);
      libContainer.removeWebResource(webResource);
      if (libContainer.isEmpty()) {
        libContainersByName.remove(library);
      }
    }
  }
}
