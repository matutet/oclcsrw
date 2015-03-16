#Notes from the Access 2010 talk on Linked Data

# Introduction #

Instructions on how to build a simple Linked Data system for records in a directory.


# Prerequisites #

> JDK 1.6 or higher (_might_ work with 1.5)<br />
> Tomcat 5 or 6 (I've not tried 7 yet)<br />
> Ant 1.7 or higher<br />
> Subversion client<br />
<br />

# Instructions #
## Download oclcsrw, build and deploy ##
> svn checkout http://oclcsrw.googlecode.com/svn/trunk/ oclcsrw<br />
> cd oclcsrw<br />
> ant<br />
> cp dist/SRW.war `<tomcat>`/webapps<br />

> Bounce your tomcat.  If everything has worked right, then this URL should get you our trivial SRU interface to date faux database that returns the same record no matter what search you do. http://localhost:8080/SRW/search/test (Adjust the port number as necessary if you did not accept the default tomcat port of 8080.)
<br />

## Bring up database ##
> Put some MarcXML records in a directory.  (The service will work with any XML, but I'm using MarcXML for this demonstration.)  I got some LCNAF records from this SRW server: http://alcme.oclc.org/srw/search/lcnaf.  Search for something amusing (I used my last name).  If you do a "view source" on the response, you'll see MarcXML records in the response.  Cut out individual records from that response and store them as separate files in the directory you picked.  Choose some content in the record to be used as the filename.  That filename (with suitable filtering) will become the URI for the record, so choose wisely.  I chose the LCCN in the 010$a field. (You can just download the two records I stored on `GoogleCode` if the above seems too complicated right now.)<br />

> Download the SRWFileSystemDatabase.template.SRWDatabase.props file into the directory with your XML records.  Rename the database SRWDatabase.props.  Adjust the filenameFilter to only select your XML files from the directory.<br />
> Edit `<tomcat>`/webapps/SRW/WEB-INF/classes/SRWServer.props.  Uncomment the three lines for the SRWFileSystemDatabase.  To change the database name, change the second part of the property name (e.g. change 'lcnaf' to '`MyTest`').  Change the location of the database home to point at the directory with your records and SRWDatabase.props.<br />

> Bounce your tomcat and try http://localhost:8080/SRW/search/lcnaf (adjusted appropriately for your environment.)  If you select "identifier" from the pulldown list under "Browse" and then click the "Browse" button, you should see a list of your XML file names.  Click on one and you should see the file's contents.<br />
<br />

## Decide on your URI pattern ##
> I decided to expose the records as `http://localhost:8080/lcnaf/<recordID>`.  We need to do some work to make that happen.<br />

> Stop your tomcat<br />
> mv `<tomcat>`/webapps/SRW `<tomcat>`/webapps/lcnaf<br />

> This sets the base context of the URI to lcnaf.  This can be slightly confusing as we now have a context named "lcnaf" and a database named "lcnaf", but this typically isn't a problem.  You can choose different names for the context and database.<br />
> We now have a framework where the SRW server will respond to URLs that begin with "lcnaf".  But, the tomcat framework needs a URL pattern after the "lcnaf" context to invoke the SRW server and even then the SRW server is expecting to see an CQL query, not a bare recordID.  How do we bridge that gap?  How do we turn /lcnaf/no12345 into /lcnaf/search/lcnaf?query=identifier=no12345?  With a URL rewrite filter.  I use the OpenSource urlrewritefilter.
> Edit `<tomcat>`/webapps/lcnaf/WEB-INF/urlrewrite.xml<br />
> The file came preconfigured to turn URIs of the form /(`[a-z0-9]`+)/ into /search/lcnaf?query=identifier=%22$1%22.  If you've chosen a different URI pattern, then adjust the configuration file appropriately. If you've named your database something other than lcnaf, then you'll need to change that part of the rule also.<br />
> Bounce your tomcat server<br />
> http://localhost:8080/lcnaf/no201063790<br />
<br />

## `SeeOther` Redirect ##
> The 303 (`SeeOther`) redirect from a URI is the mechanism that tells Linked Data aware applications that they have a `RealWorldObject` in their hands and that the object of the redirect should support content negotiation for RDF.<br />

> The redirect is handled by the urlrewrite filter and came preconfigured in the urlrewrite.xml file.  If you chose a different URI pattern, then you may need to change the regex pattern in the configuration file.  As distributed, it will send URIs of the form /(`[a-z0-9]`+) to /$1/.  (e.g. /no12345 to /no12345/)<br />
<br />

## Content Negotiation ##
> Content Negotiation is handled by the SRW server and configured separately for each database in its SRWDatabase.props file.<br />

> Edit the SRWDatabase.props file for your database.<br />

> Below the basic configuration stuff, you should see references to XML and HTML.  Turning them on will enable content negotiation for those media types and enable server side rendering of the XML to HTML.<br />

> The server-side rendering requires a trick.  The server is going to render the XML just like the browser would.  But the browser gets URLs that point it to the stylesheets it needs.  The server is going to have to use the same mechanism to fetch those stylesheets.  The browser knows what port the server is listening on, but I've found no way for a servlet to find out what port its tomcat is listening on.  So, I've added a parm in the web.xml that tells the SRW servlet what port number to use to fetch stylesheets.<br />
> Edit `<tomcat>`/webapps/lcnaf/WEB-INF/web.xml.  Uncomment the portNumber parameter.  Change the value of 8080 to whatever port your tomcat is listening on.<br />
> Bounce your tomcat server<br />

> If you ask for one of those records again and view the source, you'll see HTML now.
<br />

## Support application/rdf+xml as a media type ##
> To support media types beyond HTML and the native XML, all we need is a stylesheet to generate the new content.  If we want to support RDF, then we need a stylesheet that converts our native XML to RDF.  In the case of our MarcXML, the Library of Congress has already written a stylesheet to produce RDF.<br />

> Download http://www.loc.gov/standards/marcxml/xslt/MARC21slim2RDFDC.xsl and put it in your database directory.<br />
> Edit the SRWDatabase.props file for your database. Find the lines that define the RDF media type and uncomment them.<br />
> Bounce your tomcat server.
<br />

## Bypass Content Negotiation ##
> When a server does Content Negotiation, it is supposed to give the client a URL that it can use to find the content directly (without Content Negotiation).  That URL is supposed to be returned in the Content-Location header of the server response.  We can do that too.<br />

> Edit the SRWDatabase.props file for your database.<br />  Uncomment the `ContentLocation` lines.<br />

> That change tells the SRW server what value to put in the Content-Location header.  It doesn't tell the server how to handle a request for /lcnaf/no12345/lcnaf.xml.  For that, we'll need to go back to the rewrite rules.<br />

> Edit `<tomcat>`/webapps/lcnaf/WEB-INF/urlrewrite.xml.  At the very bottom, you'll see a rule that converts a request for a URI into a query and adds an explicit request for the media type application/rdf+xml.  Copy that rule and add support for lcnaf.xml and lcnaf.html<br />

> Bounce your tomcat server.
> > http://localhost:8080/lcnaf/no201063790/lcnaf.xml<br />
> > http://localhost:8080/lcnaf/no201063790/lcnaf.html<br />
> > http://localhost:8080/lcnaf/no201063790/rdf.xml<br />
<br />

## Teasers ##

> The observant reader will have noticed references to a URL parameter named "service" with a value of "APP".  You will also have noticed tests in the rewrite rules that are conditional on the HTTP method being "GET".  What's with that?<br />

> Our SRW server support the AtomPub protocol.  Databases need to implement a few new methods to support adding, replacing and deleting records and you can do AtomPub record management to your Linked Data collection.  (So far, I've only done that for my internal database system, Pears, but adding it to others, like SRWFileSystemDatabase, would be simple.)<br />

> I'm working on an Atom/RSS Feed mechanism for downloading content.  It will require a date index accessible by SRU.<br />

> I've got support for content-based URIs.  Typical URIs are control numbers.  But, most content is not control numbers.  If we are to encourage content providers to provide links from their content to our content, how can they do that without control numbers.  We'll support URIs built from content: e.g. http://test.viaf.org/viaf/name/shakespeare.  Essentially, we expose an index as a URI and return either a 301 (Moved) redirect, a 404 (Not Found) or a 300 (Multiple Choices) response.  (Truth in Lending: the example above should return a 300, but the current implementation returns the most popular matching record.)<br />

# Links #
> [Linked Data Webinar](http://www.oclc.org/research/events/taichi.htm#ld)

