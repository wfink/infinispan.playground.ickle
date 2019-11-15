package org.infinispan.wfink.playground.ickle.hotrod.marshaller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Company;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Employee;

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
