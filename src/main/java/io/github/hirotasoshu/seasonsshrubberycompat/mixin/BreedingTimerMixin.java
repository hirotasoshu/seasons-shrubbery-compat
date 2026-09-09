package io.github.hirotasoshu.seasonsshrubberycompat.mixin;

import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.utils.FertilizableUtil;
import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.sereneshrubbery.event.ModHybridBreeding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Scale the existing timer; never rewrite the upstream persistent timestamp. */
@Mixin(ModHybridBreeding.class)
public abstract class BreedingTimerMixin {
    @ModifyVariable(method = "tryBreedOnRandomTick", at = @At("STORE"), ordinal = 1, remap = false)
    private static long seasonalElapsed(long elapsed, ServerWorld world, BlockPos pos, Block flower) {
        if (!FabricSeasons.CONFIG.isSeasonMessingCrops()) return elapsed;
        float multiplier = FertilizableUtil.getMultiplier(world, pos, world.getBlockState(pos));
        if (!Float.isFinite(multiplier) || multiplier < 0) multiplier = 0;
        return (long) (Math.max(0L, elapsed) * (double) multiplier);
    }
}
