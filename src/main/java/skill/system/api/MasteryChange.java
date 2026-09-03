package skill.system.api;

public record MasteryChange(int oldMastery, int newMastery, int oldRank, int newRank, boolean newlyMastered) { }
