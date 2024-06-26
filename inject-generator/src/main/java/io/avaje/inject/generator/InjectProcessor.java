package io.avaje.inject.generator;

import io.avaje.prism.GenerateAPContext;
import io.avaje.prism.GenerateModuleInfoReader;
import io.avaje.prism.GenerateUtils;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.avaje.inject.generator.APContext.*;
import static io.avaje.inject.generator.ProcessingContext.*;

@GenerateUtils
@GenerateAPContext
@GenerateModuleInfoReader
@SupportedAnnotationTypes({
  AspectImportPrism.PRISM_TYPE,
  AssistFactoryPrism.PRISM_TYPE,
  ComponentPrism.PRISM_TYPE,
  Constants.TESTSCOPE,
  Constants.CONTROLLER,
  ExternalPrism.PRISM_TYPE,
  FactoryPrism.PRISM_TYPE,
  ImportPrism.PRISM_TYPE,
  InjectModulePrism.PRISM_TYPE,
  PrototypePrism.PRISM_TYPE,
  QualifierPrism.PRISM_TYPE,
  ScopePrism.PRISM_TYPE,
  SingletonPrism.PRISM_TYPE,
  "io.avaje.spi.ServiceProvider"
})
public final class InjectProcessor extends AbstractProcessor {

  private Elements elementUtils;
  private ScopeInfo defaultScope;
  private AllScopes allScopes;
  private boolean readModuleInfo;
  private final Set<String> pluginFileProvided = new HashSet<>();
  private final Set<String> moduleFileProvided = new HashSet<>();
  private boolean performModuleValidation;

  @Override
  public SourceVersion getSupportedSourceVersion() {
    return SourceVersion.latest();
  }

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    APContext.init(processingEnv);
    loadProvidedFiles();
    ProcessingContext.init(moduleFileProvided, performModuleValidation);
    loadOrderFiles();
    this.elementUtils = processingEnv.getElementUtils();
    this.allScopes = new AllScopes();
    this.defaultScope = allScopes.defaultScope();
    ExternalProvider.registerPluginProvidedTypes(defaultScope);
    pluginFileProvided.forEach(defaultScope::pluginProvided);
  }

  /**
   * Loads provider files generated by avaje-inject-maven-plugin
   */
  void loadProvidedFiles() {
    this.performModuleValidation =
      lines("target/avaje-plugin-exists.txt").isEmpty()
        && lines("build/avaje-plugin-exists.txt").isEmpty();
    pluginFileProvided.addAll(lines("target/avaje-plugin-provides.txt"));
    moduleFileProvided.addAll(lines("target/avaje-module-provides.txt"));
    pluginFileProvided.addAll(lines("build/avaje-plugin-provides.txt"));
    moduleFileProvided.addAll(lines("build/avaje-module-provides.txt"));
  }

  /**
   * Loads order files generated by avaje-inject-maven-plugin
   */
  private void loadOrderFiles() {
    Stream.concat(
        lines("target/avaje-module-dependencies.csv").stream().skip(1),
        lines("build/avaje-module-dependencies.csv").stream().skip(1))
      .filter(s -> !s.startsWith("External Module Type"))
      .distinct()
      .map(l -> l.split("\\|"))
      .map(ModuleData::new)
      .forEach(ProcessingContext::addModule);
  }

  private List<String> lines(String relativeName) {
    try {
      final String resource =
        processingEnv
          .getFiler()
          .getResource(StandardLocation.CLASS_OUTPUT, "", relativeName)
          .toUri()
          .toString()
          .replaceFirst("/target/classes", "")
          .replaceFirst("/build/classes/java/main", "");
      return Files.readAllLines(Paths.get(new URI(resource)));
    } catch (final Exception e) {
      return Collections.emptyList();
    }
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

    maybeElements(roundEnv, ExternalPrism.PRISM_TYPE).stream()
      .flatMap(Set::stream)
      .map(Element::asType)
      .map(UType::parse)
      .map(u -> "java.util.List".equals(u.mainType()) ? u.param0() : u)
      .map(UType::fullWithoutAnnotations)
      .forEach(ProcessingContext::addOptionalType);

    maybeElements(roundEnv, "io.avaje.spi.ServiceProvider").ifPresent(this::registerSPI);
    allScopes.readBeans(roundEnv);
    defaultScope.write(processingOver);
    allScopes.write(processingOver);

    if (processingOver) {
      var order =
        new FactoryOrder(ProcessingContext.modules(), defaultScope.pluginProvided())
          .orderModules();

      if (ProcessingContext.strictWiring()) {
        try {
          new SimpleOrderWriter(order, defaultScope).write();
        } catch (IOException e) {
          logError("FilerException trying to write wiring order class " + e.getMessage());
        }
      }
      ProcessingContext.writeSPIServicesFile();
      ProcessingContext.validateModule();
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

  /** Read InjectModule for things like package-info etc (not for custom scopes) */
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
            ProcessingContext.strictWiring(annotation.strictWiring());
          }
        }
      });
  }

  private void registerSPI(Set<? extends Element> beans) {
    ElementFilter.typesIn(beans).stream()
      .filter(InjectProcessor::isInjectExtension)
      .map(TypeElement::getQualifiedName)
      .map(Object::toString)
      .forEach(ProcessingContext::addInjectSPI);
  }

  private static boolean isInjectExtension(TypeElement te) {
    return te.getInterfaces().stream()
      .map(TypeMirror::toString)
      .anyMatch(EXTENSION_TYPES::contains);
  }

  private static final Set<String> EXTENSION_TYPES = Set.of(
    "io.avaje.inject.spi.ModuleOrdering",
    "io.avaje.inject.spi.AvajeModule",
    "io.avaje.inject.spi.InjectPlugin",
    "io.avaje.inject.spi.ConfigPropertyPlugin",
    "io.avaje.inject.spi.PropertyRequiresPlugin"
  );

}
