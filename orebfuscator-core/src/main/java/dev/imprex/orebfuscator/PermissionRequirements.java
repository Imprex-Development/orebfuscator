package dev.imprex.orebfuscator;

import java.util.Optional;
import java.util.OptionalInt;

public record PermissionRequirements(OptionalInt operatorLevel, Optional<String> permission) {

  public static final PermissionRequirements BYPASS = new PermissionRequirements(OptionalInt.empty(), Optional.of("orebfuscator.bypass"));
  public static final PermissionRequirements ADMIN = new PermissionRequirements(OptionalInt.of(4), Optional.of("orebfuscator.admin"));
  
  public PermissionRequirements {
    if (operatorLevel.isEmpty() && permission.isEmpty()) {
      throw new IllegalArgumentException("Either an operator level, permission or both have to defined");
    }
  }
}
