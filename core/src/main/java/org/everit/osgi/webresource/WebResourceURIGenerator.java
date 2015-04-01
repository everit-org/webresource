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
 * Standard interface to resolve URI for WebResources.
 */
public interface WebResourceURIGenerator {

  /**
   * Resolves the URI of a {@link WebResource} that can be inserted into an output (HTML, e-mail,
   * etc).
   *
   * @param lib
   *          The library of the {@link WebResource}.
   * @param file
   *          The file name of the {@link WebResource}.
   * @param version
   *          The version. Range expression is accepted.
   * @return The full path of the web resource. In case the webResource is not available, an empty
   *         optional will be returned.
   */
  Optional<String> generateURI(String lib, String file, String versionRange,
      boolean appendLastModifiedParameter);

}
