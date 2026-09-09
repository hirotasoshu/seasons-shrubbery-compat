package io.github.hirotasoshu.seasonsshrubberycompat.test;

import com.google.gson.Gson;
import io.github.lucaargolo.seasons.FabricSeasons;
import io.github.lucaargolo.seasons.resources.CropConfigs;
import io.github.lucaargolo.seasons.utils.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.block.*;
import net.minecraft.entity.ItemEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.GameRules;
import net.minecraft.world.LightType;
import net.sereneshrubbery.ModBlocks;
import net.sereneshrubbery.block.FlowerProperties;
import net.sereneshrubbery.data.BreedingTimerState;
import java.nio.file.*;
import java.util.*;

/** Exercises the real target methods in a disposable dedicated Minecraft server. */
public final class IntegrationTests implements ModInitializer {
    private static final BlockPos POS = new BlockPos(8, 100, 8);
    private static final String TIMER_KEY = "8,100,8";
    private int delay = -1;
    private int checks;
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            server.getOverworld().getChunk(0, 0);
            server.getOverworld().setTimeOfDay(6000);
            server.getGameRules().get(GameRules.DO_DAYLIGHT_CYCLE).set(false, server);
            server.getGameRules().get(GameRules.RANDOM_TICK_SPEED).set(0, server);
            server.getGameRules().get(GameRules.DO_MOB_SPAWNING).set(false, server);
            delay = 40;
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (delay < 0 || --delay != 0) return;
            delay = -1;
            try {
                run(server);
                Files.writeString(Path.of("integration-result.txt"), "PASS " + checks + " checks\n");
                System.out.println("SHRUBBERY_COMPAT_TEST_PASS: " + checks + " checks");
            } catch (Throwable error) {
                error.printStackTrace();
                try { Files.writeString(Path.of("integration-result.txt"), "FAIL " + error); }
                catch (Exception writeError) { throw new RuntimeException(writeError); }
            } finally { server.stop(false); }
        });
    }
    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    private void config(boolean crops, boolean bonemeal, boolean underground, String season) {
        FabricSeasons.CONFIG = new Gson().fromJson("{\"isSeasonMessingCrops\":" + crops
            + ",\"isSeasonMessingBonemeal\":" + bonemeal
            + ",\"doCropsGrowsNormallyUnderground\":" + underground
            + ",\"seasonLock\":{\"isSeasonLocked\":true,\"lockedSeason\":\"" + season + "\"}}", ModConfig.class);
    }
    private void override(Block flower, float multiplier) {
        HashMap<Identifier, CropConfig> configs = new HashMap<>();
        configs.put(Registries.BLOCK.getId(flower), new CropConfig(multiplier,multiplier,multiplier,multiplier));
        CropConfigs.receiveConfig(new CropConfig(1,1,1,1),configs);
    }
    private void garden(ServerWorld world, Block flower, Block partner, boolean placed) {
        for (BlockPos p : BlockPos.iterate(POS.add(-2,0,-2),POS.add(2,0,2))) {
            world.setBlockState(p,Blocks.AIR.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            world.setBlockState(p.down(),Blocks.GRASS_BLOCK.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
        }
        world.setBlockState(POS,flower.getDefaultState().with(FlowerProperties.PLAYER_PLACED,placed));
        world.setBlockState(POS.east(),partner.getDefaultState().with(FlowerProperties.PLAYER_PLACED,placed));
    }
    private int breeds(ServerWorld world, Block flower, Block partner, long elapsed, int count, boolean placed) {
        int offspring=0;
        world.getRandom().setSeed(271828L);
        for(int i=0;i<count;i++) {
            garden(world,flower,partner,placed);
            BreedingTimerState.getServerState(world).setTimer(TIMER_KEY,System.currentTimeMillis()-elapsed);
            flower.randomTick(world.getBlockState(POS),world,POS,world.getRandom());
            for(BlockPos p:BlockPos.iterate(POS.add(-1,0,-1),POS.add(1,0,1))) {
                if(!p.equals(POS)&&!p.equals(POS.east())&&!world.getBlockState(p).isAir()) offspring++;
            }
        }
        return offspring;
    }
    private int bonemeal(ServerWorld world, Block block, int count) {
        int growth=0;
        Random random=Random.create(314159L);
        Box box=new Box(POS).expand(4);
        for(int i=0;i<count;i++) {
            world.getEntitiesByClass(ItemEntity.class,box,e->true).forEach(ItemEntity::discard);
            world.setBlockState(POS.down(),Blocks.GRASS_BLOCK.getDefaultState(),Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            BlockState before=block.getDefaultState();
            world.setBlockState(POS,before,Block.NOTIFY_LISTENERS|Block.FORCE_STATE);
            ((Fertilizable)block).grow(world,random,POS,before);
            growth+=world.getEntitiesByClass(ItemEntity.class,box,e->true).stream().mapToInt(e->e.getStack().getCount()).sum();
            if(!world.getBlockState(POS).equals(before)) growth++;
        }
        world.getEntitiesByClass(ItemEntity.class,box,e->true).forEach(ItemEntity::discard);
        return growth;
    }
    private void run(MinecraftServer server) {
        ServerWorld world=server.getOverworld();
        int flowers=0;
        for(Block block:Registries.BLOCK) {
            Identifier id=Registries.BLOCK.getId(block);
            if(!id.getNamespace().equals("serene_shrubbery")||!(block instanceof Fertilizable)) continue;
            flowers++;
            check(block instanceof SeasonalFertilizable,"Bonemeal mixin missing: "+id);
            check(CropConfigs.getSeasonCropMultiplier(id,Season.SPRING)==1.25f,"Spring profile: "+id);
            check(CropConfigs.getSeasonCropMultiplier(id,Season.WINTER)==.5f,"Winter profile: "+id);
        }
        check(flowers==42,"Expected all42 flowers, got "+flowers);
        config(false,false,false,"SUMMER");
        int acquired=0;
        Random grassRandom=Random.create(123456L);
        for(int i=0;i<80;i++) {
            for(BlockPos p:BlockPos.iterate(POS.add(-3,0,-3),POS.add(3,0,3))) {
                world.setBlockState(p.down(),Blocks.GRASS_BLOCK.getDefaultState());
                world.setBlockState(p,Blocks.GRASS.getDefaultState());
            }
            ((Fertilizable)Blocks.GRASS_BLOCK).grow(world,grassRandom,POS.down(),Blocks.GRASS_BLOCK.getDefaultState());
            for(BlockPos p:BlockPos.iterate(POS.add(-2,0,-2),POS.add(2,0,2))) {
                if(Registries.BLOCK.getId(world.getBlockState(p).getBlock()).getNamespace().equals("serene_shrubbery")) acquired++;
            }
        }
        check(acquired>0,"Bonemeal must acquire starter flowers in old plains chunks");
        Block[][] pairs={{ModBlocks.RED_PANSIES,ModBlocks.YELLOW_PANSIES},
            {ModBlocks.PEACH_FOXGLOVE,ModBlocks.WHITE_FOXGLOVE},
            {ModBlocks.RED_HYDRANGEA,ModBlocks.WHITE_HYDRANGEA}};
        for(Block[] pair:pairs) {
            config(true,false,false,"SUMMER");
            int summer=breeds(world,pair[0],pair[1],180000,500,true);
            config(true,false,false,"WINTER");
            int winter=breeds(world,pair[0],pair[1],180000,500,true);
            config(true,false,false,"SPRING");
            int spring=breeds(world,pair[0],pair[1],180000,500,true);
            check(winter>0&&winter<summer*.5,"Winter breeding slower: "+winter+"/"+summer);
            check(summer>50&&spring>summer,"Spring breeding faster: "+spring+"/"+summer);
            check(breeds(world,pair[0],pair[1],500000,20,true)==20,"Spring guaranteed breeding after scaledtimer");
            config(true,false,false,"WINTER");
            check(breeds(world,pair[0],pair[1],100000,20,true)==0,"Winter minimumtime scaled");
            check(breeds(world,pair[0],pair[1],2000000,20,false)==0,"Natural flowers must not breed");
            check(breeds(world,pair[0],pair[1],2000000,20,true)==20,"Long winter wait permits breeding");
            check(breeds(world,pair[0],pair[1],-1000000,20,true)==0,"Clock regression must not breed");
            config(false,false,false,"WINTER");
            check(breeds(world,pair[0],pair[1],650000,20,true)==20,"Global crop toggle ignored");
            System.out.println("Breeding "+Registries.BLOCK.getId(pair[0])+": spring="+spring+", summer="+summer+", winter="+winter);
        }
        // Seasons profiles are user-overridable; multiplier0 suppresses breeding without removing flowers.
        Block flower=ModBlocks.RED_PANSIES;
        override(flower,0);
        config(true,true,false,"WINTER");
        check(breeds(world,flower,ModBlocks.YELLOW_PANSIES,2000000,20,true)==0,"Zero multiplier ignored");
        check(world.getBlockState(POS).isOf(flower),"Winter destroyed decorative flower");
        long originalTimer=BreedingTimerState.getServerState(world).getTimer(TIMER_KEY);
        config(true,true,false,"SPRING");
        flower.randomTick(world.getBlockState(POS),world,POS,world.getRandom());
        check(BreedingTimerState.getServerState(world).getTimer(TIMER_KEY)==originalTimer,"Season transition reset timer");
        for(Block block:new Block[]{ModBlocks.RED_PANSIES,ModBlocks.WHITE_FOXGLOVE,
                ModBlocks.ORANGE_CROWN_CACTUS,ModBlocks.BLUE_LIVERWORT,
                ModBlocks.BLANKETFLOWER,ModBlocks.BUTTERFLY_BUSH}) {
            override(block,0);
            config(true,true,false,"WINTER");
            check(bonemeal(world,block,20)==0,"Seasonal bonemeal bypass: "+block);
            config(true,false,false,"WINTER");
            check(bonemeal(world,block,20)==20,"Disabled bonemeal policy: "+block);
            override(block,.5f);
            config(true,true,false,"WINTER");
            int half=bonemeal(world,block,200);
            check(half>60&&half<140,"Half bonemeal policy: "+block+" "+half);
            override(block,2);
            check(bonemeal(world,block,20)==(block==ModBlocks.BLANKETFLOWER?20:40),"Fast bonemeal policy: "+block);
        }
        HashMap<Identifier,CropConfig> profiles=new HashMap<>();
        profiles.put(Registries.BLOCK.getId(flower),new CropConfig(1,1,1,0));
        CropConfigs.receiveConfig(new CropConfig(1,1,1,1),profiles);
        config(true,true,false,"WINTER");
        garden(world,flower,ModBlocks.YELLOW_PANSIES,true);
        check(FertilizableUtil.getMultiplier(world,POS,world.getBlockState(POS))==0,"Winter fixture");
        GreenhouseCache.add(world,new ChunkPos(POS),new GreenhouseCache.GreenHouseTicket(
            new BlockBox(POS.getX(),POS.getY(),POS.getZ(),POS.getX(),POS.getY(),POS.getZ()),Season.SUMMER));
        check(breeds(world,flower,ModBlocks.YELLOW_PANSIES,650000,20,true)==20,"Greenhouse breeding");
        check(bonemeal(world,flower,20)==20,"Greenhouse bonemeal");
        BlockPos underground=new BlockPos(8,-63,8);
        check(world.getLightLevel(LightType.SKY,underground)==0,"Underground fixture");
        config(true,true,true,"WINTER");
        check(FertilizableUtil.getMultiplier(world,underground,flower.getDefaultState())==1,"Underground exemption");
        config(true,true,false,"WINTER");
        check(FertilizableUtil.getMultiplier(world,underground,flower.getDefaultState())==0,"Underground exemption disabled");
    }
}
