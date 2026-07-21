package model;

public final class GenericPlant extends Plant {
    public GenericPlant(PlantDefinition definition) {
        this(definition, 1);
    }

    public GenericPlant(PlantDefinition definition, int level) {
        super(definition, level);
    }
}
