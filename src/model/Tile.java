import java.util.*;

public class Tile {
    private int row;
    private int col;
    private String tileType;
    private Plant plant;
    private List<Zombie> zombies;

    public boolean canPlant() {
        return false;
    }

    public void addZombie(Zombie zombie) {
    }

    public void removeZombie(Zombie zombie) {
    }

    public boolean isEmpty() {
        return false;
    }
}
