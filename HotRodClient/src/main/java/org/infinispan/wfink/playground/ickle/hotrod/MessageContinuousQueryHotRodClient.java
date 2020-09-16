package org.infinispan.wfink.playground.ickle.hotrod;

import java.io.Console;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.api.continuous.ContinuousQuery;
import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Message;
import org.infinispan.wfink.playground.ickle.hotrod.listener.MessageContinuousQueryListener;

/**
 * A simple client which use the Message class with annotations to generate the schema and marshaller for Protobuf. The client is to show how continuous queries can be used as it will use is to show messages for a registered reader.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
public class MessageContinuousQueryHotRodClient {
  final Console con;
  private RemoteCacheManager remoteCacheManager;
  private RemoteCache<String, Message> messageCache;
  private ContinuousQuery<String, Message> continuousQuery;
  private ContinuousQueryListener<String, Message> currentListener;

  public MessageContinuousQueryHotRodClient(Console con, String host, int port, String cacheName) {
    this.con = con;
    ConfigurationBuilder remoteBuilder = new ConfigurationBuilder();
    remoteBuilder.addServer().host(host).port(port).marshaller(new ProtoStreamMarshaller()); // The Protobuf based marshaller is required for query capabilities

    remoteCacheManager = new RemoteCacheManager(remoteBuilder.build());
    messageCache = remoteCacheManager.getCache(cacheName);

    if (messageCache == null) {
      throw new RuntimeException("Cache '" + cacheName + "' not found. Please make sure the server is properly configured");
    }

    registerSchemasAndMarshallers();

    continuousQuery = Search.getContinuousQuery(messageCache);
  }

  /**
   * Register the Protobuf schemas and marshallers with the client and then register the schemas with the server too.
   */
  private void registerSchemasAndMarshallers() {
    // Register entity marshallers on the client side ProtoStreamMarshaller
    // instance associated with the remote cache manager.
    SerializationContext ctx = MarshallerUtil.getSerializationContext(remoteCacheManager);

    // Cache to register the schemas with the server too
    final RemoteCache<String, String> protoMetadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

    // generate the message protobuf schema file and marshaller based on the annotations on Message class
    // and register it with the SerializationContext of the client
    String msgSchemaFile = null;
    try {
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      msgSchemaFile = protoSchemaBuilder.fileName("message.proto").packageName("playground").addClass(Message.class).build(ctx);
      protoMetadataCache.put("message.proto", msgSchemaFile);
    } catch (Exception e) {
      throw new RuntimeException("Failed to build protobuf definition from 'Message class'", e);
    }

    // check for definition error for the registered protobuf schemas
    String errors = protoMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null) {
      throw new IllegalStateException("Some Protobuf schema files contain errors: " + errors + "\nSchema :\n" + msgSchemaFile);
    }
  }

  private void registerContinuousQuery() {

    if (currentListener != null) {
      unregisterContiniousQuery();
    }

    String readerName = con.readLine("Enter reader: ");
    QueryFactory queryFactory = Search.getQueryFactory(messageCache);
    Query<Message> query = queryFactory.create("FROM playground.Message m WHERE m.reader = '" + readerName + "'");

    // Define the ContinuousQueryListener
    currentListener = new MessageContinuousQueryListener(readerName);

    // Add the listener and the query
    continuousQuery.addContinuousQueryListener(query, currentListener);
  }

  private void unregisterContiniousQuery() {
    if (currentListener != null) {
      System.out.println("Unregister Listener " + currentListener);
      continuousQuery.removeContinuousQueryListener(currentListener);
      currentListener = null;
    } else {
      System.out.println("No listner registered!");
    }
  }

  public void put() {
    int id = readId();
    String author = con.readLine("Enter author: ");
    String reader = con.readLine("Enter reader: ");
    String text = con.readLine("        Text: ");

    Message msg = new Message(id, text, author, reader);

    Message oldMsg = messageCache.put(String.valueOf(id), msg);
    if (oldMsg != null) {
      con.printf("   Replaced : %s\n", oldMsg);
    }
  }

  public void remove() {
    int id = readId();
    messageCache.remove(String.valueOf(id));
  }

  public void list() {
    con.printf("\n list of messages \n");
    for (String key : messageCache.keySet()) {
      con.printf("  Entry  : %s\n", messageCache.get(key));
    }
    con.printf("\n");
  }

  public void size() {
    con.printf("  Cache size is %d\n", messageCache.size());
  }

  private int readId() {
    Integer i = null;

    while (i == null) {
      String id = con.readLine("Enter id: ");
      try {
        i = Integer.parseInt(id);
      } catch (Exception e) {
      }
    }

    return i;
  }

  private void stop() {
    unregisterContiniousQuery();
    remoteCacheManager.stop();
  }

  private void inputLoop() {
    while (true) {
      try {
        String action = con.readLine(">");
        if ("put".equals(action)) {
          put();
        } else if ("rm".equals(action)) {
          remove();
        } else if ("register".equals(action)) {
          registerContinuousQuery();
        } else if ("unregister".equals(action)) {
          unregisterContiniousQuery();
        } else if ("list".equals(action)) {
          list();
        } else if ("size".equals(action)) {
          size();
        } else if ("q".equals(action)) {
          break;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void printConsoleHelp() {
    con.printf("Choose:\n" + "============= \n" + "put   -  put an entry\n" + "rm    -  remove an entry\n" + "list  -  list all entries which are cache\n" + "size  -  number of cache entries\n" + "register    -  register QueryListener\n" + "unregister  -  unregister all QueryListener\n"
        + "q     -  quit\n");
  }

  public static void main(String[] args) {
    final Console con = System.console();
    String host = "127.0.0.1";
    int port = 11222;
    String cacheName = "IcklePlayMessageCache";

    int argc = 0;
    while (argc < args.length) {
      if (args[argc].equals("-host")) {
        argc++;
        host = args[argc];
        argc++;
      } else if (args[argc].equals("-port")) {
        argc++;
        port = Integer.valueOf(args[argc]);
        argc++;
      } else {
        con.printf("option '%s' unknown\n", args[argc]);
        System.exit(1);
      }
    }

    MessageContinuousQueryHotRodClient client = new MessageContinuousQueryHotRodClient(con, host, port, cacheName);

    client.printConsoleHelp();
    client.inputLoop();

    client.stop();
    System.out.println("\nDone !");
  }
}
