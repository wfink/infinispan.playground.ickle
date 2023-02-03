package org.infinispan.wfink.playground.ickle.hotrod;

import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Message;
import org.infinispan.wfink.playground.ickle.hotrod.domain.MessageInitializer;

/**
 * A simple client which use the Message class with annotations to generate the schema and marshaller for Protobuf. The queries are using a simple field and one analyzed for full-text search. If the server side cache does not have Indexing enables it shows that the full-text query will not work
 * without.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
public class MessageQueryHotRodClient {
  private RemoteCacheManager remoteCacheManager;
  private RemoteCache<String, Message> messageCache;

  public MessageQueryHotRodClient(String host, String port, String cacheName) throws Exception {
    ConfigurationBuilder remoteBuilder = new ConfigurationBuilder();
    remoteBuilder.addServer().host(host).port(Integer.parseInt(port));

    MessageInitializer initializer = new org.infinispan.wfink.playground.ickle.hotrod.domain.MessageInitializerImpl();
    System.out.printf("Generated protobuf schema:\n%s\n", initializer.getProtoFile());
    remoteBuilder.addContextInitializer(initializer);

    remoteCacheManager = new RemoteCacheManager(remoteBuilder.build());
    messageCache = remoteCacheManager.getCache(cacheName);

    if (messageCache == null) {
      throw new RuntimeException("Cache '" + cacheName + "' not found. Please make sure the server is properly configured");
    }
//    registerSchema(initializer); // it is possible to register the schema with the client, but not recommended as it could overwrite current schema with old ones in production
  }

  /**
   * Register the Protobuf schemas and marshallers with the client and then register the schemas with the server too.
   */
  private void registerSchema(MessageInitializer initializer) {
    // Cache to register the schemas with the server too
    final RemoteCache<String, String> protoMetadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);
    // add the schema to the server side
    protoMetadataCache.put(initializer.getProtoFileName(), initializer.getProtoFile());

    // check for definition error for the registered protobuf schemas
    String errors = protoMetadataCache.get(".errors"); // ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX is only available if query dependency is set
    if (errors != null) {
      throw new IllegalStateException("Some Protobuf schema files contain errors: " + errors + "\nSchema :\n" + initializer.getProtoFileName());
    }
  }

  private void runIckleQuery4Message(QueryFactory qf, String query) {
    try {
      Query<Message> q = qf.create("from playground.Message m where " + query);
      List<Message> results = q.execute().list();
      System.out.printf("Query %s  : found %d matches\n", query, results.size());
      for (Message m : results) {
        System.out.println(">> " + m);
      }
    } catch (Exception e) {
      System.err.printf("ICKLE QUERY '%s' FAILURE : %s\n", query, e.getMessage());
    }
  }

  private void insertMessages() {
    System.out.println("Inserting Messages into cache...");
    messageCache.put("1", new Message(1, "First message for Ickle query", "Wolf", "Gustavo"));
    messageCache.put("2", new Message(2, "Second message for Ickle query", "Wolf", "Adrian"));
    messageCache.put("3", new Message(3, "A notification", "Wolf", "Tristan"));
    messageCache.put("4", new Message(4, "Another message", "Wolf", "Pedro"));
    messageCache.put("5", new Message(5, "Another message for Ickle-Query", "Wolf", "Adrian"));
    messageCache.put("6", new Message(6, "And another message for .Ickle.Query. with dots", "Wolf", "Adrian Nistor"));
    messageCache.put("7", new Message(7, "And another message to check query Ickle will be found", "Wolf", "Adrian"));
    messageCache.put("8", new Message(8, "And another message to check query MyIckleBla will be found", "Wolf Fink", "Adrian Nistor"));
  }

  private void findMessages() {
    QueryFactory qf = Search.getQueryFactory(messageCache);

    runIckleQuery4Message(qf, "m.author = 'Wolf'");
    runIckleQuery4Message(qf, "m.reader = 'Gustavo'");
    runIckleQuery4Message(qf, "m.reader = 'Adrian'");
    runIckleQuery4Message(qf, "m.reader = 'Adrian*'"); // will find 'Adrian Nistor' as well
    runIckleQuery4Message(qf, "m.reader = 'Adrian Nistor'");
    runIckleQuery4Message(qf, "m.text : '*Ickle'");
    runIckleQuery4Message(qf, "m.text : '*Ickle*'");
    runIckleQuery4Message(qf, "m.text : '*Ickle*' order by m.reader");
    runIckleQuery4Message(qf, "m.text : '*ickle*'");
    runIckleQuery4Message(qf, "m.text : 'ickle'");
    runIckleQuery4Message(qf, "m.text : 'Ickle'");
    runIckleQuery4Message(qf, "m.text : 'query Ickle'");
    runIckleQuery4Message(qf, "m.text : (+'message' -'Ickle' +'query')"); // match if one of the criteria matches
    runIckleQuery4Message(qf, "m.text : (+'message' && -'Ickle' && +'query')"); // match if all of the criteria matches
    runIckleQuery4Message(qf, "m.text : (+'message' +'query') AND m.text : (-'Ickle')"); // match if one of the tokens message||query match but not include Ickle
    // this will not work as the text field is analyzed for full-text query
    // runIckleQuery4Message(qf, "m.text = 'A notification'");
  }

  private void stop() {
    remoteCacheManager.stop();
  }

  public static void main(String[] args) throws Exception {
    String host = "localhost";
    String port = "11222";
    String cacheName = "IckleMessageCache";

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
