package dev.imprex.orebfuscator.reflect.dummy;

public class Player extends Entity {

  private float health = 20f;

  public Player() {
    this(Entity.DEFAULT_WOLRD);
  }

  public Player(String world) {
    super(world);
  }

  public void heal(float health) {
    health += health;
  }

  public boolean damage(float damage) {
    health -= damage;
    return health > 0;
  }

  @Override
  protected void move(int offset) {
    super.move(offset + 1);
  }
}
