package dev.imprex.orebfuscator.reflect.accessor;

import java.lang.reflect.Method;

public interface MethodAccessor extends MemberAccessor<Method> {

  Object invoke(Object target, Object... args);

}
