package org.infinispan.wfink.playground.ickle.hotrod.domain;

import org.infinispan.protostream.GeneratedSchema;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(includeClasses = { Message.class }, schemaFileName = "message.proto", schemaPackageName = "playground")
public interface MessageInitializer extends GeneratedSchema {
}
