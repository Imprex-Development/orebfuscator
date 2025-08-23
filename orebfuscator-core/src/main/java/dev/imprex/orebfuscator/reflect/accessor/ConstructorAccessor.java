package dev.imprex.orebfuscator.reflect.accessor;

import java.lang.reflect.Constructor;

public interface ConstructorAccessor extends MemberAccessor<Constructor<?>> {

  Object invoke(Object... args);

}
