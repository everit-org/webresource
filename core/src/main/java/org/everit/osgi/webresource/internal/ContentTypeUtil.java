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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

import org.everit.osgi.webresource.util.WebResourceUtil;

/**
 * Simple utility to derive the content type of a resource based on its extension and the built-in
 * properties file of the module.
 */
public final class ContentTypeUtil {

  private static final Properties DEFAULT_CONTENT_TYPES;

  private static final String UNKNOWN_CONTENT_TYPE = "application/octet-stream";

  static {
    DEFAULT_CONTENT_TYPES = new Properties();
    try (InputStream inputStream = WebResourceUtil.class
        .getResourceAsStream("/META-INF/default-content-types.properties")) {
      ContentTypeUtil.DEFAULT_CONTENT_TYPES.load(inputStream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Resolve the content type of the file that is available on the URL.
   *
   * @param url
   *          The URL of the file.
   * @return The content type of the file if known, otherwise {@value #UNKNOWN_CONTENT_TYPE}.
   */
  public static String resolveContentType(final URL url) {
    String extension = url.toExternalForm();
    int lastIndexOfSlash = extension.lastIndexOf('/');

    if (lastIndexOfSlash > 0) {
      if (lastIndexOfSlash < extension.length() - 1) {
        extension = extension.substring(lastIndexOfSlash + 1);
      } else {
        return ContentTypeUtil.UNKNOWN_CONTENT_TYPE;
      }
    }

    int indexOfExtensionSeparator = extension.indexOf('.');
    String contentType = null;
    while (indexOfExtensionSeparator >= 0 && contentType == null) {
      if (indexOfExtensionSeparator == extension.length() - 1) {
        contentType = ContentTypeUtil.UNKNOWN_CONTENT_TYPE;
      } else {
        extension = extension.substring(indexOfExtensionSeparator + 1);
        contentType = ContentTypeUtil.DEFAULT_CONTENT_TYPES.getProperty(extension);
        if (contentType == null) {
          indexOfExtensionSeparator = extension.indexOf('.');
        }
      }
    }

    if (contentType == null) {
      return ContentTypeUtil.UNKNOWN_CONTENT_TYPE;
    } else {
      return contentType;
    }
  }

  private ContentTypeUtil() {
  }
}
