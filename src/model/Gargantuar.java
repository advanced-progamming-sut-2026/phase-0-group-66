package model;

import java.util.List;

public class Gargantuar extends Zombie {
    private boolean impReady;

    public Gargantuar(ZombieDefinition definition, List<Armor> armors) {
        super(definition, armors);
        impReady = true;
    }

    public void smashPlant(Plant plant) {
        if (plant != null) {
            plant.takeDamage(Math.max(plant.getHealth(), damage));
        }
    }

    public void throwImp() {
        impReady = false;
    }

    public boolean isImpReady() {
        return impReady;
    }
}
