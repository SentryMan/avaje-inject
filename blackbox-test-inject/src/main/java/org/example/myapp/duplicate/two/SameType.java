package org.example.myapp.duplicate.two;

import javax.inject.Singleton;

@Singleton
public class SameType {
  @Singleton
  public static class Inner {}
}
