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
   *          A version range to identify the web resource or {@link Optional#empty()} to accept any
   *          version.
   * @return The web resource if found.
   * @throws NullPointerException
   *           if lib or resourceName is null.
   * @throws IllegalArgumentException
   *           if the version range is not in the expected format.
   */
  Optional<WebResource> findWebResource(String lib, String resourceName,
      Optional<String> versionRange);

}
