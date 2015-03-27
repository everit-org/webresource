package org.everit.osgi.webresource;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

/**
 * A WebResource is a file that is accessible for everyone can access. E.g.: CSS, JS and image
 * files.
 */
public interface WebResource {

  /**
   * The bundle that offers the {@link WebResource}.
   */
  Bundle getBundle();

  /**
   * After a {@link WebResource} is requested with a {@link ContentEncoding}, the content is cached
   * in the memory.
   *
   * @return The size that is occupied by the {@link WebResource} in the cache for each
   *         {@link ContentEncoding}.
   */
  Map<ContentEncoding, Integer> getCacheState();

  /**
   * The length of the content of the {@link WebResource} in the specific {@link ContentEncoding}.
   *
   * @param contentEncoding
   *          The {@link ContentEncoding} of the {@link WebResource}.
   * @return the length of the content in bytes.
   */
  long getContentLength(ContentEncoding contentEncoding);

  /**
   * The content type of the {@link WebResource}.
   */
  String getContentType();

  /**
   * The calculated ETag of the {@link WebResource} that can be used to save bandwidth.
   */
  String getETag();

  /**
   * The fileName of the {@link WebResource}.
   */
  String getFileName();

  /**
   * Creates a new {@link InputStream} to read the content of the {@link WebResource}.
   *
   * @param contentEncoding
   *          The {@link ContentEncoding} that we should get the data with.
   * @param beginIndex
   *          The beginning inded of the content.
   * @return The generated {@link InputStream}.
   * @throws IOException
   *           if a read error occures.
   */
  InputStream getInputStream(ContentEncoding contentEncoding, int beginIndex) throws IOException;

  /**
   * The date when the {@link WebResource} was last modified. This is normally the date of the
   * resource within the bundle.
   */
  long getLastModified();

  /**
   * The name of the library of the {@link WebResource}. E.g.: jquery.
   */
  String getLibrary();

  /**
   * The exact version of the {@link WebResource}.
   */
  Version getVersion();

}
