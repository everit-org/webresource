package org.everit.osgi.webresource;

import java.util.Optional;

/**
 * Finding {@link WebResource}s is possible via the WebResourceContainer interface that is also
 * registered as an OSGi service.
 */
public interface WebResourceContainer {

  /**
   * Finding a {@link WebResource}.
   *
   * @param lib
   *          Name of the library where the resource is located.
   * @param resourceName
   *          Name of the resource / file.
   * @param versionRange
   *          A version range to identify the web resource exactly.
   * @return The web resource if found.
   * @throws NullPointerException
   *           if lib or resourceName is null.
   * @throws IllegalArgumentException
   *           if the version range is not in the expected format.
   */
  Optional<WebResource> findWebResource(String lib, String resourceName, String versionRange)
      throws IllegalArgumentException;

}
