package org.infinispan.wfink.playground.ickle.hotrod.domain;

import org.infinispan.api.annotations.indexing.Basic;
import org.infinispan.api.annotations.indexing.Indexed;
import org.infinispan.api.annotations.indexing.Text;
import org.infinispan.protostream.annotations.ProtoField;

/**
 * An entity class to be stored in an Infinispan cache for Ickle queries. The annotations are processed by ProtoSchemaBuilder to create and register it for Ickle queries and indexing at server side.
 *
 * @author <a href="mailto:WolfDieter.Fink@gmail.com">Wolf-Dieter Fink</a>
 */
@Indexed
public class Message {

  private int id;

  private String text;

  private String author;

  private String reader;

  private boolean isRead;

  public Message() {
  }

  public Message(int id, String text, String author, String reader) {
    this.id = id;
    this.text = text;
    this.author = author;
    this.reader = reader;
    this.isRead = false;
  }

  /**
   * A simple field not indexed
   */
  @ProtoField(number = 1, required = true)
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  /**
   * A text field indexed
   */
  @Text(analyzer = "whitespace") // NOTE change the analyzer will need to restart the cache to take effect
  @ProtoField(number = 2)
  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  /**
   * A simple field indexed without analyzing
   */
  @Basic
  @ProtoField(number = 3)
  public String getAuthor() {
    return author;
  }

  public void setAuthor(String author) {
    this.author = author;
  }

  /**
   * A number is always a simple field, here indexed
   */
  @Basic
  @ProtoField(number = 4)
  public String getReader() {
    return reader;
  }

  public void setReader(String reader) {
    this.reader = reader;
  }

  /**
   * A boolean is always a simple field. As it is not nullable it needs a default setting or flagged as required!
   */
  @Basic
  @ProtoField(number = 5, defaultValue = "false")
  public boolean isRead() {
    return isRead;
  }

  public void setRead(boolean isRead) {
    this.isRead = isRead;
  }

  /**
   * Implement a proper hashCode to not have duplicates
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + id;
    return result;
  }

  /**
   * Implement a proper equals to not have duplicates
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Message other = (Message) obj;
    if (id != other.id)
      return false;
    return true;
  }

  @Override
  public String toString() {
    return "Mesage [" + "id=" + id + ", text='" + text + '\'' + ", author=" + author + ", reader=" + reader + ", isRead=" + isRead + ']';
  }
}