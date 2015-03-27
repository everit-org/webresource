package org.everit.osgi.webresource.internal;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.everit.osgi.webresource.ContentEncoding;
import org.everit.osgi.webresource.WebResourceConstants;
import org.osgi.framework.Version;

/**
 * Webconsole plugin that shows all available webresources and cache data.
 */
public class WebResourceWebConsolePlugin extends HttpServlet {

  private static final long serialVersionUID = 1L;

  private final WebResourceContainerImpl resourceContainer;

  private final WebResourceUtil webResourceUtil;

  public WebResourceWebConsolePlugin(final WebResourceContainerImpl resourceContainer,
      final WebResourceUtil webResourceUtil) {
    this.resourceContainer = resourceContainer;
    this.webResourceUtil = webResourceUtil;
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException,
      IOException {
    String pluginRootURI = (String) req.getAttribute("felix.webconsole.pluginRoot");
    String requestURI = req.getRequestURI();
    if (requestURI.equals(pluginRootURI)) {
      respondPluginPage(req, resp, pluginRootURI);
    } else {
      String requestPath = requestURI.substring(pluginRootURI.length());

      resp.reset();
      webResourceUtil.findWebResourceAndWriteResponse(req, resp, requestPath);
    }
  }

  private String getStringValue(final Integer value) {
    if (value == null) {
      return "empty";
    } else {
      return value.toString();
    }
  }

  private void respondPluginPage(final HttpServletRequest req, final HttpServletResponse resp,
      final String pluginRootURI)
      throws IOException {
    PrintWriter writer = resp.getWriter();
    writer.write("<table class='content'>");

    writeTableHead(writer);

    writer.write("<tbody>");

    int rawCacheSizeSum = 0;
    int deflateCacheSizeSum = 0;
    int gzipCacheSizeSum = 0;
    Map<String, LibContainer> libContainersByName = resourceContainer.getLibContainersByName();
    DecimalFormat format = new DecimalFormat();
    for (Entry<String, LibContainer> libContainersByNameEntry : libContainersByName.entrySet()) {
      String library = libContainersByNameEntry.getKey();
      LibContainer libContainer = libContainersByNameEntry.getValue();

      Map<String, NavigableMap<Version, Set<WebResourceImpl>>> versionedResourcesByName =
          libContainer.getVersionedResourcesByName();

      for (Entry<String, NavigableMap<Version, Set<WebResourceImpl>>> versionedResourcesByNameEntry : versionedResourcesByName // CS_DISABLE_LINE_LENGTH
          .entrySet()) {

        String fileName = versionedResourcesByNameEntry.getKey();
        NavigableMap<Version, Set<WebResourceImpl>> resourcesByVersion =
            versionedResourcesByNameEntry.getValue();

        for (Entry<Version, Set<WebResourceImpl>> resourcesByVersionEntry : resourcesByVersion
            .entrySet()) {
          Version version = resourcesByVersionEntry.getKey();
          Set<WebResourceImpl> resources = resourcesByVersionEntry.getValue();
          for (WebResourceImpl resource : resources) {
            writer.write("<tr><td class='content'>" + library + "</td>");

            writer.write("<td class='content'><a href=\"" + pluginRootURI + "/" + library
                + (("".equals(library)) ? "" : "/")
                + fileName + "?" + WebResourceConstants.REQUEST_PARAM_VERSION_RANGE + "=["
                + version.toString() + "," + version.toString() + "]\">" + fileName + "</a></td>");

            writer.write("<td class='content'>" + version + "</td>");
            writer.write("<td class='content'>" + resource.getContentType() + "</td>");
            writer.write("<td class='content'>" + resource.getRawLength() + "</td>");
            writer.write("<td class='content'>" + resource.getBundle().toString() + "</td>");
            Map<ContentEncoding, Integer> cacheState = resource.getCacheState();

            Integer rawCacheSize = cacheState.get(ContentEncoding.RAW);
            rawCacheSizeSum += (rawCacheSize == null) ? 0 : rawCacheSize;
            writer.write("<td class='content'>" + getStringValue(rawCacheSize) + "</td>");

            Integer deflateCacheSize = cacheState.get(ContentEncoding.DEFLATE);
            deflateCacheSizeSum += (deflateCacheSize == null) ? 0 : deflateCacheSize;
            writer.write("<td class='content'>" + getStringValue(deflateCacheSize) + "</td>");

            Integer gzipCacheSize = cacheState.get(ContentEncoding.GZIP);
            gzipCacheSizeSum += (gzipCacheSize == null) ? 0 : gzipCacheSize;
            writer.write("<td class='content'>" + getStringValue(gzipCacheSize) + "</td></tr>");
          }
        }
      }
    }
    writer.write("</tbody>");
    writer.write("</table>");

    writer.write("<table class='content'>");
    writer.write("<tr><th class='content container' colspan='2'>Cache state</th></tr>");
    writer.write("<tr><td class='content'>Raw</td><td class='content'>"
        + format.format(rawCacheSizeSum)
        + "</td></tr>");
    writer.write("<tr><td class='content'>Deflate</td><td class='content'>"
        + format.format(deflateCacheSizeSum)
        + "</td></tr>");
    writer.write("<tr><td class='content'>GZip</td><td class='content'>"
        + format.format(gzipCacheSizeSum)
        + "</td></tr>");
    writer.write("<tr><td class='content'>Sum</td><td class='content'>"
        + format.format(rawCacheSizeSum + deflateCacheSizeSum + gzipCacheSizeSum) + "</td></tr>");
    writer.write("</table>");
  }

  private void writeTableHead(final PrintWriter writer) {
    writer.write("<thead>");
    writer.write("<tr><th class='content container' colspan='8'>Web resources</th></tr>");
    writer.write("<tr><th class='content'>Library</th>");
    writer.write("<th class='content'>File</th>");
    writer.write("<th class='content'>Version</th>");
    writer.write("<th class='content'>Content type</th>");
    writer.write("<th class='content'>Size</th>");
    writer.write("<th class='content'>Bundle</th>");
    writer.write("<th class='content'>Raw</th>");
    writer.write("<th class='content'>Deflate</th>");
    writer.write("<th class='content'>GZip</th></tr>");
    writer.write("</thead>");
  }
}
