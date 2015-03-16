# Configuration Files #
The two primary configuration files, web.xml and SRWServer.props, are in the `<SRW>/WEB-INF` and `<SRW>/WEB-INF/classes` directories respectively. web.xml controls the interaction between Tomcat and the SRW servlet. SRWServer.props controls the behavior of the servlet.

## web.xml ##
Every web service, of which SRW is one, has a web.xml configuration file. This file contains configuration information for the servlet engine (Tomcat) and optional parameters for the servlets. It resides in the WEB-INF subdirectory of the web service. There should be little reason to edit this file.

There is a single optional parameter for the SRW server: `PropertiesFile`.  This parameter is commented out in the distribution.  Normally, the server expects to see a file named SRWServer.props somewhere in the Tomcat classpath.  Originally, I tend to put that file in one of the Tomcat shared directories (e.g. `<tomcat>/shared/classes` or `<tomcat>/lib`).  If you have a need to either rename or move that file, then you’ll want to uncomment the parameter and specify the full path to the properties file.

## SRWServer.props ##
The SRWServer.props file contains configuration information common to all databases (such as `resultSetIdleTime` and any SRU input parameter extensions supported.  It also contains the list of databases to be supported.  See SrwServerProps for more details.

## [DSpace](http://www.dspace.org/) specific configuration ##
If you are running DSpace, then you need to change db.DSpace.home to point at your DSpace directory. This will be the directory that contains the DSpace conf directory. (Don't forget the trailing slash on the path.) The SRWServer.props file is set to point at an SRW configuration file for the DSpace database. It expects to find that configuration file in the conf subdirectory of the DSpace directory. You will need to copy the file DSpace.SRWDatabase.props to your /conf directory. (See DSpaceSRWConfiguration.html for information about that file.)

If you are not running DSpace, this section can be left in the configuration file with no ill effects.

## [Pears](http://www.oclc.org/research/software/pears/)/[Newton](http://opensitesearch.sourceforge.net/docs/helpzone/dbb/dbb_01-00-00i.html) specific configuration ##
There are three pieces of information necessary to configure a Pears or Newton database. First, you specify that the database is a Pears/Newton database with the `db.<dbname>.class` parameter, setting it to ORG.oclc.os.SRW.SRWPearsDatabase. Replace `<dbname>` with the name of your database. (Sorry, no embedded blanks in the dbname.) Next, you specify the location of the files needed by the database via the `db.<dbname>.home` parameter. You point this at the path to the directory that contains, at least, the SRW configuration properties file for the database. (Don't forget the trailing slash on the path.) Finally, you specify the name of the properties file via the `db.<dbname>.configuration` parameter. Typically, this file is named SRWDatabase.props. (See PearsSRWConfiguration.html for information about that file.)