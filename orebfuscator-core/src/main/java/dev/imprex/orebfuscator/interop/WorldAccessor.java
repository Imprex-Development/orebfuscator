package dev.imprex.orebfuscator.interop;

public interface WorldAccessor {

  String getName();

  int getHeight();

  int getMinBuildHeight();

  int getMaxBuildHeight();

  int getSectionCount();

  int getMinSection();

  int getMaxSection();

  int getSectionIndex(int y);
}
