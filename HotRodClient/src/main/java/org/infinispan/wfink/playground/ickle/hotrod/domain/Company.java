package org.infinispan.wfink.playground.ickle.hotrod.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Cache entry class without annotation. A .proto file need to define the fields and a Marshaller is needed as well. Note that the sequence of fields need to have the correct order in .proto and Marshaller for best performance. A
 * PROTOSTREAM warning will be logged otherwise.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
public class Company {

  private int id;
  private String name;
  private List<Employee> employees;
  private Boolean isStockCompany = Boolean.FALSE;
  private Date created;

  public Company() {
    employees = new ArrayList<Employee>();
    created = new Date();
  }

  public Company(int id, String name, Boolean isStockCompany) {
    this();
    this.id = id;
    this.name = name;
    this.isStockCompany = isStockCompany;
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

  public List<Employee> getEmployees() {
    return employees;
  }

  public void setEmployees(List<Employee> employees) {
    this.employees = employees;
  }

  public Boolean getIsStockCompany() {
    return isStockCompany;
  }

  public void setIsStockCompany(Boolean isStockCompany) {
    this.isStockCompany = isStockCompany;
  }

  public Date getCreated() {
    return created;
  }

  public void setCreated(Date created) {
    this.created = created;
  }

  @Override
  public String toString() {
    return "Company [id=" + id + ", name=" + name + ", employees=" + employees + ", isStockCompany=" + isStockCompany + ", created=" + created + "]";
  }
}
