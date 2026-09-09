package io.github.hirotasoshu.seasonsshrubberycompat.mixin;

import io.github.lucaargolo.seasons.utils.FertilizableUtil;
import io.github.lucaargolo.seasons.utils.SeasonalFertilizable;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Fertilizable;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.sereneshrubbery.block.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only the optional Seasons bonemeal policy changes flower duplication. */
@Mixin({ModFlowerBlock.class, TallModFlowerBlock.class, CrownCactusBlock.class,
    LiverWortBlock.class, BlanketFlowerBlock.class, ButterflyBushBlock.class})
public abstract class FlowerBonemealMixin extends Block implements Fertilizable, SeasonalFertilizable {
    protected FlowerBonemealMixin(Settings settings) { super(settings); }

    @Inject(method = "grow", at = @At("HEAD"), cancellable = true)
    private void seasonalBonemeal(ServerWorld world, Random random, BlockPos pos,
                                  BlockState state, CallbackInfo ci) {
        FertilizableUtil.growInject(this, world, random, pos, state, ci);
    }
}
