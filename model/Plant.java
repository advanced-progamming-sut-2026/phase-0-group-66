public abstract class Plant {
    protected String name;
    protected int health;
    protected int sunCost;
    protected int row;
    protected int col;
    protected int attackPower;
    protected int cooldown;

    public void attack() {
    }

    public void takeDamage(int amount) {
    }

    public void usePlantFood() {
    }

    public boolean isAvailable() {
        return false;
    }
}
