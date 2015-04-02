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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Wraps the original {@link HttpServletRequest} to avoid the webconsole bug that makes it
 * impossible to process async requests. See <a
 * href="https://issues.apache.org/jira/browse/FELIX-4840">FELIX-4840</a> bug report.
 */
public class NoAsyncHttpServletRequest extends HttpServletRequestWrapper {

  public NoAsyncHttpServletRequest(final HttpServletRequest request) {
    super(request);
  }

  @Override
  public boolean isAsyncSupported() {
    return false;
  }

}
