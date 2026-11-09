package org.example.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import io.avaje.inject.test.InjectTest;
import jakarta.inject.Inject;

@InjectTest
class MainClassTest {

  @Inject HelloService helloService;

  @Test
  void beanWired() {
    assertEquals("hello gradle", helloService.hello());
  }

  @Test
  void gradlePluginFilesInBuildDir() throws Exception {
    Path buildDir = Path.of(System.getProperty("buildDir"));
    // written by the gradle plugin, checked by the processor for @MainClass
    assertTrue(Files.exists(buildDir.resolve("avaje-plugins.csv")));
    // written by the processor for @MainClass
    assertEquals("org.example.gradle.Main", Files.readString(buildDir.resolve("avaje-main-class.txt")).trim());
  }

  @Test
  void mainRuns() {
    Main.main(new String[0]);
  }
}
