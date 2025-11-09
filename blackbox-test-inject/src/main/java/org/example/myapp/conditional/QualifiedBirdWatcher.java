package org.example.myapp.conditional;

import io.avaje.inject.RequiresBean;
import javax.inject.Singleton;

@Singleton
@RequiresBean(qualifiers = "finch")
public class QualifiedBirdWatcher {

  private final Bird bird;

  QualifiedBirdWatcher(Bird bird) {
    this.bird = bird;
  }

  public void watch() {
    System.out.println("watching " + bird);
  }
}
