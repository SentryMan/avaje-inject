package org.multi.scope;

import io.avaje.inject.InjectModule;
import javax.inject.Scope;

@Scope
@InjectModule(
  requires = Mod1Scope.class
)
public @interface Mod2Scope {}
