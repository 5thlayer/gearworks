// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: MIT

package io.github._5thlayer.gearworks.railgun;

import java.util.Comparator;
import java.util.List;

import io.github._5thlayer.gearworks.gear.GearworksConfig;
import io.github._5thlayer.gearworks.level.RailgunCharge;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * A Railgun's shot: instant, from the shooter's eye along their gaze, as far as its range or the first
 * block whose collision shape it meets, as an arrow's would stop at it. Every living entity whose box it
 * crosses on the way takes its damage, as a projectile caused by the shooter, and is knocked a little
 * along the beam; it breaks no block. The beam is drawn in particles and heard, by everyone near.
 */
final class RailgunShot {

    /** Blocks between the beam's particles. */
    private static final double BEAM_STEP = 0.5;

    /** The beam's first particle, this far from the eye so it never fills the shooter's view. */
    private static final double BEAM_START = 1.0;

    private RailgunShot() {
    }

    static void fire(ServerLevel level, Player shooter, ItemStack railgun) {
        var from = shooter.getEyePosition();
        var look = shooter.getViewVector(1.0f);
        var to = end(level, shooter, from, from.add(look.scale(GearworksConfig.railgunRange())));
        var source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(Railgun.SHOT), shooter);
        float damage = (float) RailgunCharge.damage(GearworksConfig.railgunDamage(), Railgun.enchantmentLevel(level, railgun, Enchantments.POWER));
        double knockback = GearworksConfig.railgunKnockback();
        for (var target : struck(level, shooter, from, to)) {
            // An enderman dodges by teleporting, which counts as hurt; as with an arrow, it takes no knockback where it lands.
            if (target.hurtServer(level, source, damage) && knockback > 0 && !target.is(EntityType.ENDERMAN)) {
                target.knockback(knockback, -look.x, -look.z);
            }
        }
        beam(level, from, to, look);
        // A firework's crack for the shot and a wind charge's burst for the air it splits, both pitched up to stay short.
        level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 0.6f, 1.4f);
        level.playSound(null, shooter.getX(), shooter.getEyeY(), shooter.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, SoundSource.PLAYERS, 0.3f, 1.6f);
    }

    /** Where the shot stops: on the first block in its way, else at its range. */
    private static Vec3 end(Level level, Entity shooter, Vec3 from, Vec3 far) {
        var block = level.clip(new ClipContext(from, far, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        return block.getType() == HitResult.Type.MISS ? far : block.getLocation();
    }

    /** Every living entity but the shooter that a projectile could hit and whose box the line crosses, nearest first. */
    private static List<LivingEntity> struck(Level level, Entity shooter, Vec3 from, Vec3 to) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(from, to).inflate(1.0),
                        entity -> entity != shooter && entity.canBeHitByProjectile()
                                && entity.getBoundingBox().inflate(entity.getPickRadius()).clip(from, to).isPresent())
                .stream()
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)))
                .toList();
    }

    /** Sparks along the line, sent to every player who can see any of it. */
    private static void beam(ServerLevel level, Vec3 from, Vec3 to, Vec3 look) {
        double length = from.distanceTo(to);
        for (double along = BEAM_START; along <= length; along += BEAM_STEP) {
            var at = from.add(look.scale(along));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, true, false, at.x, at.y, at.z, 1, 0, 0, 0, 0);
        }
    }
}
