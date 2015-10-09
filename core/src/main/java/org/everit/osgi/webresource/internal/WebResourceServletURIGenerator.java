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

import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import org.everit.osgi.webresource.WebResource;
import org.everit.osgi.webresource.WebResourceContainer;
import org.everit.osgi.webresource.WebResourceURIGenerator;

/**
 * Simple URI generator for {@link WebResourceServlet}.
 */
public class WebResourceServletURIGenerator implements WebResourceURIGenerator {

  private String pathPrefix;

  private String pathSuffix;

  private ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

  private final WebResourceContainer webResourceContainer;

  /**
   * Constructor.
   *
   * @param webResourceContainer
   *          The container that holds the webresources for this uri generator.
   * @param contextPath
   *          The contextPath of that the {@link WebResourceServlet} was initialized with.
   * @param urlPattern
   *          The url pattern that the {@link WebResourceServlet} was initialized with.
   */
  public WebResourceServletURIGenerator(final WebResourceContainer webResourceContainer,
      final String contextPath,
      final String urlPattern) {
    this.webResourceContainer = webResourceContainer;
    update(contextPath, urlPattern);
  }

  @Override
  public Optional<String> generateURI(final String lib, final String file,
      final Optional<String> versionRange) {

    Optional<WebResource> webResource = webResourceContainer.findWebResource(lib, file,
        versionRange);

    if (!webResource.isPresent()) {
      return Optional.empty();
    }

    ReadLock readLock = rwLock.readLock();
    readLock.lock();
    String lPathPrefix;
    String lPathSuffix;
    try {
      lPathPrefix = pathPrefix;
      lPathSuffix = pathSuffix;
    } finally {
      readLock.unlock();
    }

    StringBuilder sb = new StringBuilder(lPathPrefix).append("/");
    if ((lib != null) && (lib.length() > 0)) {
      sb.append(lib).append("/");
    }
    sb.append(file);

    sb.append(lPathSuffix);

    char parameterSeparator = '?';
    if (versionRange.isPresent() && (versionRange.get().length() > 0)) {
      parameterSeparator = '&';
      sb.append(parameterSeparator).append("version=").append(versionRange);
    }

    sb.append(parameterSeparator).append("t=").append(webResource.get().getLastModified());

    return Optional.of(sb.toString());
  }

  /**
   * Updates the uri resolver with the new context path and url pattern.
   *
   * @param contextPath
   *          The contextPath of that the {@link WebResourceServlet} was initialized with.
   * @param urlPattern
   *          The url pattern that the {@link WebResourceServlet} was initialized with.
   */
  void update(final String contextPath, final String urlPattern) {
    String pathPrefixResult = contextPath;

    if (pathPrefixResult.endsWith("/")) {
      pathPrefixResult = pathPrefixResult.substring(0, pathPrefixResult.length() - 1);
    }

    String pathSuffixResult = "";

    if (urlPattern.startsWith("*")) {
      pathSuffixResult = urlPattern.substring(1);
    } else {
      String patternForPrefix = urlPattern;
      if (patternForPrefix.endsWith("*")) {
        patternForPrefix = urlPattern.substring(0, urlPattern.length() - 1);
      }
      if (patternForPrefix.startsWith("/")) {
        patternForPrefix = patternForPrefix.substring(1);
      }
      if (patternForPrefix.endsWith("/")) {
        patternForPrefix = patternForPrefix.substring(0, patternForPrefix.length() - 1);
      }
      if (patternForPrefix.length() > 0) {
        pathPrefixResult += "/" + patternForPrefix;
      }
    }
    WriteLock writeLock = rwLock.writeLock();
    writeLock.lock();
    try {
      pathPrefix = pathPrefixResult;
      pathSuffix = pathSuffixResult;
    } finally {
      writeLock.unlock();
    }
  }
}
