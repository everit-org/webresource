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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.apache.felix.utils.version.VersionRange;
import org.everit.osgi.webresource.WebResource;
import org.osgi.framework.Version;

/**
 * Internal container class that holds the currently managed {@link WebResource}s for a library.
 */
public class LibContainer {

  private final Map<String, NavigableMap<Version, Set<WebResourceImpl>>> versionedResourcesByName =
      new ConcurrentSkipListMap<>();

  /**
   * Adding a new {@link WebResource} to the container..
   *
   * @param resource
   *          The {@link WebResource}.
   */
  public synchronized void addWebResource(final WebResourceImpl resource) {
    String fileName = resource.getFileName();
    NavigableMap<Version, Set<WebResourceImpl>> resourcesByVersion = versionedResourcesByName
        .get(fileName);
    if (resourcesByVersion == null) {
      resourcesByVersion = new ConcurrentSkipListMap<>();
      versionedResourcesByName.put(fileName, resourcesByVersion);
    }
    Version version = resource.getVersion();
    Set<WebResourceImpl> resources = resourcesByVersion.get(version);
    if (resources == null) {
      resources = Collections.newSetFromMap(new ConcurrentHashMap<WebResourceImpl, Boolean>());
      resourcesByVersion.put(version, resources);
    }
    resources.add(resource);

  }

  /**
   * Finds a WebResource within the Library.
   *
   * @param resourceName
   *          The name of the {@link WebResource}.
   * @param versionRange
   *          The version range in which the {@link WebResource}s should be searched.
   * @return The found {@link WebResource} if available.
   */
  public Optional<WebResource> findWebResource(final String resourceName,
      final VersionRange versionRange) {
    NavigableMap<Version, Set<WebResourceImpl>> resourceByVersion = versionedResourcesByName
        .get(resourceName);
    if ((resourceByVersion == null) || (resourceByVersion.size() == 0)) {
      // There is no resource by the name
      return Optional.empty();
    }
    if ((versionRange == null) || versionRange.getCeiling().equals(VersionRange.INFINITE_VERSION)) {
      // Selecting the highest version of the resource
      Optional<WebResource> optionalWebResource =
          selectResourceWithHighestVersion(resourceByVersion);

      if (optionalWebResource.isPresent()) {
        WebResource webResource = optionalWebResource.get();
        Version version = webResource.getVersion();
        if (versionRange.contains(version)) {
          return Optional.of(webResource);
        } else {
          return Optional.empty();
        }
      } else {
        return Optional.empty();
      }
    }

    if (versionRange.isPointVersion()) {
      // Selecting an exact version of resource. Normally comes with expression [x, x] where x is
      // the same.
      Set<WebResourceImpl> resources = resourceByVersion.get(versionRange.getFloor());
      return selectResourceFromSet(resources);
    }

    Optional<WebResource> result = null;
    Version ceilingVersion = versionRange.getCeiling();

    Entry<Version, Set<WebResourceImpl>> potentialEntry =
        resolvePotentialEntriesByVersionRange(versionRange, resourceByVersion, ceilingVersion);

    if ((potentialEntry != null) && versionRange.contains(potentialEntry.getKey())) {
      result = selectResourceFromSet(potentialEntry.getValue());
    }

    return result;
  }

  Map<String, NavigableMap<Version, Set<WebResourceImpl>>> getVersionedResourcesByName() {
    return versionedResourcesByName;
  }

  public boolean isEmpty() {
    return versionedResourcesByName.size() == 0;
  }

  /**
   * Removes a {@link WebResource} from the library container.
   *
   * @param resource
   *          The {@link WebResource} that will be removed.
   */
  public synchronized void removeWebResource(final WebResource resource) {
    String fileName = resource.getFileName();
    NavigableMap<Version, Set<WebResourceImpl>> resourcesByVersion = versionedResourcesByName
        .get(fileName);
    Version version = resource.getVersion();
    Set<WebResourceImpl> resources = resourcesByVersion.get(version);
    resources.remove(resource);
    if (resources.size() == 0) {
      resourcesByVersion.remove(version);
      if (resourcesByVersion.size() == 0) {
        versionedResourcesByName.remove(fileName);
      }
    }
  }

  private Entry<Version, Set<WebResourceImpl>> resolvePotentialEntriesByVersionRange(
      final VersionRange versionRange,
      final NavigableMap<Version, Set<WebResourceImpl>> resourceByVersion,
      final Version ceilingVersion) {
    Entry<Version, Set<WebResourceImpl>> potentialEntry = null;
    if (!versionRange.isOpenCeiling()) {
      potentialEntry = resourceByVersion.floorEntry(ceilingVersion);
    } else {
      potentialEntry = resourceByVersion.lowerEntry(ceilingVersion);
    }
    return potentialEntry;
  }

  private Optional<WebResource> selectResourceFromSet(final Set<WebResourceImpl> resources) {
    if (resources == null) {
      return Optional.empty();
    }
    // TODO what if the set gets empty in the moment where the iterator is requested.
    Iterator<WebResourceImpl> iterator = resources.iterator();
    if (iterator.hasNext()) {
      return Optional.of(iterator.next());
    } else {
      return Optional.empty();
    }
  }

  private Optional<WebResource> selectResourceWithHighestVersion(
      final NavigableMap<Version, Set<WebResourceImpl>> resourceByVersion) {
    Entry<Version, Set<WebResourceImpl>> lastEntry = resourceByVersion.lastEntry();
    if (lastEntry != null) {
      return selectResourceFromSet(lastEntry.getValue());
    } else {
      // This could happen if the resource is removed on a parallel thread after the size is
      // checked.
      return Optional.empty();
    }
  }
}
