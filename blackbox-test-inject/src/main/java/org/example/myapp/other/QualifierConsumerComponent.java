package org.example.myapp.other;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
final class QualifierConsumerComponent {

  private final String parent;
  private final String child;

  @Inject
  QualifierConsumerComponent(@Named("parent") String parent, @Named("child") String child) {
    this.parent = parent;
    this.child = child;
  }

  String parent() {
    return parent;
  }

  String child() {
    return child;
  }
}
