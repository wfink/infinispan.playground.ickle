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
Simple start a Infinispan 10+ or RHDG 8+ server and add the following cache.

Build and Run the example
-------------------------
1. Type this command to build and deploy the archive:

        mvn clean package

     Before runing it add the following cache configuration to the infinispan.xml configuration

         <replicated-cache name="IcklePlayCompanyCache"/>
         <replicated-cache name="IcklePlayMessageCache"/>

2. Use java command or an IDE to start a simple example

   MessageQueryHotRodClient
 
     This example use a simple String key with the Message.id to store different messages.
     The client use Ickle queries to demonstrate simple queries to match an attribute or use analyzed fields.

     Note that we don't use an Indexed cache here, so the query can be slow.
     As a result the client will fail to use a full-text query for the 'text' field.
           IllegalStateException: The cache must be indexed in order to use full-text queries
     Change the configuration as followed:

        <*-cache name="IcklePlayMessageCache">
          <indexing>
            <indexed-entities>
              <indexed-entity>playground.Message</indexed-entity>
            </indexed-entities>
          </indexing>
        </*-cache>

    After the change the full-text is working, but note that the query will only work without a cluster correctly.
    Running in a cluster might need some adjustments to the index configuration as the index need to be build different and shared.



   CompanyQueryHotRodClient
 
     This example use a simple String key to store a Company object with inner list of Employees.
     The client use Ickle queries to demonstrate how to access the list and use queries to match an attribute of the inner object.

     Before runing it add the following cache configuration to the default configuration

        <*-cache name="IcklePlayCompanyCache">
          <indexing>
            <indexed-entities>
              <indexed-entity>playground.Company</indexed-entity>
            </indexed-entities>
          </indexing>
        </*-cache>

3. Use Maven to start a client with a continuous query example

   MessageContinuousQueryHotRodClient

  Start one or more clients with

       maven exec:java

  The client is able to add and list messages in cache. 
  Register a ContinuousQuery for a reader with 'register' and start another instance to add, update and remove messages.
  The registered ContinuousQuery listener will show each change which match the reader with prefix NEW/DELETED/UPDATED.
