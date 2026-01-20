package studio.fantasyit.maid_rpg_task.behavior.common;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.Map;
import java.util.UUID;

public class DpsModifierBehavior extends Behavior<EntityMaid> {
    private static final UUID HEALTH_MODIFIER_ID = UUID.fromString("0af1c7b0-d87a-4a6c-a3a3-665f926e3d93");
    private static final double HEALTH_REDUCTION_PERCENTAGE = 0.70; // Reduces max health by 70%

    public DpsModifierBehavior() {
        super(Map.of()); // No required memory modules
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel level, EntityMaid maid) {
        return true;
    }

    @Override
    protected void start(ServerLevel level, EntityMaid maid, long gameTime) {
        var attribute = maid.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null && attribute.getModifier(HEALTH_MODIFIER_ID) == null) {
            double currentMaxHealth = attribute.getBaseValue();
            double reductionAmount = -currentMaxHealth * HEALTH_REDUCTION_PERCENTAGE;

            AttributeModifier modifier = new AttributeModifier(
                    HEALTH_MODIFIER_ID,
                    "DPS task health reduction",
                    reductionAmount,
                    AttributeModifier.Operation.ADDITION
            );

            attribute.addPermanentModifier(modifier);

            // Ensure current health doesn't exceed new max
            if (maid.getHealth() > maid.getMaxHealth()) {
                maid.setHealth(maid.getMaxHealth());
            }
        }
    }

    @Override
    protected void stop(ServerLevel level, EntityMaid maid, long gameTime) {
        var attribute = maid.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(HEALTH_MODIFIER_ID);

            // Optional: Heal her to new full health after DPS ends
            if (maid.getHealth() > maid.getMaxHealth()) {
                maid.setHealth(maid.getMaxHealth());
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
}
