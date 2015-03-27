package org.everit.osgi.webresource;

/**
 * Standard interface to resolve URI for WebResources.
 */
public interface WebResourceURIResolver {

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
   * @return The full path of the web resource or null if the web resource does not exist.
   */
  String resolve(String lib, String file, String versionRange,
      boolean appendLastModifiedParameter);

}
