package studio.fantasyit.maid_rpg_task.event;

import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import studio.fantasyit.maid_rpg_task.task.MaidTankTask;

import java.util.*;

@Mod.EventBusSubscriber
public class MaidEventHandler {

    // Track damage to apply per maid per tick
    private static final Map<UUID, Float> pendingDamage = new HashMap<>();

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        LivingEntity target = event.getEntity();

        Player owner;
        if (target instanceof Player player) {
            owner = player;
        } else if (target instanceof TamableAnimal pet && pet.getOwner() instanceof Player) {
            owner = (Player) pet.getOwner();
        } else {
            owner = null;
        }

        if (owner == null || !(target.level() instanceof ServerLevel level)) return;

        // Only redirect if the target is the owner or their pet
        boolean shouldRedirect = target == owner ||
                (target instanceof TamableAnimal pet && pet.getOwner() == owner);

        if (!shouldRedirect) return;

        AABB box = owner.getBoundingBox().inflate(16);
        List<EntityMaid> maids = level.getEntitiesOfClass(
                EntityMaid.class, box,
                maid -> maid.isAlive()
                        && maid.getOwner() == owner
                        && maid.getTask() != null
                        && maid.getTask().getUid().equals(MaidTankTask.UID)
        );

        if (maids.isEmpty()) return;

        // Choose closest maid
        EntityMaid maid = maids.stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(target)))
                .orElse(maids.get(0));

        // Reduce the damage to the player or pet
        float originalDamage = event.getAmount();
        float reducedDamage = originalDamage * 0.5f;
        event.setAmount(reducedDamage);

        // Accumulate redirected damage
        UUID maidId = maid.getUUID();
        pendingDamage.merge(maidId, originalDamage, Float::sum);

        // Optional debug
        System.out.println("Redirect scheduled: " + originalDamage + " -> maid " + maid.getName().getString()
                + " [Total pending: " + pendingDamage.get(maidId) + "]");
    }

    // Apply pending damage safely at end of server tick
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (pendingDamage.isEmpty()) return;

        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (EntityMaid maid : level.getEntitiesOfClass(EntityMaid.class, new AABB(-30000, -300, -30000, 30000, 300, 30000))) {
                UUID maidId = maid.getUUID();
                if (!pendingDamage.containsKey(maidId)) continue;

                float damage = pendingDamage.get(maidId);
                if (damage > 0 && maid.isAlive()) {
                    DamageSource source = level.damageSources().generic();
                    if (!maid.isInvulnerableTo(source)) {
                        maid.hurt(source, damage);
                    }
                }
            }
        }

        // Clear after applying
        pendingDamage.clear();
    }
}
