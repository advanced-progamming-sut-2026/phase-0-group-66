package model;

public class SpecialLevel extends Level {
    private final String specialRule;
    private final String rewardDescription;
    private boolean ruleApplied;

    public SpecialLevel(String levelId, SeasonType season, int levelNumber,
                        SpecialLevelType type, String specialRule,
                        String rewardDescription) {
        super(levelId, season, levelNumber, type, 8, 50);
        this.specialRule = specialRule == null ? "" : specialRule.trim();
        this.rewardDescription = rewardDescription == null ? "" : rewardDescription.trim();
    }

    public String getSpecialRule() { return specialRule; }
    public String getRewardDescription() { return rewardDescription; }
    public boolean isRuleApplied() { return ruleApplied; }

    public void applySpecialRule() {
        ruleApplied = true;
    }
}
