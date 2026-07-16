package model;

import java.util.List;

public class BasicZombie extends Zombie {
    private String chapterTag;

    public BasicZombie(ZombieDefinition definition, List<Armor> armors) {
        super(definition, armors);
    }

    @Override
    public void move() {
        super.move();
    }

    public String getChapterTag() {
        return chapterTag;
    }

    public void setChapterTag(String chapterTag) {
        this.chapterTag = chapterTag;
    }
}
