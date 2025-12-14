package dev.imprex.orebfuscator.reflect.accessor;


import java.lang.reflect.Member;
import org.jspecify.annotations.NonNull;

public interface MemberAccessor<TMember extends Member> {

  @NonNull TMember member();
}
