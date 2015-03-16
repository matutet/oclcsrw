# Introduction #

In VIAF and WorldCat Identities, we claim to support persistent URIs.  But, our URIs are usually made from some content in the record (like an LCCN) and that content changes with alarming regularity.  This article will explain how we support persistence in the URIs.


# Details #

The trick is to make a database with either the missing records or some sort of pointer to the new record that corresponds to the old URI.

In WorldCat Identities, we compare the URIs in an old database with the URIs in a new database and generate a list of URIs missing in the new database.  We then fetch the records corresponding to the missing URIs from the old database and look to see if their content can be found in a record in the new database.  If so, we create a record for an AbandonedIdentities database that has the old URI, the new URI and some explanation of how we came to pick that new URI.  If we can't find the content, we simply place the old record into the AbandonedIdentities database.

We do a federated search whenever we do a URI lookup in the Identities or VIAF databases.  If the URI is found in the current database, then we're done.  If the URI was found in the backup database, then we either generate an HTTP Redirect or display the old record with some sort of warning.

The HTTP redirect is generated by the SRWMergeDatabase.  After a record is fetched from the backup database, it is transformed by the Merge database.  The HTTPGenericHeaderSetter class is always looking for a particular type of record in a response and will generate the Redirect response.  The transformation is specified in the SRWDatabase.props file for the Merge database.  An example for VIAF is below:
```
DBList=VIAF, Persist
Persist=PersistToRedirect.xsl
Persist.identifier=http://oclcsrw.google.code/redirect
```