package org.infinispan.wfink.playground.ickle.hotrod.marshaller;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Employee;

public class EmployeeMarshaller implements MessageMarshaller<Employee> {

  @Override
  public String getTypeName() {
    return "playground.Employee";
  }

  @Override
  public Class<Employee> getJavaClass() {
    return Employee.class;
  }

  @Override
  public Employee readFrom(ProtoStreamReader reader) throws IOException {
    int id = reader.readInt("id");
    String name = reader.readString("name");
    String email = reader.readString("email");

    return new Employee(id, name, email);
  }

  @Override
  public void writeTo(ProtoStreamWriter writer, Employee employee) throws IOException {
    writer.writeInt("id", employee.getId());
    writer.writeString("name", employee.getName());
    writer.writeString("email", employee.getEmail());
  }
}
