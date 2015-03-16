How to expose a bunch of records in a directory as Linked Data

# Introduction #

The SRWServer exposes the contents of a database a Linked Data.  But sometimes you only want to expose a single file as Linked Data.  An entire database for one record seems overkill.  Instead, we have a way to declare the contents of a directory to be an SRWDatabase and use the Linked Data (and AtomPub!) capabilities of the SRWServer without having to put the content in a database.


# Details #

Create a directory to hold your database records and the SRWDatabase.props file.  This will be your db.xxx.home directory in the SRWServer.props file

Create an entry in your SRWServer.props file for the new database:
```
    db.xxx.class=ORG.oclc.os.SRW.SRWFileSystemDatabase
    db.xxx.home=c:/f-drive/dbs/xxx/
    db.xxx.configuration=SRWDatabase.props
```

Determine what webapp is going to expose your URIs and create it by cloning your srw webapp. Add the urlrewrite filter to the web.xml.
```
  <filter>
    <filter-name>UrlRewriteFilter</filter-name>
    <filter-class>org.tuckey.web.filters.urlrewrite.UrlRewriteFilter</filter-class>

    <init-param>
      <param-name>confReloadCheckInterval</param-name>
      <param-value>300</param-value>
      </init-param>

    <init-param>
      <param-name>logLevel</param-name>
      <!--param-value>sysout:DEBUG</param-value-->
      <param-value>LOG4J</param-value>
      </init-param>

    <init-param>
      <param-name>statusEnabled</param-name>
      <param-value>true</param-value>
      </init-param>

    <!-- you can change status path so that it does not conflict with your installed
         apps (note, defaults to /rewrite-status) note, must start with / -->
    <init-param>
      <param-name>statusPath</param-name>
      <param-value>/rewrite-status</param-value>
      </init-param> 
    </filter>

  <filter-mapping>
    <filter-name>UrlRewriteFilter</filter-name>
    <url-pattern>/*</url-pattern>
    </filter-mapping> 
```

Add to urlrewrite.xml the patterns that will take care of your RealWorldObjects (303 redirects) and your generic objects (redirects to SRW searches with Service=APP set).
```
<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE urlrewrite PUBLIC "-//tuckey.org//DTD UrlRewrite 2.6//EN"
        "http://tuckey.org/res/dtds/urlrewrite2.6.dtd">
<urlrewrite>
    <rule>
        <note>Send a 303 (SC_SEE_ALSO_REDIRECT) for Real World Objects</note>
        <from>^/([a-z]+)$</from>
        <to type="seeother-redirect">$1/</to>
        </rule>
        
    <rule>
        <note>
            convert URI to SRW search
            </note>
        <condition type="method">GET</condition>
        <from>^/([a-z]+)/$</from>
        <set name="service">APP</set>
        <to>/search/xxx?query=oai.identifier+exact+%22$1.rdf%22</to>
        </rule>
    </urlrewrite>
```

The second rule above would convert the URI /xxx/abc/ to a search for a file with the name abc.rdf.  A more complicated name (like bogus\_record\_number\_abc.rdf) could be accommodated with a simple change to the `<to>` rule:
```
        <to>/search/xxx?query=oai.identifier+exact+%22bogus_record_number_$1.rdf%22</to>
```

At this point, you have the contents of a file directory exposed as an SRW database.  Now follow the instructions for doing LinkedData in SRW.