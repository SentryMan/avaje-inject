package io.avaje.inject.generator;

import io.avaje.prism.GenerateAPContext;
import io.avaje.prism.GenerateUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import static io.avaje.inject.generator.APContext.typeElement;
import static io.avaje.inject.generator.ProcessingContext.*;

@GenerateUtils
@GenerateAPContext
@SupportedAnnotationTypes({
  AssistFactoryPrism.PRISM_TYPE,
  InjectModulePrism.PRISM_TYPE,
  FactoryPrism.PRISM_TYPE,
  SingletonPrism.PRISM_TYPE,
  ComponentPrism.PRISM_TYPE,
  PrototypePrism.PRISM_TYPE,
  ScopePrism.PRISM_TYPE,
  Constants.TESTSCOPE,
  Constants.CONTROLLER,
  ImportPrism.PRISM_TYPE,
  AspectImportPrism.PRISM_TYPE,
  QualifierPrism.PRISM_TYPE
})
public final class Processor extends AbstractProcessor {

  private Elements elementUtils;
  private ScopeInfo defaultScope;
  private AllScopes allScopes;
  private boolean readModuleInfo;
  private final Set<String> pluginFileProvided = new HashSet<>();
  private final Set<String> moduleFileProvided = new HashSet<>();

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    loadProvidedFiles(processingEnv.getFiler());
    ProcessingContext.init(processingEnv, moduleFileProvided);
    this.elementUtils = processingEnv.getElementUtils();
    this.allScopes = new AllScopes();
    this.defaultScope = allScopes.defaultScope();
    ExternalProvider.registerPluginProvidedTypes(defaultScope);
    pluginFileProvided.forEach(defaultScope::pluginProvided);
  }

  /**
   * Loads provider files generated by avaje-inject-maven-plugin
   */
  void loadProvidedFiles(Filer filer) {
    pluginFileProvided.addAll(lines(filer, "target/avaje-plugin-provides.txt", "/target/classes"));
    moduleFileProvided.addAll(lines(filer, "target/avaje-module-provides.txt", "/target/classes"));
    pluginFileProvided.addAll(lines(filer, "build/avaje-plugin-provides.txt", "/build/classes/java/main"));
    moduleFileProvided.addAll(lines(filer, "build/avaje-module-provides.txt", "/build/classes/java/main"));
  }

  private static List<String> lines(Filer filer, String relativeName, String replace) {
    try {
      final String resource = resource(filer, relativeName, replace);
      try (var inputStream = new URI(resource).toURL().openStream();
           var reader = new BufferedReader(new InputStreamReader(inputStream))) {
        return reader.lines().collect(Collectors.toList());
      }
    } catch (final Exception e) {
      return Collections.emptyList();
    }
  }

  private static String resource(Filer filer, String relativeName, String replace) throws IOException {
    return filer
      .getResource(StandardLocation.CLASS_OUTPUT, "", relativeName)
      .toUri()
      .toString()
      .replace(replace, "");
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    APContext.setProjectModuleElement(annotations, roundEnv);
    readModule(roundEnv);

    final var processingOver = roundEnv.processingOver();
    ProcessingContext.processingOver(processingOver);

    readBeans(delayedElements());
    addImportedAspects(importedAspects(roundEnv));
    maybeElements(roundEnv, QualifierPrism.PRISM_TYPE).stream()
      .flatMap(Set::stream)
      .flatMap(e -> ElementFilter.methodsIn(e.getEnclosedElements()).stream())
      .forEach(this::validateQualifier);

    maybeElements(roundEnv, ScopePrism.PRISM_TYPE).ifPresent(this::readScopes);
    maybeElements(roundEnv, FactoryPrism.PRISM_TYPE).ifPresent(this::readFactories);

    if (defaultScope.includeSingleton()) {
      maybeElements(roundEnv, SingletonPrism.PRISM_TYPE).ifPresent(this::readBeans);
    }
    maybeElements(roundEnv, ComponentPrism.PRISM_TYPE).ifPresent(this::readBeans);
    maybeElements(roundEnv, PrototypePrism.PRISM_TYPE).ifPresent(this::readBeans);

    readImported(importedElements(roundEnv));

    maybeElements(roundEnv, Constants.CONTROLLER).ifPresent(this::readBeans);
    maybeElements(roundEnv, ProxyPrism.PRISM_TYPE).ifPresent(this::readBeans);
    maybeElements(roundEnv, AssistFactoryPrism.PRISM_TYPE).ifPresent(this::readAssisted);

    allScopes.readBeans(roundEnv);
    defaultScope.write(processingOver);
    allScopes.write(processingOver);

    if (processingOver) {
      ProcessingContext.clear();
    }
    return false;
  }

  private void validateQualifier(ExecutableElement method) {
    var type = APContext.asTypeElement(method.getReturnType());
    if (type == null || type.getKind() != ElementKind.ANNOTATION_TYPE) {
      return;
    }

    var enclosedMethods = ElementFilter.methodsIn(type.getEnclosedElements());
    if (enclosedMethods.size() > 1) {
      APContext.logError(method, "Qualifier annotation members can only have a single attribute");
    }
    enclosedMethods.forEach(this::validateQualifier);
  }

  // Optional because these annotations are not guaranteed to exist
  private static Optional<? extends Set<? extends Element>> maybeElements(RoundEnvironment round, String name) {
    return Optional.ofNullable(typeElement(name)).map(round::getElementsAnnotatedWith);
  }

  private Set<TypeElement> importedElements(RoundEnvironment roundEnv) {
    return maybeElements(roundEnv, ImportPrism.PRISM_TYPE).stream()
      .flatMap(Set::stream)
      .map(ImportPrism::getInstanceOn)
      .flatMap(p -> p.value().stream())
      .map(ProcessingContext::asElement)
      .filter(this::notAlreadyProvided)
      .collect(Collectors.toSet());
  }

  private boolean notAlreadyProvided(TypeElement e) {
    final String type = e.getQualifiedName().toString();
    return !moduleFileProvided.contains(type) && !pluginFileProvided.contains(type);
  }

  private static Map<String, AspectImportPrism> importedAspects(RoundEnvironment roundEnv) {
    return maybeElements(roundEnv, AspectImportPrism.PRISM_TYPE).stream()
      .flatMap(Set::stream)
      .map(AspectImportPrism::getAllInstancesOn)
      .flatMap(List::stream)
      .collect(Collectors.toMap(p -> p.value().toString(), p -> p));
  }

  private void readScopes(Set<? extends Element> scopes) {
    for (final Element element : scopes) {
      if ((element.getKind() == ElementKind.ANNOTATION_TYPE) && (element instanceof TypeElement)) {
        final var type = (TypeElement) element;
        allScopes.addScopeAnnotation(type);
      }
    }
    addTestScope();
  }

  /**
   * Add built-in test scope for <code>@TestScope</code> if available.
   */
  private void addTestScope() {
    final var testScopeType = elementUtils.getTypeElement(Constants.TESTSCOPE);
    if (testScopeType != null) {
      allScopes.addScopeAnnotation(testScopeType);
    }
  }

  private void readFactories(Set<? extends Element> beans) {
    readChangedBeans(ElementFilter.typesIn(beans), true, false);
  }

  private void readAssisted(Set<? extends Element> beans) {
    ElementFilter.typesIn(beans).forEach(t -> {
      var reader = new AssistBeanReader(t);
      try {
        new SimpleAssistWriter(reader).write();
      } catch (IOException e) {
        e.printStackTrace();
      }
    });
  }

  private void readBeans(Set<? extends Element> beans) {
    readChangedBeans(ElementFilter.typesIn(beans), false, false);
  }

  private void readImported(Set<? extends Element> beans) {
    readChangedBeans(ElementFilter.typesIn(beans), false, true);
  }

  /**
   * Read the beans that have changed.
   */
  private void readChangedBeans(Set<TypeElement> beans, boolean factory, boolean importedComponent) {
    for (final var typeElement : beans) {
      if (typeElement.getKind() == ElementKind.INTERFACE) {
        continue;
      }
      final var scope = findScope(typeElement);
      if (!factory) {
        // will be found via custom scope so effectively ignore additional @Singleton
        if (scope == null) {
          defaultScope.read(typeElement, false, importedComponent);
        }
      } else if (scope != null) {
        scope.read(typeElement, true, false);
      } else {
        defaultScope.read(typeElement, true, false);
      }
    }
  }

  /**
   * Find the scope if the Factory has a scope annotation.
   */
  private ScopeInfo findScope(Element element) {
    for (final AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
      final var scopeInfo = allScopes.get(annotationMirror.getAnnotationType().toString());
      if (scopeInfo != null) {
        return scopeInfo;
      }
    }
    return null;
  }

  /**
   * Read the existing meta-data from InjectModule (if found) and the factory bean (if exists).
   */
  private void readModule(RoundEnvironment roundEnv) {
    if (readModuleInfo) {
      // only read the module meta data once
      return;
    }
    readModuleInfo = true;
    final var factory = loadMetaInfServices();
    if (factory != null) {
      final var moduleType = elementUtils.getTypeElement(factory);
      if (moduleType != null) {
        defaultScope.readModuleMetaData(moduleType);
      }
    }
    allScopes.readModules(loadMetaInfCustom());
    readInjectModule(roundEnv);
  }

  /**
   * Read InjectModule for things like package-info etc (not for custom scopes)
   */
  private void readInjectModule(RoundEnvironment roundEnv) {
    // read other that are annotated with InjectModule
    maybeElements(roundEnv, InjectModulePrism.PRISM_TYPE).stream()
      .flatMap(Set::stream)
      .forEach(element -> {
        final var scope = ScopePrism.getInstanceOn(element);
        if (scope == null) {
          // it it not a custom scope annotation
          final var annotation = InjectModulePrism.getInstanceOn(element);
          if (annotation != null) {
            defaultScope.details(annotation.name(), element);
            context.setAutoProvideStrategy(AutoProvideStrategy.valueOf(annotation.autoProvideStrategy()));
          }
        }
      });
  }
}
