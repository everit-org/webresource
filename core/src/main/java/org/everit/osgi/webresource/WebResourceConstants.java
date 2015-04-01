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

/**
 * Constants to help the usage of the webresource library.
 */
public final class WebResourceConstants {

  /**
   * Prefix of the library which the WebResources should be provided with. Each subfolder of
   * {@link #CAPABILITY_ATTRIBUTE_RESOURCE_FOLDER} will be appended to the value of library prefix.
   */
  public static final String CAPABILITY_ATTRIBUTE_LIBRARY_PREFIX = "libraryPrefix";

  /**
   * The folder in the bundle that contains the {@link WebResource}s. The folder will be scanned
   * recursively.
   */
  public static final String CAPABILITY_ATTRIBUTE_RESOURCE_FOLDER = "resourceFolder";

  /**
   * The version of the {@link WebResource}s that are provided by the capability.
   */
  public static final String CAPABILITY_ATTRIBUTE_VERSION = "version";

  /**
   * Namespace of the {@link WebResource} capability.
   */
  public static final String CAPABILITY_NAMESPACE = "everit.webresource";

  /**
   * Name of the unknown mime type.
   */
  public static final String MIME_TYPE_UNKNOWN = "application/octet-stream";

  /**
   * Name of version range parameter on webconsole.
   */
  public static final String REQUEST_PARAM_VERSION_RANGE = "version";

  private WebResourceConstants() {
  }
}
