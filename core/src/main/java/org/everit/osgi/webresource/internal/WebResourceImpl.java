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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPOutputStream;

import javax.annotation.Generated;

import org.everit.osgi.webresource.ContentEncoding;
import org.everit.osgi.webresource.WebResource;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * Implementation of {@link WebResource} interface.
 */
public class WebResourceImpl implements WebResource {

  private static final int COPY_BUFFER_SIZE = 1024;

  private final Bundle bundle;

  private final Map<ContentEncoding, byte[]> cache =
      new ConcurrentHashMap<ContentEncoding, byte[]>();

  private final String contentType;

  private final String etag;

  private final String fileName;

  private final long lastModified;

  private final String library;

  private final int rawLength;

  private final URL resourceURL;

  private final Version version;

  /**
   * Constructor.
   *
   * @param bundle
   *          The bundle that holds the {@link WebResource}.
   * @param library
   *          The library name of the {@link WebResource}.
   * @param fileName
   *          The name of the {@link WebResource}.
   * @param resourceURL
   *          The {@link URL} where the raw content of the {@link WebResource} is available.
   * @param version
   *          Version of the {@link WebResource}.
   * @param contentType
   *          The content type of the {@link WebResource}.
   */
  public WebResourceImpl(final Bundle bundle, final String library, final String fileName,
      final URL resourceURL,
      final Version version, final String contentType) {
    this.resourceURL = resourceURL;
    this.bundle = bundle;
    this.contentType = contentType;
    try {
      URLConnection urlConnection = resourceURL.openConnection();
      lastModified = urlConnection.getLastModified();
      rawLength = urlConnection.getContentLength();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    this.fileName = fileName;
    this.version = version;
    this.library = library;
    etag = resolveETag();
  }

  public void destroy() {
    cache.clear();
  }

  @Override
  @Generated("eclipse")
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    WebResourceImpl other = (WebResourceImpl) obj;
    if (bundle == null) {
      if (other.bundle != null) {
        return false;
      }
    } else if (!bundle.equals(other.bundle)) {
      return false;
    }
    if (fileName == null) {
      if (other.fileName != null) {
        return false;
      }
    } else if (!fileName.equals(other.fileName)) {
      return false;
    }
    if (library == null) {
      if (other.library != null) {
        return false;
      }
    } else if (!library.equals(other.library)) {
      return false;
    }
    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }
    return true;
  }

  @Override
  public Bundle getBundle() {
    return bundle;
  }

  @Override
  public Map<ContentEncoding, Integer> getCacheState() {
    Map<ContentEncoding, Integer> result = new HashMap<ContentEncoding, Integer>();
    ContentEncoding[] contentEncodings = ContentEncoding.values();
    for (ContentEncoding contentEncoding : contentEncodings) {
      byte[] cachedData = cache.get(contentEncoding);
      if (cachedData != null) {
        result.put(contentEncoding, cachedData.length);
      }
    }
    return result;
  }

  private byte[] getContentData(final ContentEncoding contentEncoding) {
    byte[] contentData = cache.get(contentEncoding);
    if (contentData == null) {
      contentData = readContentIntoCache(contentEncoding);
    }
    return contentData;
  }

  @Override
  public long getContentLength(final ContentEncoding contentEncoding) {
    return getContentData(contentEncoding).length;
  }

  @Override
  public String getContentType() {
    return contentType;
  }

  @Override
  public String getETag() {
    return etag;
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public InputStream getInputStream(final ContentEncoding contentEncoding, final int beginIndex)
      throws IOException {
    byte[] contentData = getContentData(contentEncoding);
    ByteArrayInputStream bin = new ByteArrayInputStream(contentData, beginIndex, contentData.length
        - beginIndex);
    return bin;
  }

  @Override
  public long getLastModified() {
    return lastModified;
  }

  @Override
  public String getLibrary() {
    return library;
  }

  public int getRawLength() {
    return rawLength;
  }

  @Override
  public Version getVersion() {
    return version;
  }

  @Override
  @Generated("eclipse")
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = (prime * result) + ((bundle == null) ? 0 : bundle.hashCode());
    result = (prime * result) + ((fileName == null) ? 0 : fileName.hashCode());
    result = (prime * result) + ((library == null) ? 0 : library.hashCode());
    result = (prime * result) + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  private byte[] longToBytes(final long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }

  private synchronized byte[] readContentIntoCache(final ContentEncoding contentEncoding) {
    byte[] contentData = cache.get(contentEncoding);
    if (contentData == null) {
      try (InputStream inputStream = resourceURL.openStream();) {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        OutputStream out;

        // TODO store the deflate and gzip compressed format in the way that the common parts are
        // not calculated and stored twice (as the compression part is the same, only the header and
        // tail is different).
        if (ContentEncoding.GZIP.equals(contentEncoding)) {
          out = new GZIPOutputStream(bout);
        } else if (ContentEncoding.DEFLATE.equals(contentEncoding)) {
          out = new DeflaterOutputStream(bout);
        } else {
          out = bout;
        }
        byte[] buf = new byte[COPY_BUFFER_SIZE];
        int r = inputStream.read(buf);
        while (r > -1) {
          out.write(buf, 0, r);
          r = inputStream.read(buf);
        }
        out.close();
        contentData = bout.toByteArray();
        cache.put(contentEncoding, contentData);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return contentData;
  }

  private String resolveETag() {
    try (InputStream in = resourceURL.openStream()) {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

      Charset defaultCharset = Charset.forName("UTF8");
      messageDigest.update(library.getBytes(defaultCharset));
      messageDigest.update(fileName.getBytes(defaultCharset));
      messageDigest.update(version.toString().getBytes(defaultCharset));
      byte[] buf = new byte[COPY_BUFFER_SIZE];
      int r = in.read(buf);
      while (r > -1) {
        messageDigest.update(buf, 0, r);
        r = in.read(buf);
      }
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      bout.write(messageDigest.digest());
      bout.write(longToBytes(lastModified));
      return String.format("%x", new BigInteger(1, bout.toByteArray()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }
}
