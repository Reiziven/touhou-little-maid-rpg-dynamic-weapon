package studio.fantasyit.maid_rpg_task.task;

import com.github.tartaricacid.touhoulittlemaid.api.task.IRangedAttackTask;
import com.github.tartaricacid.touhoulittlemaid.config.subconfig.MaidConfig;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.*;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.init.InitSounds;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;
import com.github.tartaricacid.touhoulittlemaid.util.SoundUtil;
import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.behavior.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import studio.fantasyit.maid_rpg_task.MaidRpgTask;
import studio.fantasyit.maid_rpg_task.behavior.*;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.DanmakuShoot;
import com.github.tartaricacid.touhoulittlemaid.init.InitEnchantments;
import net.minecraft.world.item.ItemStack;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidAttackStrafingTask;
import com.github.tartaricacid.touhoulittlemaid.entity.ai.brain.task.MaidShootTargetTask;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.StartAttacking;
import net.minecraft.world.entity.ai.behavior.StopAttackingIfTargetInvalid;

import java.util.List;



public class MaidDPSTask implements IRangedAttackTask {

    public static final ResourceLocation UID =
            new ResourceLocation(MaidRpgTask.MODID, "dps_task");

    private static final int DEFAULT_SEARCH_DISTANCE = 32;
    private static final int RANGED_SEARCH_DISTANCE = 64;

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    @Override
    public ItemStack getIcon() {
        return Items.NETHERITE_SWORD.getDefaultInstance();
    }

    @Override
    public SoundEvent getAmbientSound(EntityMaid maid) {
        return SoundUtil.attackSound(maid, InitSounds.MAID_ATTACK.get(), 0.5f);
    }


    
    @Override
    public List<Pair<Integer, BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid) {

        BehaviorControl<EntityMaid> startAttack =
                StartAttacking.create(e -> true, IRangedAttackTask::findFirstValidAttackTarget);

        BehaviorControl<EntityMaid> stopAttack =
                StopAttackingIfTargetInvalid.create(target -> farAway(target, maid));

        BehaviorControl<Mob> moveToTarget =
                SetWalkTargetFromAttackTargetIfTargetOutOfReach.create(0.6f);

        BehaviorControl<EntityMaid> strafing =
                new MaidAttackStrafingTask();

        BehaviorControl<Mob> meleeAttack =
                MeleeAttack.create(10);

        BehaviorControl<EntityMaid> bowShootTask = new MaidShootTargetTask(10);

        BehaviorControl<EntityMaid> crossbowShoot =
                new MaidCrossbowAttack();

        BehaviorControl<EntityMaid> dpsModifier =
                new DpsModifierBehavior();

        BehaviorControl<EntityMaid> weaponSwitch =
                new MaidSwitchWeaponBehavior();

        MaidUseShieldTask shield =
                new MaidUseShieldTask();

        // Remove duplicated strafing/shoot entries from Mage task

        return Lists.newArrayList(
                Pair.of(2, weaponSwitch),
                Pair.of(3, dpsModifier),
                Pair.of(4, strafing),
                Pair.of(4, crossbowShoot),
                Pair.of(5, startAttack),
                Pair.of(4, bowShootTask),
                Pair.of(5, stopAttack),
                Pair.of(5, moveToTarget),
                Pair.of(5, meleeAttack),
                Pair.of(5, shield)
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
        return maid.distanceTo(target) <= getSearchRange(maid)
                && maid.getSensing().hasLineOfSight(target);
    }

    @Override
    public float searchRadius(EntityMaid maid) {
        return getSearchRange(maid);
    }

    @Override
    public AABB searchDimension(EntityMaid maid) {
        return maid.getBoundingBox().inflate(getSearchRange(maid));
    }

    private boolean farAway(LivingEntity target, EntityMaid maid) {
        return maid.distanceTo(target) > getSearchRange(maid);
    }

    private float getSearchRange(EntityMaid maid) {
        return hasRangedWeapon(maid) ? RANGED_SEARCH_DISTANCE : DEFAULT_SEARCH_DISTANCE;
    }

    private boolean hasRangedWeapon(EntityMaid maid) {
        if (isRangedWeapon(maid.getMainHandItem())) return true;
        if (isRangedWeapon(maid.getOffhandItem())) return true;

        CombinedInvWrapper inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            if (isRangedWeapon(inv.getStackInSlot(i))) return true;
        }
        return false;
    }

    private boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    private ItemStack findAndConsumeArrow(EntityMaid maid, ItemStack bow) {
        boolean infinity =
                EnchantmentHelper.getTagEnchantmentLevel(
                        Enchantments.INFINITY_ARROWS, bow) > 0;

        CombinedInvWrapper inv = maid.getAvailableInv(true);
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack.getItem() instanceof ArrowItem) {
                ItemStack used = stack.copy();
                used.setCount(1);
                if (!infinity) stack.shrink(1);
                return used;
            }
        }
        return ItemStack.EMPTY;
    }
}
