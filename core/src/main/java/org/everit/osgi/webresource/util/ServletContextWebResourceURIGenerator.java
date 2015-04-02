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
package org.everit.osgi.webresource.util;

import java.util.Iterator;
import java.util.Optional;
import java.util.Queue;

import javax.servlet.ServletContext;

import org.everit.osgi.webresource.WebResourceURIGenerator;

/**
 * Implementation of {@link WebResourceURIGenerator} that expects having one or more
 * {@link WebResourceURIGenerator} instances packed into a {@link Queue}, registered as
 * {@link ServletContext} attribute. The key of the attribute is the full name of
 * {@link WebResourceURIGenerator} interface.
 */
public class ServletContextWebResourceURIGenerator implements WebResourceURIGenerator {

  private final ServletContext context;

  private Queue<WebResourceURIGenerator> uriGeneratorQueue;

  public ServletContextWebResourceURIGenerator(final ServletContext context) {
    this.context = context;
  }

  @Override
  public Optional<String> generateURI(final String lib, final String file,
      final String versionRange, final boolean appendLastModifiedParameter) {
    Queue<WebResourceURIGenerator> lUriGeneratorQueue = getUriGeneratorQueue();

    if (lUriGeneratorQueue == null) {
      return Optional.empty();
    }

    String result = null;
    Iterator<WebResourceURIGenerator> iterator = lUriGeneratorQueue.iterator();
    while (result == null && iterator.hasNext()) {
      Object nextItemObject = iterator.next();
      if (nextItemObject instanceof WebResourceURIGenerator) {
        // It might happen that WebResourceURIGenerator from different version of the API are placed
        // into the ServletContext so we must check
        WebResourceURIGenerator webResourceURIGenerator = (WebResourceURIGenerator) nextItemObject;
        result = webResourceURIGenerator.generateURI(lib, file, versionRange,
            appendLastModifiedParameter).orElse(null);
      }
    }
    return Optional.ofNullable(result);
  }

  @SuppressWarnings("unchecked")
  private Queue<WebResourceURIGenerator> getUriGeneratorQueue() {
    if (uriGeneratorQueue == null) {
      uriGeneratorQueue = (Queue<WebResourceURIGenerator>) context
          .getAttribute(WebResourceURIGenerator.class.getName());
    }
    return uriGeneratorQueue;
  }

}