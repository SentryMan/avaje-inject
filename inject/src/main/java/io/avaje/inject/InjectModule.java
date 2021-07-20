package io.avaje.inject;

/**
 * Used to explicitly specify if it depends on externally provided beans or provides.
 * <p>
 */
public @interface InjectModule {

  /**
   * Explicitly specify the name of the module.
   */
  String name() default "";

  /**
   * Explicitly define features that are provided by this module and required by other modules.
   * <p>
   * This is used to order wiring across multiple modules.
   */
  Class<?>[] provides() default {};

  /**
   * The dependencies that are provided externally or by other modules and that are required
   * when wiring this module.
   */
  Class<?>[] requires() default {};

  /**
   * Internal use only - identifies the custom scope annotation associated to this module.
   * <p>
   * When a module is generated for a custom scope this is set to link the module back to the
   * custom scope annotation and support partial compilation.
   */
  String customScopeType() default "";

}
