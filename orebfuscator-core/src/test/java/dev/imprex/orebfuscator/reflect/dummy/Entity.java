package dev.imprex.orebfuscator.reflect.dummy;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Entity implements Id {

  public static final String DEFAULT_WOLRD = "default";

  private static int nextEntityId = 0;

  public static int nextId() {
    return nextEntityId++;
  }

  protected final int entityId;

  private volatile String world;
  private int position;

  public Entity(String world) {
    this.entityId = nextId();
    this.world = world;
  }

  @Override
  public final int id() {
    return entityId;
  }

  public String getWorld() {
    return world;
  }

  public int getPosition() {
    return position;
  }

  public synchronized boolean teleport(String world, int position) throws IOException, NullPointerException {
    Objects.requireNonNull(world);

    try (InputStream inputStream = Files.newInputStream(Paths.get(world))) {
      return position == 0;
    }
  }

  public void destroy() throws NullPointerException, ClassNotFoundException {
    this.world = null;
  }

  protected void move(int offset) {
    this.position += offset;
  }
}
