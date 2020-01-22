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
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
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
      new ConcurrentHashMap<>();

  private final String contentType;

  private final String etag;

  private final String fileName;

  private final long lastModified;

  private final String lastModifiedRFC1123GMT;

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
      this.lastModified = urlConnection.getLastModified();
      this.rawLength = urlConnection.getContentLength();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }

    this.fileName = fileName;
    this.version = version;
    this.library = library;
    this.etag = resolveETag();
    this.lastModifiedRFC1123GMT = resolveLastModifiedRFC1123();
  }

  public void destroy() {
    this.cache.clear();
  }

  // CHECKSTYLE.OFF: CyclomaticComplexity
  // CHECKSTYLE.OFF: NPathComplexity
  @Override
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
    if (this.bundle == null) {
      if (other.bundle != null) {
        return false;
      }
    } else if (!this.bundle.equals(other.bundle)) {
      return false;
    }
    if (this.fileName == null) {
      if (other.fileName != null) {
        return false;
      }
    } else if (!this.fileName.equals(other.fileName)) {
      return false;
    }
    if (this.library == null) {
      if (other.library != null) {
        return false;
      }
    } else if (!this.library.equals(other.library)) {
      return false;
    }
    if (this.version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!this.version.equals(other.version)) {
      return false;
    }
    return true;
  }
  // CHECKSTYLE.ON: CyclomaticComplexity
  // CHECKSTYLE.ON: NPathComplexity

  @Override
  public Bundle getBundle() {
    return this.bundle;
  }

  @Override
  public Map<ContentEncoding, Integer> getCacheState() {
    Map<ContentEncoding, Integer> result = new HashMap<>();
    ContentEncoding[] contentEncodings = ContentEncoding.values();
    for (ContentEncoding contentEncoding : contentEncodings) {
      byte[] cachedData = this.cache.get(contentEncoding);
      if (cachedData != null) {
        result.put(contentEncoding, cachedData.length);
      }
    }
    return result;
  }

  private byte[] getContentData(final ContentEncoding contentEncoding) {
    byte[] contentData = this.cache.get(contentEncoding);
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
    return this.contentType;
  }

  @Override
  public String getETag() {
    return this.etag;
  }

  @Override
  public String getFileName() {
    return this.fileName;
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
    return this.lastModified;
  }

  @Override
  public String getLastModifiedRFC1123GMT() {
    return this.lastModifiedRFC1123GMT;
  }

  @Override
  public String getLibrary() {
    return this.library;
  }

  public int getRawLength() {
    return this.rawLength;
  }

  @Override
  public Version getVersion() {
    return this.version;
  }

  @Override
  @Generated("eclipse")
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (this.bundle == null ? 0 : this.bundle.hashCode());
    result = prime * result + (this.fileName == null ? 0 : this.fileName.hashCode());
    result = prime * result + (this.library == null ? 0 : this.library.hashCode());
    result = prime * result + (this.version == null ? 0 : this.version.hashCode());
    return result;
  }

  private byte[] longToBytes(final long x) {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(x);
    return buffer.array();
  }

  private synchronized byte[] readContentIntoCache(final ContentEncoding contentEncoding) {
    byte[] contentData = this.cache.get(contentEncoding);
    if (contentData == null) {
      try (InputStream inputStream = this.resourceURL.openStream();) {
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
        byte[] buf = new byte[WebResourceImpl.COPY_BUFFER_SIZE];
        int r = inputStream.read(buf);
        while (r > -1) {
          out.write(buf, 0, r);
          r = inputStream.read(buf);
        }
        out.close();
        contentData = bout.toByteArray();
        this.cache.put(contentEncoding, contentData);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return contentData;
  }

  private String resolveETag() {
    try (InputStream in = this.resourceURL.openStream()) {
      MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

      Charset defaultCharset = Charset.forName("UTF8");
      messageDigest.update(this.library.getBytes(defaultCharset));
      messageDigest.update(this.fileName.getBytes(defaultCharset));
      messageDigest.update(this.version.toString().getBytes(defaultCharset));
      byte[] buf = new byte[WebResourceImpl.COPY_BUFFER_SIZE];
      int r = in.read(buf);
      while (r > -1) {
        messageDigest.update(buf, 0, r);
        r = in.read(buf);
      }
      ByteArrayOutputStream bout = new ByteArrayOutputStream();
      bout.write(messageDigest.digest());
      bout.write(longToBytes(this.lastModified));
      return String.format("%x", new BigInteger(1, bout.toByteArray()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  private String resolveLastModifiedRFC1123() {
    Instant instant = Instant.ofEpochMilli(this.lastModified);
    ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(instant, ZoneId.of("GMT"));
    return DateTimeFormatter.RFC_1123_DATE_TIME.format(zonedDateTime);
  }
}
