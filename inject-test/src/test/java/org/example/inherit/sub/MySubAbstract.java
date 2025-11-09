package org.example.inherit.sub;

import javax.inject.Inject;
import org.example.iface.MySomeNested;

public abstract class MySubAbstract {

  @Inject
  MySomeNested someNested;

  protected MySomeNested someNested() {
    return someNested;
  }
}
