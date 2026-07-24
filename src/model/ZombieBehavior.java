package model;

@FunctionalInterface
public interface ZombieBehavior {
    void perform(Game game, Zombie zombie);
}
