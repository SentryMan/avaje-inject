package org.example.myapp.lifecycle;

import javax.inject.Singleton;

@Singleton
public class GreetingService extends ServiceCreator {
  private String greeting;

  @Override
  public void setup() {
    greeting = "Hello from GreetingService!";
  }

  public String getGreeting() {
    return greeting;
  }
}
