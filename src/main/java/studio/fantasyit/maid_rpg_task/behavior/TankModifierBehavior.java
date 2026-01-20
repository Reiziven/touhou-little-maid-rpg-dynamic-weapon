package studio.fantasyit.maid_rpg_task.behavior;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.behavior.Behavior;

import java.util.Map;
import java.util.UUID;

public class TankModifierBehavior extends Behavior<EntityMaid> {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("0af1c7b0-d87a-4a6c-a3a3-665f926e3d93");
    private static final UUID ATTACK_MODIFIER_ID = UUID.fromString("3e418842-f531-4c80-9e3c-e7ccfe44db9c");
    private static final double HEALTH_REDUCTION_PERCENTAGE = -0.6; // Reduces max health by 70%
    private static final double ATTACK_INCREASE_PERCENTAGE = -2.0; // Increases attack by 50%

    public TankModifierBehavior() {
        super(Map.of()); // No required memory modules
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        // Reduce Max Health
        var healthAttr = maid.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null && healthAttr.getModifier(HEALTH_MODIFIER_ID) == null) {
            double reductionAmount = -healthAttr.getBaseValue() * HEALTH_REDUCTION_PERCENTAGE;
            AttributeModifier healthMod = new AttributeModifier(
                    HEALTH_MODIFIER_ID,
                    "Tank task health reduction",
                    reductionAmount,
                    AttributeModifier.Operation.ADDITION
            );
            healthAttr.addPermanentModifier(healthMod);

            if (maid.getHealth() > maid.getMaxHealth()) {
                maid.setHealth(maid.getMaxHealth());
            }
        }

        // Increase Attack Damage
        var attackAttr = maid.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null && attackAttr.getModifier(ATTACK_MODIFIER_ID) == null) {
            double increaseAmount = attackAttr.getBaseValue() * ATTACK_INCREASE_PERCENTAGE;
            AttributeModifier attackMod = new AttributeModifier(
                    ATTACK_MODIFIER_ID,
                    "Tank task attack boost",
                    increaseAmount,
                    AttributeModifier.Operation.ADDITION
            );
            attackAttr.addPermanentModifier(attackMod);
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        // Restore Max Health
        var healthAttr = maid.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            healthAttr.removeModifier(HEALTH_MODIFIER_ID);
            if (maid.getHealth() > maid.getMaxHealth()) {
                maid.setHealth(maid.getMaxHealth());
            }
        }

        // Remove Attack Boost
        var attackAttr = maid.getAttribute(Attributes.ATTACK_DAMAGE);
        if (attackAttr != null) {
            attackAttr.removeModifier(ATTACK_MODIFIER_ID);
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
}
