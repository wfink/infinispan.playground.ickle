package org.infinispan.wfink.playground.ickle.hotrod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.MarshallerUtil;
import org.infinispan.commons.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.annotations.ProtoSchemaBuilder;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Company;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Employee;
import org.infinispan.wfink.playground.ickle.hotrod.marshaller.CompanyMarshaller;
import org.infinispan.wfink.playground.ickle.hotrod.marshaller.EmployeeMarshaller;

/**
 * A simple client which use a proto file to register the schema and marshaller for Protobuf. The queries are using a simple field and one analyzed for full-text search. If the server side cache does not have Indexing enabled it shows that the full-text query will not work without. This is an
 * example how to use *.proto file and MessageMarshaller implementations to configure protobuf. This has been deprecated and the {@link ProtoSchemaBuilder} should be used to generate the resources from the annotations within the Java Pojos.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
public class CompanyQueryHotRodClient {
  private static final String PROTOBUF_DEFINITION_COMPANY = "/playground/company.proto";

  private RemoteCacheManager remoteCacheManager;
  private RemoteCache<Integer, Company> companyCache;

  public CompanyQueryHotRodClient(String host, String port, String cacheName) {
    ConfigurationBuilder remoteBuilder = new ConfigurationBuilder();
    remoteBuilder.addServer().host(host).port(Integer.parseInt(port)).marshaller(new ProtoStreamMarshaller()); // The Protobuf based marshaller is required for query capabilities

    remoteCacheManager = new RemoteCacheManager(remoteBuilder.build());
    companyCache = remoteCacheManager.getCache(cacheName);

    if (companyCache == null) {
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
    SerializationContext ctx = MarshallerUtil.getSerializationContext(remoteCacheManager);
    // register the necessary proto files
    try {
      ctx.registerProtoFiles(FileDescriptorSource.fromResources(PROTOBUF_DEFINITION_COMPANY));
    } catch (Exception e) {
      throw new RuntimeException("Failed to read protobuf definition '" + PROTOBUF_DEFINITION_COMPANY + "'", e);
    }
    ctx.registerMarshaller(new CompanyMarshaller());
    ctx.registerMarshaller(new EmployeeMarshaller());

    // register the schemas with the server too
    final RemoteCache<String, String> protoMetadataCache = remoteCacheManager.getCache(ProtobufMetadataManagerConstants.PROTOBUF_METADATA_CACHE_NAME);

    protoMetadataCache.put(PROTOBUF_DEFINITION_COMPANY, readResource(PROTOBUF_DEFINITION_COMPANY));

    // check for definition error for the registered protobuf schemas
    String errors = protoMetadataCache.get(ProtobufMetadataManagerConstants.ERRORS_KEY_SUFFIX);
    if (errors != null) {
      throw new IllegalStateException("Some Protobuf schema files contain errors:\n" + errors);
    }
  }

  /**
   * Helper to read the protobuf file to String
   *
   * @param resourcePath
   * @return
   */
  private String readResource(String resourcePath) {
    try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
      InputStreamReader reader = new InputStreamReader(is, "UTF-8");
      StringWriter writer = new StringWriter();
      char[] buf = new char[1024];
      int len;
      while ((len = reader.read(buf)) != -1) {
        writer.write(buf, 0, len);
      }
      return writer.toString();
    } catch (Exception e) {
      throw new RuntimeException("Failed to read from resource '" + resourcePath + "'", e);
    }
  }

  private void runIckleQuery4Company(QueryFactory qf, String query) {
    Query<Company> q = qf.create("from playground.Company c where " + query);
    List<Company> results = q.execute().list();
    System.out.printf("Query %s  : found %d matches\n", query, results.size());
    for (Company c : results) {
      printCompany(c);
    }
  }

  private void printCompany(Company c) {
    System.out.println("   " + c);
    if (c.getEmployees().size() > 0) {
      System.out.println("     Employees:");
      for (Employee e : c.getEmployees()) {
        System.out.println("       " + e);
      }
    }
  }

  private void insertCompanies() {
    System.out.println("Inserting Companies into cache...");
    Company c = new Company(1, "Red Hat", true);
    c.getEmployees().add(new Employee(1, "Wolf Fink", "wf@redhat.com", 127, true));
    c.getEmployees().add(new Employee(2, "William", "m@redhat.com", 17, true));
    companyCache.put(c.getId(), c);
    c = new Company(2, "JBoss", false);
    c.getEmployees().add(new Employee(3, "Adrian Brock", "ab@jboss.org"));
    c.getEmployees().add(new Employee(4, "Scott Stark", "sst@jboss.org"));
    companyCache.put(c.getId(), c);
    c = new Company(3, "Microsoft", true);
    companyCache.put(c.getId(), c);
    c = new Company(4, "SAP", true);
    companyCache.put(c.getId(), c);
    c = new Company(5, "Orga Systems", false);
    companyCache.put(c.getId(), c);
  }

  private void insertCompanyCircular() {
    System.out.println("Inserting Companies into cache...");
    Company c = new Company(1, "Red Hat", true);
    c.getEmployees().add(new Employee(1, "Wolf Fink", "wf@redhat.com", 127, c));
    c.getEmployees().add(new Employee(2, "William", "m@redhat.com", 17, c));

    printCompany(c);

    companyCache.put(1, c);
  }

  private void findCompanies() {
    QueryFactory qf = Search.getQueryFactory(companyCache);

    runIckleQuery4Company(qf, "c.isStockCompany = false");
    runIckleQuery4Company(qf, "c.employee.name = 'Wolf Fink'");
    runIckleQuery4Company(qf, "c.employee.age > 100");
    // example for boolean query; without "=true" it might have not the expected result
    runIckleQuery4Company(qf, "c.employee.engaged = true");
    // example for embedded object check; which is not as expected due to https://issues.jboss.org/browse/ISPN-9766
    runIckleQuery4Company(qf, "c.employee is not empty");
    runIckleQuery4Company(qf, "c.employee is empty");
    // example of differences between Lucene and RDBMS queries
    // this query return "RedHat" because the relation of employee name AND age is lost
    // because the engine store a flat structure
    runIckleQuery4Company(qf, "c.employee.name = 'Wolf Fink' and c.employee.age < 100");
    runIckleQuery4Company(qf, "c.employee.name in ('William') and c.name = 'JBoss'");
  }

  private void removeNonStockCompanyIds() {
    QueryFactory qf = Search.getQueryFactory(companyCache);

    // as Infinispan 11 implements generics for query a single line will not work as before, so the projection needs to be adjusted
    // first this is a long example to understand the process in detail

    // Option 1 to get the correct result :
    List<Object[]> keys = qf.<Object[]>create("select c.id from playground.Company c where c.isStockCompany = false").execute().list();

    // Option 2 :
    // Query<Object[]> query = qf.create("...");
    // List<Object[]> list = query.execute().list();

    // Option 3 :
    // List<Object[]> list = (List<Object[]>) qf.create("...").execute().list();

    Set<Integer> deleteList = new HashSet<>();
    for (Object[] key : keys) {
      deleteList.add((Integer) key[0]);
    }
    System.out.println("Old style result is " + deleteList);

    // now an example which uses streams to create the list in one line

    deleteList = qf.<Object[]>create("select c.id from playground.Company c where c.isStockCompany = false").execute().list().stream().map(row -> (Integer) row[0]).collect(Collectors.toSet());
    System.out.println("Stream result is " + deleteList); // this result will be the same as the "Old style" before

    companyCache.keySet().removeAll(deleteList);
  }

  private void stop() {
    remoteCacheManager.stop();
  }

  public static void main(String[] args) {
    String host = "localhost";
    String port = "11222";
    String cacheName = "IcklePlayCompanyCache";

    if (args.length > 0) {
      port = args[0];
    }
    if (args.length > 1) {
      port = args[1];
    }
    if (args.length > 2) {
      cacheName = args[2];
    }
    CompanyQueryHotRodClient client = new CompanyQueryHotRodClient(host, port, cacheName);

//  client.insertCompanies();
    client.insertCompanyCircular();
    client.findCompanies();

//    client.removeNonStockCompanyIds();
//    client.findCompanies();

    client.stop();
    System.out.println("\nDone !");
  }
}
