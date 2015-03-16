# Defining Local SRU Parameters #

SRU input parameter extensions take the form:
```
extension.<sru-parm>=<srw-element>
extension.<srw-element>.namespace=<srw-element-namespace>
```

For instance, to request a restrictor summary (a local version of facets) I add this to my SRWServer.props file:
```
extension.x-info-14-restrictorSummary=restrictorSummary
extension.restrictorSummary.namespace=info:srw/extension/14/restrictorSummary
```
The value for the namespace is pretty much meaningless, so make up your own URL.

# Accessing Those Parameters In Your Code #
The SRWDatabase class exposes a Hashtable names extraRequestDataElements.  Using the get() method on that class with a key whose value is the `<srw-element>` from above will return the value of that parameter.  In the example above, to get the value of the SRU x-info-14-restrictorySummary parameter, I do this:
```
        String s=(String)extraRequestDataElements.get("restrictorSummary");
        if(s!=null && !s.equals("false")) {
            restrictorSummary=true;
            log.info("turned restrictorSummary on");
        }
```

# Setting Extra Data #