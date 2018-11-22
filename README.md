Infinispan Queries with Ickle
===============================

Author: Wolf-Dieter Fink
Level: Advanced
Technologies: Infinispan, Hot Rod, Query, Ickle


What is it?
-----------

Different examples how to use the Ickle query language.

Hot Rod is a binary TCP client-server protocol. The Hot Rod protocol facilitates faster client and server interactions in comparison to other text based protocols and allows clients to make decisions about load balancing, failover and data location operations.

This example demonstrates how to use a server side Filter to retrieve only the entries matching some criteria.


Prepare a server instance
-------------
Simple start a Infinispan or JDG server and add the following cache.

Build and Run the example
-------------------------
1. Type this command to build and deploy the archive:

        mvn clean package

2. Use java command or an IDE to start a simple example

   MessageQueryHotRodClient
 
     This example use a simple String key with the Message.id to store different messages.
     The client use Ickle queries to demonstrate simple queries to match an attribute or use analyzed fields.

     Before runing it add the following cache configuration to the default configuration

       If standalone.xml (not clustered) is used
         <local-cache name="IcklePlayMessageCache"/>
       If clustered.xml is used
         <replicated-cache name="IcklePlayMessageCache"/>

     Note that we don't use an Indexed cache here, so the query can be slow.
     As a result the client will fail to use a full-text query for the 'text' field.
           IllegalStateException: The cache must be indexed in order to use full-text queries
     Change the configuration as followed:

        <*-cache name="IcklePlayMessageCache">
          <indexing index="ALL" auto-config="true"/>
        </*-cache>

    After the change the full-text is working, but note that the query will only work without a cluster correctly.
    Running in a cluster might need some adjustments to the index configuration as the index need to be build different and shared.

   You might noticed that the query with "text : '*Ickle*'" will not find any, this is related to a bug in 9.4 https://issues.jboss.org/browse/ISPN-9494



   CompanyQueryHotRodClient
 
     This example use a simple String key to store a Company object with inner list of Employees.
     The client use Ickle queries to demonstrate how to access the list and use queries to match an attribute of the inner object.

     Before runing it add the following cache configuration to the default configuration

       <*-cache name="IcklePlayCompanyCache"/>
