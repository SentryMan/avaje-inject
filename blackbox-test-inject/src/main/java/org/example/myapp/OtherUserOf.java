package org.example.myapp;

import javax.inject.Singleton;

@Singleton
class OtherUserOf {

  final OtherService otherService;

  OtherUserOf(OtherService otherService) {
    this.otherService = otherService;
  }
}
