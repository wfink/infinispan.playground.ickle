package org.infinispan.wfink.playground.ickle.hotrod.domain;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Company.class, Employee.class }, schemaFileName = "company.proto", schemaPackageName = "playground")
public interface CompanyInitializer extends GeneratedSchema {
}
