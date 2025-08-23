package dev.imprex.orebfuscator.reflect.accessor;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Member;

public interface MemberAccessor<TMember extends Member> {

  @NotNull TMember member();
}
