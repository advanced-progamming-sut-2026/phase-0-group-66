package model;

final class BeghouledCombatPlant {
    String type;
    BeghouledCombatProfile profile;
    int health;
    private int cooldown;

    BeghouledCombatPlant(String type, BeghouledCombatProfile profile) {
        this.type = type;
        this.profile = profile;
        health = profile.maxHealth();
        cooldown = profile.intervalTicks();
    }

    void tick() {
        if (cooldown > 0) {
            cooldown--;
        }
    }

    boolean ready() {
        return cooldown <= 0 && profile.damage() > 0;
    }

    boolean isDead() {
        return health <= 0;
    }

    void damage(int amount) {
        health = Math.max(0, health - Math.max(0, amount));
    }

    void resetCooldown() {
        cooldown = profile.intervalTicks();
    }

    void upgrade(String newType, BeghouledCombatProfile newProfile) {
        double ratio = profile.maxHealth() <= 0 ? 1.0
            : health / (double) profile.maxHealth();
        type = newType;
        profile = newProfile;
        health = Math.max(1, (int) Math.round(newProfile.maxHealth() * ratio));
        cooldown = Math.min(cooldown, newProfile.intervalTicks());
    }
}
