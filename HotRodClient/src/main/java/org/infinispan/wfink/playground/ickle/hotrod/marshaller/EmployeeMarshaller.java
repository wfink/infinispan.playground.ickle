package org.infinispan.wfink.playground.ickle.hotrod.marshaller;

import java.io.IOException;

import org.infinispan.protostream.MessageMarshaller;
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
    Integer age = reader.readInt("age");
    boolean engaged = reader.readBoolean("engaged");

    return new Employee(id, name, email, age, engaged);
  }

  @Override
  public void writeTo(ProtoStreamWriter writer, Employee employee) throws IOException {
    writer.writeInt("id", employee.getId());
    writer.writeString("name", employee.getName());
    writer.writeString("email", employee.getEmail());
    writer.writeInt("age", employee.getAge());
    writer.writeBoolean("engaged", employee.isEngaged());
  }
}
