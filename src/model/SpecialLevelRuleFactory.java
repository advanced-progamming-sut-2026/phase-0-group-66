package model;

import java.util.EnumMap;
import java.util.Map;

public final class SpecialLevelRuleFactory {
    private static final Map<SpecialLevelType, SpecialLevelRule> RULES = createRules();

    private SpecialLevelRuleFactory() { }

    public static SpecialLevelRule create(SpecialLevelType type) {
        SpecialLevelType actual = type == null ? SpecialLevelType.NORMAL : type;
        SpecialLevelRule rule = RULES.get(actual);
        if (rule == null) {
            throw new IllegalArgumentException("Unsupported special level type: " + actual);
        }
        return rule;
    }

    private static Map<SpecialLevelType, SpecialLevelRule> createRules() {
        EnumMap<SpecialLevelType, SpecialLevelRule> rules = new EnumMap<>(SpecialLevelType.class);
        register(rules, new NormalLevelRule());
        register(rules, new ConveyorBeltRule());
        register(rules, new LockedPlantsRule());
        register(rules, new SaveOurSeedsRule());
        register(rules, new TimedWarRule());
        register(rules, new NightOpsRule());
        register(rules, new DeadLineRule());
        register(rules, new LoveYourPlantsRule());
        register(rules, new PlantWhatYouGetRule());
        return Map.copyOf(rules);
    }

    private static void register(Map<SpecialLevelType, SpecialLevelRule> rules,
                                 SpecialLevelRule rule) {
        rules.put(rule.getType(), rule);
    }
}
