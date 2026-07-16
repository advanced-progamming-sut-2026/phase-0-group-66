package model;

import java.util.List;

public class ConeheadZombie extends Zombie {
    public ConeheadZombie(ZombieDefinition definition, List<Armor> armors) {
        super(definition, armors);
    }

    public int getArmorHealth() {
        int total = 0;
        for (Armor armor : getArmors()) {
            total += armor.getHealth();
        }
        return total;
    }
}
