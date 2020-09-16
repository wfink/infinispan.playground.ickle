package org.infinispan.wfink.playground.ickle.hotrod.listener;

import org.infinispan.query.api.continuous.ContinuousQueryListener;
import org.infinispan.wfink.playground.ickle.hotrod.domain.Message;

public class MessageContinuousQueryListener implements ContinuousQueryListener<String, Message> {
  private final String readerName;

  public MessageContinuousQueryListener(String readerName) {
    super();
    this.readerName = readerName;
  }

  @Override
  public void resultJoining(String key, Message value) {
    System.out.printf("NEW   message for %s to read  : %s \n", readerName, value);
  }

  @Override
  public void resultUpdated(String key, Message value) {
    System.out.printf("UPDATED message for %s to read  : %s \n", readerName, value);
  }

  @Override
  public void resultLeaving(String key) {
    System.out.printf("DELETED message for %s         : %s \n", readerName, key);
  }

  @Override
  public String toString() {
    return "MessageContinuousQueryListener [readerName=" + readerName + "]";
  }

}
