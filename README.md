webresource
===========

## Introduction

Webresources are small static files that are downloaded during rendering
a website. E.g.: CSS, Javascript, images, ...


## The Extender Component

There is an Extender component with the following configuration option:

 - __alias:__ Coming from whiteboard pattern. This is the alias where the
   registered servlet that serves the resources will listen.


## Capability

The extender picks up every bundle that has the "everit.webresource"
capability. The capability can have the following attributes:

 - __resourceFolder:__ The folder in the bundle where the resources
   are located

 - __libraryPrefix:__ A prefix that should be pasted in front of the
   folder structure in the URL.

 - __version:__ Optional attribute that can define the version of the
   webresources. If not defined, the version of the bundle will be used.


## Version handling

The _webresource_version_ can be specified as a parameter of the servlet
request. Using ranges in the version expression is allowed. Examples: 

 - /alias/jquery/jquer.js?webresource_version=2.1.0
 - /alias/jquery/jquer.js?webresource_version=[2.1.0,3)


## WebResourceLocator

Not implemented yet

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

[![Analytics](https://ga-beacon.appspot.com/UA-15041869-4/everit-org/webresource)](https://github.com/igrigorik/ga-beacon)
