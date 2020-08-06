package org.infinispan.wfink.playground.ickle.hotrod.marshaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Company;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Employee;

/**
 * Implementation of a Message Marshaller for Protobuf to be used with *.proto files. The Cache entry class is without annotations for Protobuf in this case. All Field and Index configuration is included within the *.proto file. Note that the sequence of fields need to have the correct order in
 * .proto and Marshaller for best performance. A PROTOSTREAM warning will be logged otherwise.
 *
 * As the MessageMarshaller is marked as deprecated it is recommended to use the approach with annotated Java classes and use the ProtoSchemaBuilder to generate the proto information and Marshaller. The MessageMarshaller will not be removed until there is an alternative way.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
@SuppressWarnings("deprecation")
public class CompanyMarshaller implements MessageMarshaller<Company> {

  @Override
  public String getTypeName() {
    return "playground.Company";
  }

  @Override
  public Class<Company> getJavaClass() {
    return Company.class;
  }

  @Override
  public Company readFrom(ProtoStreamReader reader) throws IOException {
    final int id = reader.readInt("id");
    final String name = reader.readString("name");
    final Boolean isStockCompany = reader.readBoolean("isStockCompany");
    final List<Employee> employees = reader.readCollection("employee", new ArrayList<Employee>(), Employee.class);
    final Date created = new Date(reader.readLong("created"));

    Company company = new Company();
    company.setName(name);
    company.setId(id);
    company.setEmployees(employees);
    company.setIsStockCompany(isStockCompany);
    company.setCreated(created);
    return company;
  }

  @Override
  public void writeTo(ProtoStreamWriter writer, Company company) throws IOException {
    writer.writeInt("id", company.getId());
    writer.writeString("name", company.getName());
    writer.writeBoolean("isStockCompany", company.getIsStockCompany());
    writer.writeCollection("employee", company.getEmployees(), Employee.class);
    writer.writeLong("created", company.getCreated().getTime());
  }
}
