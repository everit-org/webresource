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
 * Standard interface to generate URI for WebResources that can be used on websites, e-mails, etc.
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
   * @param versionRange
   *          The version range expression that must the {@link WebResource#getVersion()} match or
   *          {@link Optional#empty()} if any version is accepted.
   * @return The URI that can be used to access the {@link WebResource} or {@link Optional#empty()}
   *         if the URI cannot be resolved.
   */
  Optional<String> generateURI(String lib, String file, Optional<String> versionRange);

}
