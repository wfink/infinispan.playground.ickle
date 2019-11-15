package org.infinispan.wfink.playground.ickle.hotrod;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.List;

import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.Search;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.marshall.ProtoStreamMarshaller;
import org.infinispan.protostream.FileDescriptorSource;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.query.dsl.Query;
import org.infinispan.query.dsl.QueryFactory;
import org.infinispan.query.remote.client.ProtobufMetadataManagerConstants;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Company;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Employee;
import org.infinispan.wfink.playground.ickle.hotrod.marshaller.CompanyMarshaller;
import org.infinispan.wfink.playground.ickle.hotrod.marshaller.EmployeeMarshaller;

public class CompanyQueryHotRodClient {
  private static final String PROTOBUF_DEFINITION_COMPANY = "/playground/company.proto";

  private RemoteCacheManager remoteCacheManager;
  private RemoteCache<Integer, Company> companyCache;

  public CompanyQueryHotRodClient(String host, String port, String cacheName) {
    ConfigurationBuilder remoteBuilder = new ConfigurationBuilder();
    remoteBuilder.addServer().host(host).port(Integer.parseInt(port))
    .marshaller(new ProtoStreamMarshaller());  // The Protobuf based marshaller is required for query capabilities

    remoteCacheManager = new RemoteCacheManager(remoteBuilder.build());
    companyCache = remoteCacheManager.getCache(cacheName);

    if (companyCache == null) {
      throw new RuntimeException("Cache '" + cacheName + "' not found. Please make sure the server is properly configured");
    }

    registerSchemasAndMarshallers();
  }

  /**
   * Register the Protobuf schemas and marshallers with the client and then
   * register the schemas with the server too.
   */
  private void registerSchemasAndMarshallers() {
    // Register entity marshallers on the client side ProtoStreamMarshaller
    // instance associated with the remote cache manager.
    SerializationContext ctx = ProtoStreamMarshaller.getSerializationContext(remoteCacheManager);
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


  private void runIckleQuery(QueryFactory qf, String query) {
    Query q = qf.create(query);
    List<Company> results = q.list();
    System.out.printf("Query %s  : found %d matches\n", query, results.size());
    for (Company c : results) {
      System.out.println("   " + c);
      if (c.getEmployees().size() > 0) {
        System.out.println("     Employees:");
        for (Employee e : c.getEmployees()) {
          System.out.println("       " + e);
        }
      }
    }
  }

  private void insertCompanies() {
    System.out.println("Inserting Messages into cache...");
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

  private void findCompanies() {
    QueryFactory qf = Search.getQueryFactory(companyCache);

    runIckleQuery(qf, "from playground.Company c where c.isStockCompany = false");
    runIckleQuery(qf, "from playground.Company c where c.employee.name = 'Wolf Fink'");
    runIckleQuery(qf, "from playground.Company c where c.employee.age > 100");
    // example for boolean query; without "=true" it might have not the expected result
    runIckleQuery(qf, "from playground.Company c where c.employee.engaged = true");
    // example for embedded object check; which is not as expected due to https://issues.jboss.org/browse/ISPN-9766
    runIckleQuery(qf, "from playground.Company c where c.employee is not empty");
    runIckleQuery(qf, "from playground.Company c where c.employee is empty");
    // example of differences between Lucene and RDBMS queries
    // this query return "RedHat" because the relation of employee name AND age is lost
    // because the engine store a flat structure
    runIckleQuery(qf, "from playground.Company c where c.employee.name = 'Wolf Fink' and c.employee.age < 100");
    runIckleQuery(qf, "from playground.Company c where c.employee.name in ('William') and c.name = 'JBoss'");
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

    client.insertCompanies();
    client.findCompanies();

    client.stop();
    System.out.println("\nDone !");
  }
}
