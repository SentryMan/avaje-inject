package org.multi.scope;

import org.multi.crosscut.CrossCutModule;
import org.multi.modd.ModDModule;
import org.multi.mode.ModEModule;
import org.other.one.custom.*;

import io.avaje.inject.InjectModule;
import jakarta.inject.Scope;

@Scope
@InjectModule(requires = {ExternalModule, ModDModule.class, CrossCutModule.class, ModEModule.class})
public @interface ManyScope {}
