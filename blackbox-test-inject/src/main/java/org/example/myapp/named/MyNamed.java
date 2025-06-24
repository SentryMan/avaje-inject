package org.example.myapp.named;

import io.avaje.inject.Component;
import io.avaje.inject.PreDestroy;
import javax.inject.Named;
import org.example.myapp.MyDestroyOrder;

@Named("my-name-with-hyphens")
@Component
public class MyNamed {

  @PreDestroy(priority = 999)
  public void close() {
    MyDestroyOrder.add("MyNamed");
  }
}
