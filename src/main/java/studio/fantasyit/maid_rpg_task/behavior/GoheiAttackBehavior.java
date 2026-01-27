package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.projectile.DanmakuShoot;
import com.github.tartaricacid.touhoulittlemaid.init.InitEnchantments;
import com.github.tartaricacid.touhoulittlemaid.item.ItemHakureiGohei;
import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Optional;

public class GoheiAttackBehavior extends Behavior<EntityMaid> {
    private final int interval;
    private int cooldown;

    public GoheiAttackBehavior(int interval) {
        super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_PRESENT));
        this.interval = interval;
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        ItemStack main = maid.getMainHandItem();
        return ItemHakureiGohei.isGohei(main);
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        cooldown = 0;
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        Optional<LivingEntity> targetOpt = maid.getBrain().getMemory(MemoryModuleType.ATTACK_TARGET);
        if (targetOpt.isEmpty()) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        LivingEntity target = targetOpt.get();
        ItemStack main = maid.getMainHandItem();
        if (!ItemHakureiGohei.isGohei(main)) return;

        maid.getBrain().getMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES).ifPresent(livingEntities -> {
            long entityCount = livingEntities.stream().filter(test -> enemyEntityTest(maid, target, test)).count();
            AttributeInstance attackDamage = maid.getAttribute(Attributes.ATTACK_DAMAGE);
            float attackValue = 2.0f;
            if (attackDamage != null) {
                attackValue = (float) attackDamage.getBaseValue();
            }
            int impedingLevel = EnchantmentHelper.getTagEnchantmentLevel(InitEnchantments.IMPEDING.get(), main);
            int speedyLevel = EnchantmentHelper.getTagEnchantmentLevel(InitEnchantments.SPEEDY.get(), main);
            int multiShotLevel = EnchantmentHelper.getTagEnchantmentLevel(Enchantments.MULTISHOT, main);
            int endersEnderLevel = EnchantmentHelper.getTagEnchantmentLevel(InitEnchantments.ENDERS_ENDER.get(), main);
            float speed = (0.3f) * (speedyLevel + 1);
            boolean hurtEnderman = endersEnderLevel > 0;
            float distance = maid.distanceTo(target);
            speed = speed + Mth.clamp(distance / 40f - 0.4f, 0, 2.4f);
            float inaccuracy = 1 - Mth.clamp(distance / 100f, 0, 0.8f);

            if (entityCount <= 1) {
                if (multiShotLevel > 0) {
                    DanmakuShoot.create().setWorld(level).setThrower(maid)
                            .setTarget(target).setRandomColor().setRandomType()
                            .setDamage(attackValue).setGravity(0)
                            .setVelocity(speed).setHurtEnderman(hurtEnderman)
                            .setInaccuracy(inaccuracy).setFanNum(3).setYawTotal(Math.PI / 12)
                            .setImpedingLevel(impedingLevel)
                            .fanShapedShot();
                } else {
                    DanmakuShoot.create().setWorld(level).setThrower(maid)
                            .setTarget(target).setRandomColor().setRandomType()
                            .setDamage(attackValue).setGravity(0)
                            .setVelocity(speed).setHurtEnderman(hurtEnderman)
                            .setInaccuracy(inaccuracy / 5).setImpedingLevel(impedingLevel)
                            .aimedShot();
                }
            } else if (entityCount <= 5) {
                DanmakuShoot.create().setWorld(level).setThrower(maid)
                        .setTarget(target).setRandomColor().setRandomType()
                        .setDamage(attackValue).setGravity(0)
                        .setVelocity(speed).setHurtEnderman(hurtEnderman)
                        .setInaccuracy(inaccuracy / 5).setFanNum(8).setYawTotal(Math.PI / 3)
                        .setImpedingLevel(impedingLevel)
                        .fanShapedShot();
            } else {
                DanmakuShoot.create().setWorld(level).setThrower(maid)
                        .setTarget(target).setRandomColor().setRandomType()
                        .setDamage(attackValue).setGravity(0)
                        .setVelocity(speed).setHurtEnderman(hurtEnderman)
                        .setInaccuracy(inaccuracy / 5).setFanNum(32).setYawTotal(2 * Math.PI / 3)
                        .setImpedingLevel(impedingLevel)
                        .fanShapedShot();
            }
            main.hurtAndBreak(1, maid, (m) -> m.broadcastBreakEvent(InteractionHand.MAIN_HAND));
        });

        cooldown = interval;
    }

    private boolean enemyEntityTest(EntityMaid shooter, LivingEntity target, LivingEntity test) {
        boolean canAttack = shooter.canAttack(test);
        boolean sameType = target.getType().equals(test.getType());
        return canAttack && sameType && shooter.canSee(test);
    }
}
