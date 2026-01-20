package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;

import java.util.Map;
import java.util.UUID;

public class MasterModifierBehavior extends Behavior<EntityMaid> {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("0af1c7b0-d87a-4a6c-a3a3-665f926e3d93");

    private static final UUID BASE_ATTACK_BOOST_ID = UUID.fromString("3e418842-f531-4c80-9e3c-e7ccfe44db9c");
    private static final UUID OFFHAND_ATTACK_BOOST_ID = UUID.fromString("4f528852-b631-4d81-ae4b-7cd9fe44dc1e");

    private static final double HEALTH_REDUCTION_PERCENTAGE = -0.6; // Reduces max health by 80%
    private static final double BASE_ATTACK_INCREASE_PERCENTAGE = 0.35; // 35% attack boost
    private static final double OFFHAND_ATTACK_INCREASE_PERCENTAGE = 0.15; // 15% offhand boost

    public MasterModifierBehavior() {
        super(Map.of()); // No required memory modules
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        var healthAttr = maid.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getModifier(HEALTH_MODIFIER_ID) == null) {
            double reductionAmount = -healthAttr.getBaseValue() * HEALTH_REDUCTION_PERCENTAGE;
            AttributeModifier healthMod = new AttributeModifier(
                    HEALTH_MODIFIER_ID,
                    "DPS task health reduction",
                    reductionAmount,
                    AttributeModifier.Operation.ADDITION
            );
            healthAttr.addPermanentModifier(healthMod);

            if (maid.getHealth() > maid.getMaxHealth()) {
                maid.setHealth(maid.getMaxHealth());
            }
        }

        updateAttackBoosts(maid);
    }

    @Override
    protected void tick(ServerLevel level, EntityMaid maid, long gameTime) {
        updateAttackBoosts(maid);
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        var healthAttr = maid.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_MODIFIER_ID);
            if (maid.getHealth() > maid.getMaxHealth()) {
                maid.setHealth(maid.getMaxHealth());
            }
        }

        var attackAttr = maid.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            if (attackAttr.getModifier(BASE_ATTACK_BOOST_ID) != null) {
                attackAttr.removeModifier(BASE_ATTACK_BOOST_ID);
            }
            if (attackAttr.getModifier(OFFHAND_ATTACK_BOOST_ID) != null) {
                attackAttr.removeModifier(OFFHAND_ATTACK_BOOST_ID);
            }
        }
    }

    @Override
    protected boolean canStillUse(ServerLevel level, EntityMaid maid, long gameTime) {
        return true;
    }

    @Override
    protected boolean timedOut(long gameTime) {
        return false;
    }

    private void updateAttackBoosts(EntityMaid maid) {
        AttributeInstance attackAttr = maid.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr == null) return;

        if (attackAttr.getModifier(BASE_ATTACK_BOOST_ID) != null) {
            attackAttr.removeModifier(BASE_ATTACK_BOOST_ID);
        }
        if (attackAttr.getModifier(OFFHAND_ATTACK_BOOST_ID) != null) {
            attackAttr.removeModifier(OFFHAND_ATTACK_BOOST_ID);
        }

        AttributeModifier baseAttackMod = new AttributeModifier(
                BASE_ATTACK_BOOST_ID,
                "DPS base attack boost",
                BASE_ATTACK_INCREASE_PERCENTAGE,
                AttributeModifier.Operation.MULTIPLY_TOTAL);
        attackAttr.addPermanentModifier(baseAttackMod);

        if (isOffhandSwordOrAxe(maid)) {
            AttributeModifier offhandMod = new AttributeModifier(
                    OFFHAND_ATTACK_BOOST_ID,
                    "DPS offhand weapon boost",
                    OFFHAND_ATTACK_INCREASE_PERCENTAGE,
                    AttributeModifier.Operation.MULTIPLY_TOTAL);
            attackAttr.addPermanentModifier(offhandMod);
        }
    }

    private boolean isOffhandSwordOrAxe(EntityMaid maid) {
        ItemStack offhandItem = maid.getItemBySlot(EquipmentSlot.OFFHAND);
        if (offhandItem.isEmpty()) return false;
        Item item = offhandItem.getItem();
        return item instanceof SwordItem || item instanceof AxeItem;
    }
}
