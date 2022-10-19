package org.infinispan.wfink.playground.ickle.hotrod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.infinispan.wfink.playground.ickle.hotrod.domain.Simple;

/**
 * A simple client which use the Message class with annotations to generate the schema and marshaller for Protobuf. The queries are using a simple field and one analyzed for full-text search. If the server side cache does not have Indexing enables it shows that the full-text query will not work
 * without.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
public class PerfQueryTest {
  private RemoteCacheManager remoteCacheManager;
  private RemoteCache<String, Simple> cache;
  private final Set<String> queryKeys;
  private int ok, ko;

  public PerfQueryTest(String host, String port, String cacheName) {
    ConfigurationBuilder remoteBuilder = new ConfigurationBuilder();
    remoteBuilder.addServer().host(host).port(Integer.parseInt(port)).marshaller(new ProtoStreamMarshaller()); // The Protobuf based marshaller is required for query capabilities

    remoteCacheManager = new RemoteCacheManager(remoteBuilder.build());
    cache = remoteCacheManager.getCache(cacheName);

    if (cache == null) {
      throw new RuntimeException("Cache '" + cacheName + "' not found. Please make sure the server is properly configured");
    }

    registerSchemasAndMarshallers();

    queryKeys = new HashSet<String>();
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
    String msgSchemaFile = null;
    try {
      ProtoSchemaBuilder protoSchemaBuilder = new ProtoSchemaBuilder();
      msgSchemaFile = protoSchemaBuilder.fileName("simple.proto").packageName("playground").addClass(Simple.class).build(ctx);
      protoMetadataCache.put("simple.proto", msgSchemaFile);
    } catch (Exception e) {
      throw new RuntimeException("Failed to build protobuf definition from 'Simple class'", e);
    }

    // check for definition error for the registered protobuf schemas
    String errors = protoMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null) {
      throw new IllegalStateException("Some Protobuf schema files contain errors: " + errors + "\nSchema :\n" + msgSchemaFile);
    }
  }

  private void generateData(int numRecords) {
    for (int i = 1; i <= numRecords; i++) {
      Simple s = new Simple(String.valueOf(i), String.valueOf(i));
      cache.put(s.getKey(), s);
      queryKeys.add(s.getKey());
      if (i % 1000 == 0)
        System.out.println("generating data : " + i);
    }
  }

  private void query() {
    QueryFactory queryFactory = Search.getQueryFactory(cache);
    long start = System.currentTimeMillis();

    this.ok = 0;
    this.ko = 0;
    queryKeys.forEach(key -> {
      Query<Simple> query = queryFactory.create("from playground.Simple where key= :key");
      query.setParameter("key", key);
      List<Simple> list = query.execute().list();
      if (list.size() == 1) {
        this.ok++;
      } else {
        this.ko++;
      }
      if (this.ok % 1000 == 0)
        System.out.println("running query " + ok);
    });

    long duration = System.currentTimeMillis() - start;
    System.out.println("Run " + queryKeys.size() + " within " + duration + "ms ok=" + this.ok + " ko=" + this.ko);
  }

  private void stop() {
    remoteCacheManager.stop();
  }

  public static void main(String[] args) {
    String host = "localhost";
    String port = "11222";
    String cacheName = "PerfQueryCache";

    if (args.length > 0) {
      port = args[0];
    }
    if (args.length > 1) {
      port = args[1];
    }
    PerfQueryTest client = new PerfQueryTest(host, port, cacheName);

    client.generateData(10000);
    client.query();

    client.stop();
    System.out.println("\nDone !");
  }
}
