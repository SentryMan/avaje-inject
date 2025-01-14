package io.avaje.inject.aop;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;
import java.lang.annotation.*;

import io.avaje.inject.aop.Aspect.Import.Imports;

/**
 * Meta annotation used to define an Aspect.
 *
 * <p>Create an annotation and annotate with {@code @Aspect} to define an aspect annotation. The
 * associated type that implements {@link AspectProvider} will be used as the target class. The
 * aspect provider should be a {@code @Singleton} bean registered with <em>avaje-inject</em>.
 */
@Target(ANNOTATION_TYPE)
@Retention(RUNTIME)
public @interface Aspect {

  /**
   * Specify the priority ordering when multiple aspects are on a method.
   *
   * <p>When multiple aspects are on a method they are nested. The highest ordering value will be
   * the outer-most aspect, the lowest ordering will be the inner-most aspect.
   *
   * <p>The outer-most aspect will have it's <em>before</em> executed first, followed by the
   * <em>before</em> of the inner nested aspects ultimately down the invocation of the target
   * method.
   *
   * <p>The reverse ordering occurs for <em>after</em> with the outer-most aspect having it's
   * <em>after</em> executed last.
   *
   * @return The ordering of this aspect. High value for outer-most aspect.
   */
  int ordering() default 1000;

  /**
   * Marks an External Annotation as being used for aspects
   */
  @Retention(SOURCE)
  @Repeatable(Imports.class)
  @Target({PACKAGE, TYPE, MODULE})
  @interface Import {

    /**
     * Annotation type to import
     */
    Class<? extends Annotation> value();

    /**
     * Specify the priority of the imported aspect.
     *
     * @see Aspect#ordering()
     */
    int ordering() default 1000;

    @Retention(SOURCE)
    @Target({TYPE, PACKAGE, MODULE})
    @interface Imports {

      Import[] value();
    }
  }
}
