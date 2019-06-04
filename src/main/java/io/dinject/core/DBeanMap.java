package io.dinject.core;

import io.dinject.BeanEntry;

import javax.inject.Named;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.dinject.BeanEntry.NORMAL;
import static io.dinject.BeanEntry.PRIMARY;
import static io.dinject.BeanEntry.SECONDARY;
import static io.dinject.BeanEntry.SUPPLIED;

/**
 * Map of types (class types, interfaces and annotations) to a DContextEntry where the
 * entry holds a list of bean instances for that type.
 */
class DBeanMap {

  private final Map<String, DContextEntry> beans = new LinkedHashMap<>();

  /**
   * Create for context builder.
   */
  DBeanMap() {
  }

  /**
   * Add test double supplied beans.
   */
  void add(List<SuppliedBean> suppliedBeans) {
    for (SuppliedBean suppliedBean : suppliedBeans) {
      addSuppliedBean(suppliedBean);
    }
  }

  private void addSuppliedBean(SuppliedBean supplied) {

    Class<?> suppliedType = supplied.getType();
    Named annotation = suppliedType.getAnnotation(Named.class);
    String name = (annotation == null) ? null : annotation.value();

    DContextEntryBean entryBean = DContextEntryBean.of(supplied.getBean(), name, SUPPLIED);
    beans.computeIfAbsent(suppliedType.getCanonicalName(), s -> new DContextEntry()).add(entryBean);
    for (Class<?> anInterface : suppliedType.getInterfaces()) {
      beans.computeIfAbsent(anInterface.getCanonicalName(), s -> new DContextEntry()).add(entryBean);
    }
  }

  void registerPrimary(String canonicalName, Object bean, String name, Class<?>... types) {
    registerWith(PRIMARY, canonicalName, bean, name, types);
  }

  void registerSecondary(String canonicalName, Object bean, String name, Class<?>... types) {
    registerWith(SECONDARY, canonicalName, bean, name, types);
  }

  void register(String canonicalName, Object bean, String name, Class<?>... types) {
    registerWith(NORMAL, canonicalName, bean, name, types);
  }

  void registerWith(int flag, String canonicalName, Object bean, String name, Class<?>... types) {

    DContextEntryBean entryBean = DContextEntryBean.of(bean, name, flag);
    beans.computeIfAbsent(canonicalName, s -> new DContextEntry()).add(entryBean);

    if (types != null) {
      for (Class<?> type : types) {
        beans.computeIfAbsent(type.getName(), s -> new DContextEntry()).add(entryBean);
      }
    }
  }

  /**
   * Return the bean instance given the class and name.
   */
  @SuppressWarnings("unchecked")
  <T> T getBean(Class<T> type, String name) {

    DContextEntry entry = beans.get(type.getCanonicalName());
    if (entry != null) {
      T bean = (T) entry.get(name);
      if (bean != null) {
        return bean;
      }
    }
    return null;
  }

  <T> BeanEntry<T> candidate(Class<T> type, String name) {

    DContextEntry entry = beans.get(type.getCanonicalName());
    if (entry != null) {
      return entry.candidate(name);
    }
    return null;
  }

  /**
   * Add all bean instances matching the given type to the list.
   */
  @SuppressWarnings("unchecked")
  void addAll(Class type, List list) {
    DContextEntry entry = beans.get(type.getCanonicalName());
    if (entry != null) {
      entry.addAll(list);
    }
  }

  /**
   * Return true if there is a supplied bean for this type.
   */
  boolean isSupplied(String type) {
    DContextEntry entry = beans.get(type);
    return entry != null && entry.isSupplied();
  }
}
