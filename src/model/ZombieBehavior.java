package model;

/** Runtime strategy for a zombie's periodic special ability. */
@FunctionalInterface
public interface ZombieBehavior {
    void perform(Game game, Zombie zombie);
}
