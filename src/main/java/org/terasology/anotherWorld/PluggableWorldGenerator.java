/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.anotherWorld;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.anotherWorld.coreBiome.ForestBiome;
import org.terasology.anotherWorld.coreBiome.MountainBiome;
import org.terasology.anotherWorld.coreBiome.OceanBiome;
import org.terasology.anotherWorld.coreBiome.RiverBiome;
import org.terasology.engine.CoreRegistry;
import org.terasology.engine.SimpleUri;
import org.terasology.engine.module.DependencyInfo;
import org.terasology.engine.module.Module;
import org.terasology.engine.module.ModuleManager;
import org.terasology.math.Vector3i;
import org.terasology.world.ChunkView;
import org.terasology.world.chunks.Chunk;
import org.terasology.world.generator.WorldGenerator;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class PluggableWorldGenerator implements WorldGenerator {
    private static final Logger logger = LoggerFactory.getLogger(PluggableWorldGenerator.class);

    private String seed;
    private Map<String, Biome> biomes = new HashMap<>();

    /**
     * Chance a chunk will contain a biome definition. Defines the effective size of a biome.
     */
    private float biomeScarcity = 0.10f;
    /**
     * How far a biome influences the terrain from its location (in blocks). If there is no biome in range of a block,
     * that block will have a default biome (River).
     */
    private int biomeInfluenceRange = 40;

    protected BiomeInfluenceProvider biomeInfluenceProvider;

    private Vector3i chunkSize = new Vector3i(16, 256, 16);

    private List<ChunkGenerator> chunkGenerators = new LinkedList<>();

    public PluggableWorldGenerator() {
        initializeCoreBiomes();
        loadBiomes();

        biomeInfluenceProvider = new BiomeInfluenceProvider(seed, biomes.values(), biomes.get("AnotherWorld:River"), biomeScarcity, biomeInfluenceRange, chunkSize);
    }

    public void addChunkGenerator(ChunkGenerator chunkGenerator) {
        chunkGenerators.add(chunkGenerator);
    }

    @Override
    public void setWorldSeed(String seed) {
        this.seed = seed;
    }

    @Override
    public void initialize() {
        appendGenerators();

        for (ChunkGenerator chunkGenerator : chunkGenerators) {
            chunkGenerator.initializeWithSeed(seed);
        }
    }

    protected abstract void appendGenerators();

    private void initializeCoreBiomes() {
        Biome ocean = new OceanBiome();
        biomes.put(ocean.getBiomeId(), ocean);
        Biome desert = new OceanBiome();
        biomes.put(desert.getBiomeId(), desert);
        Biome mountain = new MountainBiome();
        biomes.put(desert.getBiomeId(), mountain);
        Biome forest = new ForestBiome();
        biomes.put(forest.getBiomeId(), forest);
        Biome river = new RiverBiome();
        biomes.put(river.getBiomeId(), river);
    }

    private void loadBiomes() {
        ModuleManager moduleManager = CoreRegistry.get(ModuleManager.class);

        for (Module module : moduleManager.getActiveModules()) {
            loadBiomesFromModule(moduleManager, module);
        }
    }

    private void loadBiomesFromModule(ModuleManager moduleManager, Module module) {
        for (DependencyInfo dependency : module.getModuleInfo().getDependencies()) {
            loadBiomesFromModule(moduleManager, moduleManager.getLatestModuleVersion(dependency.getId()));
        }
        if (module.isCodeModule()) {
            loadBiomesImpl(module.getReflections());
        }
    }

    private void loadBiomesImpl(Reflections reflections) {
        final Set<Class<?>> typesAnnotatedWith = reflections.getTypesAnnotatedWith(RegisterBiome.class);
        for (Class<?> biomeClass : typesAnnotatedWith) {
            if (!Biome.class.isAssignableFrom(biomeClass)) {
                logger.error("Found a class " + biomeClass.getName() + " that has the @RegisterBiome annotation but does not implement Biome interface");
                continue;
            }
            try {
                final Constructor<?> constructor = biomeClass.getConstructor(new Class[0]);
                final Biome biome = (Biome) constructor.newInstance(new Object[0]);
                biomes.put(biome.getBiomeId(), biome);
            } catch (NoSuchMethodException e) {
                logger.error("Found a class " + biomeClass.getName() + " that has the @RegisterBiome but does not have a default constructor", e);
            } catch (InvocationTargetException e) {
                logger.error("Found a class " + biomeClass.getName() + " that has the @RegisterBiome but can't instantiate it", e);
            } catch (InstantiationException e) {
                logger.error("Found a class " + biomeClass.getName() + " that has the @RegisterBiome but can't instantiate it", e);
            } catch (IllegalAccessException e) {
                logger.error("Found a class " + biomeClass.getName() + " that has the @RegisterBiome but can't instantiate it", e);
            }
        }
    }

    @Override
    public void applySecondPass(Vector3i chunkPos, ChunkView view) {
    }

    @Override
    public SimpleUri getUri() {
        return null;
    }

    @Override
    public void createChunk(Chunk chunk) {
        ChunkInformation chunkInformation = new ChunkInformation();
        for (ChunkGenerator chunkGenerator : chunkGenerators) {
            chunkGenerator.generateInChunk(chunk, chunkInformation);
        }
    }

    @Override
    public float getFog(float x, float y, float z) {
        return 0;
    }

    @Override
    public float getTemperature(float x, float y, float z) {
        return 0;
    }

    @Override
    public float getHumidity(float x, float y, float z) {
        return 0;
    }
}