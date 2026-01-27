package studio.fantasyit.maid_rpg_task.task;


import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.*;
import com.github.tartaricacid.touhoulittlemaid.entity.item.EntityExtinguishingAgent;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.DanmakuShoot;
import com.github.tartaricacid.touhoulittlemaid.init.InitEnchantments;
import com.github.tartaricacid.touhoulittlemaid.init.InitItems;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.SpectralArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import studio.fantasyit.maid_rpg_task.Config;
import studio.fantasyit.maid_rpg_task.MaidRpgTask;
import studio.fantasyit.maid_rpg_task.behavior.*;
import studio.fantasyit.maid_rpg_task.compat.PlayerRevive;
import studio.fantasyit.maid_rpg_task.menu.MaidReviveConfigGui;

import java.util.List;
import java.util.function.Predicate;

public class MaidMasterTask implements IRangedAttackTask {
    public static final ResourceLocation UID = new ResourceLocation(MaidRpgTask.MODID, "master_maid");
    private static final int DEFAULT_SEARCH_DISTANCE = 32;
    private static final int RANGED_SEARCH_DISTANCE = 64;

    @Override
    public boolean isEnable(EntityMaid maid) {
        return PlayerRevive.isEnable() && Config.enableMasterTask && Config.enableReviveTask;
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return Items.NETHER_STAR.getDefaultInstance();
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return SoundUtil.attackSound(maid, InitSounds.MAID_ATTACK.get(), 0.5f);
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> supplementedTask = StartAttacking.create(e -> true, IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<EntityMaid> findTargetTask = StopAttackingIfTargetInvalid.create(target -> farAway(target, maid));

        BehaviorControl<Mob> moveToTargetTask = SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.6f);
        BehaviorControl<EntityMaid> strafingTask = new MaidAttackStrafingTask();

        BehaviorControl<Mob> meleeAttackTask = MeleeAttack.create(10);
        BehaviorControl<EntityMaid> bowShootTask = new MaidShootTargetTask(10);
        BehaviorControl<EntityMaid> crossbowShootTask = new MaidCrossbowAttack();

        MaidUseShieldTask maidUseShieldTask = new MaidUseShieldTask();
        BehaviorControl<EntityMaid> tankAggroBehavior = new TankAggroBehavior();
        BehaviorControl<EntityMaid> tankRedirectTask = new TankRedirectBehavior();
        BehaviorControl<EntityMaid> stunBehavior = new StunBehavior();
        BehaviorControl<EntityMaid> playerReviveBehavior = new PlayerReviveBehavior();
        BehaviorControl<EntityMaid> supportEffectBehavior = new SupportEffectBehavior();

        BehaviorControl<EntityMaid> masterModifierTask = new MasterModifierBehavior();
        BehaviorControl<EntityMaid> weaponSwitchTask = new MaidSwitchWeaponBehavior();

        return Lists.newArrayList(
                Pair.of(1, masterModifierTask),
                Pair.of(2, weaponSwitchTask),
                Pair.of(1, playerReviveBehavior),
                Pair.of(5, supportEffectBehavior),
                Pair.of(5, tankAggroBehavior),
                Pair.of(5, tankRedirectTask),
                Pair.of(5, stunBehavior),
                Pair.of(4, strafingTask),
                Pair.of(4, bowShootTask),
                Pair.of(4, crossbowShootTask),

                Pair.of(5, supplementedTask),
                Pair.of(5, findTargetTask),
                Pair.of(5, moveToTargetTask),
                Pair.of(5, meleeAttackTask),
                Pair.of(5, maidUseShieldTask)
        );
    }

    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createRideBrainTasks(EntityMaid maid) {
        BehaviorControl<EntityMaid> supplementedTask = StartAttacking.create(e -> true, IRangedAttackTask::findFirstValidAttackTarget);
        BehaviorControl<EntityMaid> findTargetTask = StopAttackingIfTargetInvalid.create(target -> farAway(target, maid));
        BehaviorControl<Mob> meleeAttackTask = MeleeAttack.create(10);
        BehaviorControl<EntityMaid> bowShootTask = new MaidShootTargetTask(10);
        BehaviorControl<EntityMaid> crossbowShootTask = new MaidCrossbowAttack();

        MaidUseShieldTask maidUseShieldTask = new MaidUseShieldTask();
        BehaviorControl<EntityMaid> tankAggroBehavior = new TankAggroBehavior();
        BehaviorControl<EntityMaid> tankRedirectTask = new TankRedirectBehavior();
        BehaviorControl<EntityMaid> stunBehavior = new StunBehavior();
        BehaviorControl<EntityMaid> playerReviveBehavior = new PlayerReviveBehavior();
        BehaviorControl<EntityMaid> supportEffectBehavior = new SupportEffectBehavior();
        BehaviorControl<EntityMaid> masterModifierTask = new MasterModifierBehavior();
        BehaviorControl<EntityMaid> weaponSwitchTask = new MasterWeaponSwitchBehavior();

        return Lists.newArrayList(
                Pair.of(1, masterModifierTask),
                Pair.of(2, weaponSwitchTask),
                Pair.of(1, playerReviveBehavior),
                Pair.of(5, supportEffectBehavior),
                Pair.of(5, tankAggroBehavior),
                Pair.of(5, tankRedirectTask),
                Pair.of(5, stunBehavior),
                Pair.of(4, bowShootTask),
                Pair.of(4, crossbowShootTask),
                Pair.of(5, supplementedTask),
                Pair.of(5, findTargetTask),
                Pair.of(5, meleeAttackTask),
                Pair.of(5, maidUseShieldTask)
        );
    }


    @Override
    public void performRangedAttack(EntityMaid entityMaid, LivingEntity livingEntity, float v) {
        ItemStack mainHandItem = entityMaid.getMainHandItem();
        if (mainHandItem.getItem() instanceof BowItem) {
            ItemStack arrow = findAndConsumeArrow(entityMaid, mainHandItem);
            if (arrow.isEmpty()) {
                return;
            }
            AbstractArrow entityArrow = ProjectileUtil.getMobArrow(entityMaid, arrow, v);
            entityArrow = ((BowItem) mainHandItem.getItem()).customArrow(entityArrow);
            entityArrow.setBaseDamage(
                    entityArrow.getBaseDamage() * (0.8 + v)
            );

            double x = livingEntity.getX() - entityMaid.getX();
            double y = livingEntity.getEyeY() - entityMaid.getEyeY();
            double z = livingEntity.getZ() - entityMaid.getZ();
            float distance = entityMaid.distanceTo(livingEntity);
            float velocity = Mth.clamp(distance / 10f, 1.6f, 3.2f);
            float inaccuracy = 1 - Mth.clamp(distance / 100f, 0, 0.9f);
            entityArrow.setBaseDamage(entityArrow.getBaseDamage() + 4.0D);
            entityArrow.setNoGravity(true);
            entityArrow.shoot(x, y, z, velocity, inaccuracy);
            mainHandItem.hurtAndBreak(1, entityMaid, (maid) -> maid.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            entityMaid.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (entityMaid.getRandom().nextFloat() * 0.4F + 0.8F));
            entityMaid.level().addFreshEntity(entityArrow);
        } else if (mainHandItem.getItem() instanceof CrossbowItem) {
            ItemStack arrow = findAndConsumeArrow(entityMaid, mainHandItem);
            if (arrow.isEmpty()) {
                return;
            }
            AbstractArrow entityArrow = ProjectileUtil.getMobArrow(entityMaid, arrow, v);
            double x = livingEntity.getX() - entityMaid.getX();
            double y = livingEntity.getEyeY() - entityMaid.getEyeY();
            double z = livingEntity.getZ() - entityMaid.getZ();
            float distance = entityMaid.distanceTo(livingEntity);
            float velocity = Mth.clamp(distance / 8f, 2.6f, 3.6f);
            float inaccuracy = 1 - Mth.clamp(distance / 120f, 0, 0.9f);
            entityArrow.shoot(x, y, z, velocity, inaccuracy);
            mainHandItem.hurtAndBreak(1, entityMaid, (maid) -> maid.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            entityMaid.playSound(SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.0F / (entityMaid.getRandom().nextFloat() * 0.4F + 0.8F));
            entityMaid.level().addFreshEntity(entityArrow);
        } else if (ItemHakureiGohei.isGohei(mainHandItem)) {
            entityMaid.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).ifPresent(livingEntities -> {
                long entityCount = livingEntities.stream().filter(test -> enemyEntityTest(entityMaid, livingEntity, test)).count();
                var level = entityMaid.level();
                AttributeInstance attackDamage = entityMaid.getAttribute(Attributes.ATTACK_DAMAGE);
                float attackValue = 2.0f;
                if (attackDamage != null) {
                    attackValue = (float) attackDamage.getBaseValue();
                }
                int impedingLevel = EnchantmentHelper.getTagEnchantmentLevel(InitEnchantments.IMPEDING.get(), mainHandItem);
                int speedyLevel = EnchantmentHelper.getTagEnchantmentLevel(InitEnchantments.SPEEDY.get(), mainHandItem);
                int multiShotLevel = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.MULTISHOT, mainHandItem);
                int endersEnderLevel = EnchantmentHelper.getTagEnchantmentLevel(InitEnchantments.ENDERS_ENDER.get(), mainHandItem);
                float speed = (0.3f * (v + 1)) * (speedyLevel + 1);
                boolean hurtEnderman = endersEnderLevel > 0;
                float distance = entityMaid.distanceTo(livingEntity);
                speed = speed + Mth.clamp(distance / 40f - 0.4f, 0, 2.4f);
                float inaccuracy = 1 - Mth.clamp(distance / 100f, 0, 0.8f);
                if (entityCount <= 1) {
                    if (multiShotLevel > 0) {
                        DanmakuShoot.create().setWorld(level).setThrower(entityMaid)
                                .setTarget(livingEntity).setRandomColor().setRandomType()
                                .setDamage(attackValue * (v + 1.2f)).setGravity(0)
                                .setVelocity(speed).setHurtEnderman(hurtEnderman)
                                .setInaccuracy(inaccuracy).setFanNum(3).setYawTotal(Math.PI / 12)
                                .setImpedingLevel(impedingLevel)
                                .fanShapedShot();
                    } else {
                        DanmakuShoot.create().setWorld(level).setThrower(entityMaid)
                                .setTarget(livingEntity).setRandomColor().setRandomType()
                                .setDamage(attackValue * (v + 1)).setGravity(0)
                                .setVelocity(speed).setHurtEnderman(hurtEnderman)
                                .setInaccuracy(inaccuracy / 5).setImpedingLevel(impedingLevel)
                                .aimedShot();
                    }
                } else if (entityCount <= 5) {
                    DanmakuShoot.create().setWorld(level).setThrower(entityMaid)
                            .setTarget(livingEntity).setRandomColor().setRandomType()
                            .setDamage(attackValue * (v + 1.2f)).setGravity(0)
                            .setVelocity(speed).setHurtEnderman(hurtEnderman)
                            .setInaccuracy(inaccuracy / 5).setFanNum(8).setYawTotal(Math.PI / 3)
                            .setImpedingLevel(impedingLevel)
                            .fanShapedShot();
                } else {
                    DanmakuShoot.create().setWorld(level).setThrower(entityMaid)
                            .setTarget(livingEntity).setRandomColor().setRandomType()
                            .setDamage(attackValue * (v + 1.5f)).setGravity(0)
                            .setVelocity(speed).setHurtEnderman(hurtEnderman)
                            .setInaccuracy(inaccuracy / 5).setFanNum(32).setYawTotal(2 * Math.PI / 3)
                            .setImpedingLevel(impedingLevel)
                            .fanShapedShot();
                }
                mainHandItem.hurtAndBreak(1, entityMaid, (maid) -> maid.broadcastBreakEvent(InteractionHand.MAIN_HAND));
            });
        }
    }

    public boolean isWeapon(EntityMaid maid, ItemStack stack) {
        return ItemHakureiGohei.isGohei(stack);
    }

    private boolean enemyEntityTest(EntityMaid shooter, LivingEntity target, LivingEntity test) {
        boolean canAttack = shooter.canAttack(test);
        boolean sameType = target.getType().equals(test.getType());
        return canAttack && sameType && shooter.canSee(test);
    }

    @Override
    public boolean canSee(EntityMaid maid, LivingEntity target) {
        // Use distance check + basic visibility to avoid recursion
        return maid.distanceTo(target) <= getSearchRange(maid) && maid.getSensing().hasLineOfSight(target);
    }

    @Override
    public float searchRadius(EntityMaid maid) {
        return getSearchRange(maid);
    }

    @Override
    public AABB searchDimension(EntityMaid maid) {
        return maid.getBoundingBox().inflate(getSearchRange(maid));
    }

    @Override
    public boolean hasExtraAttack(EntityMaid maid, Entity target) {
        return maid.getOffhandItem().is(InitItems.EXTINGUISHER.get()) && target.fireImmune();
    }

    @Override
    public boolean doExtraAttack(EntityMaid maid, Entity target) {
        Level world = maid.level();
        AABB aabb = target.getBoundingBox().inflate(1.5, 1, 1.5);
        List<EntityExtinguishingAgent> extinguishingAgents = world.getEntitiesOfClass(EntityExtinguishingAgent.class, aabb, Entity::isAlive);
        if (extinguishingAgents.isEmpty()) {
            world.addFreshEntity(new EntityExtinguishingAgent(world, target.position()));
            maid.getOffhandItem().hurtAndBreak(1, maid, (m) -> m.broadcastBreakEvent(InteractionHand.OFF_HAND));
            return true;
        }
        return false;
    }

    @Override
    public List<Pair<String, Predicate<EntityMaid>>> getConditionDescription(EntityMaid maid) {
        return Lists.newArrayList(
                Pair.of("master_maid", e -> true),
                Pair.of("assault_weapon", this::hasAssaultWeapon),
                Pair.of("extinguisher", this::hasExtinguisher)
        );
    }

    private boolean hasAssaultWeapon(EntityMaid maid) {
        return isWeapon(maid, maid.getMainHandItem());
    }

    private boolean hasExtinguisher(EntityMaid maid) {
        return maid.getOffhandItem().is(InitItems.EXTINGUISHER.get());
    }


    private boolean farAway(LivingEntity target, EntityMaid maid) {
        return maid.distanceTo(target) > getSearchRange(maid);
    }

    private float getSearchRange(EntityMaid maid) {
        if (hasRangedWeapon(maid)) {
            return RANGED_SEARCH_DISTANCE;
        }
        return DEFAULT_SEARCH_DISTANCE;
    }

    private boolean hasRangedWeapon(EntityMaid maid) {
        if (isRangedWeapon(maid.getMainHandItem())) return true;
        if (isRangedWeapon(maid.getOffhandItem())) return true;

        CombinedInvWrapper handler = maid.getAvailableInv(true);
        for (int i = 0; i < handler.getSlots(); i++) {
            if (isRangedWeapon(handler.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    private boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    private ItemStack findAndConsumeArrow(EntityMaid maid, ItemStack bowItem) {
        boolean hasInfinity = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.INFINITY_ARROWS, bowItem) > 0
                || EnchantmentHelper.getTagEnchantmentLevel(Enchantments.INFINITY_ARROWS, bowItem) > 0;
        CombinedInvWrapper handler = maid.getAvailableInv(true);
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (stack.getItem() instanceof ArrowItem || stack.getItem() instanceof SpectralArrowItem) {
                ItemStack used = stack.copy();
                used.setCount(1);
                if (!hasInfinity) {
                    stack.shrink(1);
                }
                return used;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public MenuProvider getTaskConfigGuiProvider(EntityMaid maid) {
        return new MenuProvider() {
            @Override
            public Component getDisplayName() {
                return Component.literal("");
            }

            @Override
            public AbstractContainerMenu createMenu(int index, Inventory playerInventory, Player player) {
                return new MaidReviveConfigGui.Container(index, playerInventory, maid.getId());
            }
        };
    }
}
