package org.infinispan.wfink.playground.ickle.hotrod.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Embedded;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.option.Structure;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * An entity class to be stored in an Infinispan cache for Ickle queries. The annotations are processed by ProtoSchemaBuilder to create and register it for Ickle queries and indexing at server side. Marshallers are generated from the annotations with the Protostream processor with the Maven build
 * process.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
@Indexed
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

  @ProtoField(number = 1, required = true)
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  @Basic
  @ProtoField(number = 2, required = true)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Embedded(includeDepth = 1, structure = Structure.FLATTENED)
  @ProtoField(number = 3)
  public List<Employee> getEmployees() {
    return employees;
  }

  public void setEmployees(List<Employee> employees) {
    this.employees = employees;
  }

  @ProtoField(number = 4, defaultValue = "false")
  public Boolean isStockCompany() {
    return isStockCompany;
  }

  public void setStockCompany(Boolean isStockCompany) {
    this.isStockCompany = isStockCompany;
  }

  @ProtoField(number = 5, required = true)
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
