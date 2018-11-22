package org.infinispan.wfink.playground.ickle.hotrod;

import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Message;

/**
 * A simple client which use the Message class with annotations to generate the schema and marshaller for Protobuf. The queries are using a simple field and one analyzed for full-text search. If the server side cache does not have Indexing
 * enables it shows that the full-text query will not work without.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
public class MessageQueryHotRodClient {
  private RemoteCacheManager remoteCacheManager;
  private RemoteCache<String, Message> messageCache;

  public MessageQueryHotRodClient(String host, String port, String cacheName) {
    ConfigurationBuilder remoteBuilder = new ConfigurationBuilder();
    remoteBuilder.addServer().host(host).port(Integer.parseInt(port))
    .marshaller(new ProtoStreamMarshaller()); // The Protobuf based marshaller is required for query capabilities

    remoteCacheManager = new RemoteCacheManager(remoteBuilder.build());
    messageCache = remoteCacheManager.getCache(cacheName);

    if (messageCache == null) {
      throw new RuntimeException("Cache '" + cacheName + "' not found. Please make sure the server is properly configured");
    }

    registerSchemasAndMarshallers();
  }

  /**
   * Register the Protobuf schemas and marshallers with the client and then register the schemas with the server too.
   */
  private void registerSchemasAndMarshallers() {
    // Register entity marshallers on the client side ProtoStreamMarshaller
    // instance associated with the remote cache manager.
    SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);

    // Cache to register the schemas with the server too
    final RemoteCache<String, String> protoMetadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

    // generate the message protobuf schema file and marshaller based on the annotations on Message class
    // and register it with the SerializationContext of the client
    try {
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      String msgSchemaFile = protoSchemaBuilder.fileName("message.proto").packageName("playground").addClass(Message.class).build(ctx);
      protoMetadataCache.put("message.proto", msgSchemaFile);
    } catch (Exception e) {
      throw new RuntimeException("Failed to build protobuf definition from 'Message class'", e);
    }

    // check for definition error for the registered protobuf schemas
    String errors = protoMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null) {
      throw new IllegalStateException("Some Protobuf schema files contain errors:\n" + errors);
    }
  }

  private void runIckleQuery(QueryFactory qf, String query) {
    try {
      Query q = qf.create(query);
      List<Object> results = q.list();
      System.out.printf("Query %s  : found %d matches\n", query, results.size());
      for (Object m : results) {
        System.out.println(">> " + m);
      }
    } catch (Exception e) {
      System.err.println("ICKLE QUERY FAILURE : " + e.getMessage());
    }
  }

  private void insertMessages() {
    System.out.println("Inserting Messages into cache...");
    messageCache.put("1", new Message(1, "First message for Ickle query", "Wolf", "Gustavo"));
    messageCache.put("2", new Message(1, "Second message for Ickle query", "Wolf", "Adrian"));
    messageCache.put("3", new Message(1, "A notification", "Wolf", "Tristan"));
    messageCache.put("4", new Message(1, "Another message", "Wolf", "Pedro"));
  }

  private void findMessages() {
    QueryFactory qf = Search.getQueryFactory(messageCache);

    runIckleQuery(qf, "from playground.Message m where m.author = 'Wolf'");
    runIckleQuery(qf, "from playground.Message m where m.reader = 'Gustavo'");
    runIckleQuery(qf, "from playground.Message m where m.text : '*Ickle*'");
    runIckleQuery(qf, "from playground.Message m where m.text : '*ickle*'");
    // this will not work as the text field is analyzed for full-text query
    runIckleQuery(qf, "from playground.Message m where m.text = 'A notification'");
  }

  private void stop() {
    remoteCacheManager.stop();
  }

  public static void main(String[] args) {
    String host = "localhost";
    String port = "11222";
    String cacheName = "IcklePlayMessageCache";

    if (args.length > 0) {
      port = args[0];
    }
    if (args.length > 1) {
      port = args[1];
    }
    MessageQueryHotRodClient client = new MessageQueryHotRodClient(host, port, cacheName);

    client.insertMessages();
    client.findMessages();

    client.stop();
    System.out.println("\nDone !");
  }
}
