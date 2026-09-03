package skill.system;

import legend.game.combat.Battle;
import legend.game.combat.bent.MonsterBattleEntity;
import legend.game.combat.types.CombatantStruct1a8;
import legend.game.combat.ui.BattleAction;
import legend.game.combat.ui.GatherBattleActionsEvent;
import legend.game.combat.ui.RegisterBattleActionsEvent;
import legend.game.inventory.Equipment;
import legend.game.inventory.EquipmentRegistryEvent;
import legend.game.inventory.InventoryEntry;
import legend.game.inventory.ItemStack;
import legend.game.inventory.screens.ShopScreen;
import legend.game.modding.events.battle.BattleEndedEvent;
import legend.game.modding.events.battle.BattleStartedEvent;
import legend.game.modding.events.engine.EngineStateChangeEvent;
import legend.game.modding.events.gamestate.GameLoadedEvent;
import legend.game.modding.events.inventory.ShopContentsEvent;
import legend.game.saves.ConfigEntry;
import legend.game.saves.ConfigRegistryEvent;
import legend.game.textures.RegisterAtlasTexturesEvent;
import org.legendofdragoon.modloader.Mod;
import org.legendofdragoon.modloader.events.EventListener;
import org.legendofdragoon.modloader.registries.Registrar;
import org.legendofdragoon.modloader.registries.RegistryDelegate;
import org.legendofdragoon.modloader.registries.RegistryId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import skill.system.battle.CustomBattleActionBridge;
import skill.system.equipment.StealManualEquipment;
import skill.system.equipment.ManualAtlasIcon;
import skill.system.persistence.SkillMasteryConfig;
import skill.system.steal.EnemyHeldStateStore;
import skill.system.steal.HeldResource;
import skill.system.steal.StealBossRegistry;
import skill.system.steal.StealBattleAction;
import skill.system.steal.StealTargetPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;
import java.util.random.RandomGeneratorFactory;

import static legend.core.GameEngine.EVENTS;
import static legend.core.GameEngine.REGISTRIES;
import static legend.game.Scus94491BpeSegment_8006.battleState_8006e398;
import static legend.game.combat.bent.BattleEntity27c.FLAG_DEAD;

@Mod(id = SkillSystemMod.MOD_ID, version = "3.0.0")
public final class SkillSystemMod {
  public static final String MOD_ID = "skill_system";
  public static final RegistryId SKILLS_MENU_ID = new RegistryId(MOD_ID, "skills");
  public static final String SKILLS_MENU_NAME_KEY = "skill_system.ui.skills";
  private static final Logger LOGGER = LogManager.getFormatterLogger(SkillSystemMod.class);

  private static final Registrar<Equipment, EquipmentRegistryEvent> EQUIPMENT = new Registrar<>(REGISTRIES.equipment, MOD_ID);
  private static final Registrar<ConfigEntry<?>, ConfigRegistryEvent> CONFIGS = new Registrar<>(REGISTRIES.config, MOD_ID);
  private static final Registrar<BattleAction, RegisterBattleActionsEvent> BATTLE_ACTIONS = new Registrar<>(REGISTRIES.battleActions, MOD_ID);

  public static final RegistryDelegate<Equipment> STEAL_MANUAL = EQUIPMENT.register("steal_manual", StealManualEquipment::new);
  public static final RegistryDelegate<SkillMasteryConfig> MASTERY_CONFIG = CONFIGS.register("mastery_data", SkillMasteryConfig::new);
  public static final RegistryDelegate<BattleAction> STEAL_ACTION = BATTLE_ACTIONS.register("steal", StealBattleAction::new);
  public static final EnemyHeldStateStore HELD_RESOURCES = new EnemyHeldStateStore();

  private static RandomGenerator battleRng = RandomGenerator.getDefault();
  private static boolean testMasteryApplied;

  public SkillSystemMod() { EVENTS.register(this); }

  @EventListener public void registerEquipment(final EquipmentRegistryEvent event) { EQUIPMENT.registryEvent(event); }
  @EventListener public void registerConfig(final ConfigRegistryEvent event) { CONFIGS.registryEvent(event); }
  @EventListener public void registerBattleActions(final RegisterBattleActionsEvent event) { BATTLE_ACTIONS.registryEvent(event); }
  @EventListener public void registerAtlasTextures(final RegisterAtlasTexturesEvent event) { ManualAtlasIcon.registerAll(event); }

  @EventListener
  public void engineStateChanged(final EngineStateChangeEvent event) {
    if(event.engineState instanceof final Battle battle) CustomBattleActionBridge.install(battle);
  }

  @EventListener
  public void gatherBattleActions(final GatherBattleActionsEvent event) {
    if(!SkillSystemRuntime.canUseSkill(event.player.character, SkillSystemRuntime.STEAL)) return;

    event.actions.put(STEAL_ACTION.get(), 450);
    final int rank = SkillSystemRuntime.getSkillRank(event.player.character, SkillSystemRuntime.STEAL);
    final List<Boolean> livingBossFlags = new ArrayList<>();
    for(int slot = 0; slot < battleState_8006e398.getMonsterCount(); slot++) {
      final var state = battleState_8006e398.monsterBents_e50[slot];
      if(state != null && !state.hasFlag(FLAG_DEAD)) {
        livingBossFlags.add(StealBossRegistry.isBoss(state.innerStruct_00.charId_272));
      }
    }
    if(StealTargetPolicy.shouldDisableAction(rank, livingBossFlags)) event.disabledActions.add(STEAL_ACTION.get());
  }

  @EventListener
  public void gameLoaded(final GameLoadedEvent event) {
    SkillSystemRuntime.load();
  }

  @EventListener
  public void addForestShopManual(final ShopContentsEvent event) {
    if(!"lod:forest_item_shop".equals(event.shop.getRegistryId().toString())) return;
    final boolean present = event.contents.stream().anyMatch(entry -> entry.item.getRegistryId().toString().equals("skill_system:steal_manual"));
    if(!present) event.contents.add(new ShopScreen.ShopEntry<InventoryEntry<?>>(STEAL_MANUAL.get(), 20));
  }

  @EventListener
  public void battleStarted(final BattleStartedEvent event) {
    HELD_RESOURCES.clear();
    battleRng = RandomGeneratorFactory.<RandomGenerator>of("L64X128MixRandom").create(System.nanoTime() ^ System.identityHashCode(event.battle));
    this.applyTestMasteryIfRequested();
    final Integer forcedGold = SkillSystemDiagnostics.forcedStealGold();
    if(forcedGold != null || SkillSystemDiagnostics.forceStealSuccess()) {
      LOGGER.warn("[SkillSystem] TEST ONLY overrides active: forcedGold=%s forceSuccess=%s",
        forcedGold, SkillSystemDiagnostics.forceStealSuccess());
    }
    for(int slot = 0; slot < battleState_8006e398.getMonsterCount(); slot++) {
      if(battleState_8006e398.monsterBents_e50[slot] == null) continue;
      final MonsterBattleEntity enemy = battleState_8006e398.monsterBents_e50[slot].innerStruct_00;
      final List<HeldResource> candidates = new ArrayList<>();
      for(final CombatantStruct1a8.ItemDrop drop : enemy.combatant_144.drops) {
        if(drop.item() instanceof ItemStack) candidates.add(new HeldResource(HeldResource.ResourceType.ITEM, drop.item(), drop.chance()));
        else if(drop.item() instanceof Equipment) candidates.add(new HeldResource(HeldResource.ResourceType.EQUIPMENT, drop.item(), drop.chance()));
      }
      if(forcedGold != null) {
        HELD_RESOURCES.createForced(enemy, new HeldResource(HeldResource.ResourceType.GOLD, forcedGold, 100));
      } else {
        HELD_RESOURCES.create(enemy, candidates, enemy.combatant_144.gold_196, StealBossRegistry.isBoss(enemy.charId_272), battleRng);
      }
      SkillSystemDiagnostics.log(LOGGER,
        "Held resource generated target=%s charId=%d boss=%s state=%s", enemy.getName(), enemy.charId_272,
        StealBossRegistry.isBoss(enemy.charId_272), HELD_RESOURCES.get(enemy) == null ? null : HELD_RESOURCES.get(enemy).resource());
    }
  }

  private void applyTestMasteryIfRequested() {
    if(testMasteryApplied || !SkillSystemDiagnostics.enabled()) return;
    final String raw = System.getProperty(SkillSystemDiagnostics.TEST_MASTERY_PROPERTY);
    if(raw == null || raw.isBlank()) return;

    final int mastery;
    try {
      mastery = Integer.parseInt(raw);
    } catch(final NumberFormatException e) {
      LOGGER.warn("[SkillSystem] Ignoring invalid test mastery value %s", raw);
      testMasteryApplied = true;
      return;
    }

    for(int slot = 0; slot < battleState_8006e398.getPlayerCount(); slot++) {
      final var state = battleState_8006e398.playerBents_e40.get(slot);
      if(state != null && SkillSystemRuntime.isSkillManualEquipped(state.innerStruct_00.character, SkillSystemRuntime.STEAL)) {
        SkillSystemRuntime.setSkillMasteryForTesting(state.innerStruct_00.character, SkillSystemRuntime.STEAL, mastery);
        LOGGER.warn("[SkillSystem] TEST ONLY: set %s Steal mastery to %d", state.innerStruct_00.getName(),
          SkillSystemRuntime.getSkillMastery(state.innerStruct_00.character, SkillSystemRuntime.STEAL));
      }
    }
    testMasteryApplied = true;
  }

  @EventListener
  public void battleEnded(final BattleEndedEvent event) {
    HELD_RESOURCES.clear();
  }

  public static RandomGenerator battleRng() {
    // Event callbacks and battle actions execute on the game thread; the active instance owns the stream.
    return battleRng;
  }
}
