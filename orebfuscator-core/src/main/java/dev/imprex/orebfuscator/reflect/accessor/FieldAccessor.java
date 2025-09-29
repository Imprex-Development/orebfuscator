package dev.imprex.orebfuscator.reflect.accessor;

import java.lang.reflect.Field;

public interface FieldAccessor extends MemberAccessor<Field> {

  boolean readonly();

  Object get(Object instance);

  void set(Object instance, Object value);

}
