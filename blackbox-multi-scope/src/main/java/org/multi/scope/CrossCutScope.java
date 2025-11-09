package org.multi.scope;

import io.avaje.inject.InjectModule;
import javax.inject.Scope;

@Scope
@InjectModule(requires = {ModAScope.class, ModBScope.class})
public @interface CrossCutScope {}
