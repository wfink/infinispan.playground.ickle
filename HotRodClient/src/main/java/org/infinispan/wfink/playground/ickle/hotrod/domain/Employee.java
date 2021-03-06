package org.infinispan.wfink.playground.ickle.hotrod.domain;

/**
 * Cache entry class without annotation. A .proto file need to define the fields and a Marshaller is needed as well. Note that the sequence of fields need to have the correct order in .proto and Marshaller for best performance. A PROTOSTREAM warning will be logged otherwise.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
public class Employee {

  private int id;
  private String name;
  private String email;
  private Integer age;
  private boolean isEngaged = false;

  public Employee() {
  }

  public Employee(int id, String name, String email) {
    this(id, name, email, null, true);
  }

  public Employee(int id, String name, String email, Integer age, boolean engaged) {
    this.id = id;
    this.name = name;
    this.email = email;
    this.age = age;
    this.isEngaged = engaged;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Integer getAge() {
    return age;
  }

  public void setAge(Integer age) {
    this.age = age;
  }

  public boolean isEngaged() {
    return isEngaged;
  }

  public void setEngaged(boolean isEngaged) {
    this.isEngaged = isEngaged;
  }

  @Override
  public String toString() {
    return "Person{" + "id=" + id + ", name='" + name + ", age='" + age + ", engaged='" + isEngaged + ", email=" + email + "]";
  }
}
