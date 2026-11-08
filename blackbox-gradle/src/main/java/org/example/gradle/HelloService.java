package org.example.gradle;

import jakarta.inject.Singleton;

@Singleton
public class HelloService {

  public String hello() {
    return "hello gradle";
  }
}
