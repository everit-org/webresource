webresource
===========

## Introduction

Webresources are small static files that are downloaded during rendering
a website. E.g.: CSS, Javascript, images, ...

## Basic usage

 - Install the org.everit.osgi.webresource module into your OSGi container
 - Install the WebResourceServlet (that is provided as an OSGi service) into
   your ServletContext
 - Use CommonContextWebResourceURIGenerator to generate URIs that point to
   WebResources. This class will pick up the URIGenerator(s) registered by
   WebResourceServlet within the same ServletContext.

You can also install custom WebResourceURIGenerator implementations in the
servletContext. In this case:

 - Get the Collection of WebResourceGenerators from the servlet context by
   calling _WebResourceUtil.getUriGeneratorsOfServletContext(context);_ 
 - Add your generator to the collection

## Create WebResource packages

To make an OSGi bundle also a WebResource package the _everit.webresource_
capability must be provided. The capability can have the following
attributes:

 - __resourceFolder:__ The folder in the bundle where the resources
   are located

 - __libraryPrefix:__ A prefix that should be pasted in front of the
   folder structure in the URL. E.g.: "foo/bar"

 - __version:__ Optional attribute that can define the version of the
   webresources. If not defined, the version of the bundle will be used.

## Version handling

The _webresource_version_ can be specified as a parameter of the servlet
request. Using ranges in the version expression is allowed. Examples: 

 - /alias/jquery/jquer.js?webresource_version=2.1.0
 - /alias/jquery/jquer.js?webresource_version=[2.1.0,3)


## Cache

There is a primitive, in-memory cache. The GZIP, Deflate and RAW data is
stored in cache after the first request. When a bundle is stopped, all
webresources are removed from the cache that came from that bundle.


## WebConsole Plugin

There is a WebConsole plugin that is registered when the Extender component
is started. It shows the registered webresources and the state of the caches.


### Content-Encoding

GZIP, Deflate and RAW content encodings are supported.

## ETag support

SHA-256 hash of the RAW content is concatenated with the last modification date
of the webresource file.

## Minimum requirements

 - __OSGi 6:__ WebResourceServlet is registered with prototype service scope.
   Luckily Felix and Equinox has OSGi 6 support now.
 - __Servlet 3.1:__ _WebResourceUtil_ writes the content of the WebResources
   using asynchronous IO if possible.  
 - __Java 8__: Many of the features of Java 8 is used (time, optional, ...)
