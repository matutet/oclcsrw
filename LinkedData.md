# Introduction #

We are using this SRU server to deliver Linked Data for our [VIAF](http://viaf.org) project.  That means that we support generic URIs for objects in our repository.  The generic URIs support content negotiation and suffixes can be added to the generic URI to pipeline the content negotiation.  We support a URI for the Real World Object version of the object, including doing 303 (see other) redirection to either the generic URI or to the RDF version of the object (based on content negotiation on the Real World Object URI.)


# Details #

The way the magic works is that I have a [urlrewrite filter](http://tuckey.org/urlrewrite/) in front of my server and when it sees a URI, it pulls the record key out of the URI and reformulates an SRU search on that key.  It also adds a parm to the request to tell my server to strip off the SRU response wrapper from the response, returning the bare record.  It can also set another parm telling the server what content type to set in the response.

# Features and How They Are Implemented #
## Generic URI ##
First, you need some regular expression that you can use to detect the URI nature of the incoming URL.  In the case of VIAF, the URI consists of 1 or more digits, so the pattern is `([0-9][0-9]+)`.

In the urlrewrite filter configuration file (urlrewrite.xml) put in a rule for that pattern.  In VIAF, that rule looks like this:
```
    <rule>
        <note>
            convert viafID
            </note>
        <condition type="method">GET</condition>
        <from>/([0-9][0-9]+)$</from>
        <set name="service">APP</set>
        <to>/search?query=local.viafID+exact+%22$1%22</to>
        </rule>
```
This is critical to get right, so let me explain what is going on here.

The `<condition>` element ensures that what we have is a GET request.  (My server is slowly working its way toward supporting the Atom Publishing Protocol, so it supports other HTTP methods on that URI.)

The `<from>` element is the test that this is a URI.

The `<set>` action sets a parm named "service" to "APP", signaling to the server that it has gotten an `AtomPub` request and should expect to strip the SRU response wrapper off the outbound response and return just the bare record in the response.  (It used to be that I would accept one of these URIs and return an SRU response with the record embedded.  I decided that that was bad behavior and now return only the record the client requested.  If they wanted an SRU response, they'd have asked for one.)

The `<to>` element makes the SRU request out of the URI.  In this case, all it has to do is create the query parm of the SRU request and everything else will have appropriate defaults to return the record.  See the later example for how to ask for non-default forms of the record.

But...

This assumes that you want the default record schema to be what is returned by the generic URI.  My experience is that the generic URI should return HTML, unless content negotiation causes you to want to return something else.  So, how can we get the generic URI to return HTML?

First, you have to tell the SRU server that you want the HTML version of the record to be returned.  That assumes that HTML is one of the record schemas you support.  So, let's assume that you have such a record schemas and that its name is "HTML".  Then the `<to>` element becomes:
```
        <to>/search?query=local.viafID+exact+%22$1%22&amp;recordSchema=HTML</to>
```

But...

The default `ContentType` returned by the SRU server is text/xml.  We want to tell the client that we are returning text/html or application/xhtml.  We can do this by using the `<set>` element to add a "`ContentType`" parameter.  So, we'll add this to our rule:
```
        <set name="`ContentType`">text/html</set>
```