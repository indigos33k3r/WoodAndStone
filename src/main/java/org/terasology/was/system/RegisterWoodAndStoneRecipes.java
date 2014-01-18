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
package org.terasology.was.system;

import org.terasology.engine.CoreRegistry;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.systems.ComponentSystem;
import org.terasology.entitySystem.systems.In;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.inventory.SlotBasedInventoryManager;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Region3i;
import org.terasology.math.Vector3i;
import org.terasology.was.system.hand.CraftInHandRecipeRegistry;
import org.terasology.was.system.hand.recipe.CompositeTypeBasedCraftInHandRecipe;
import org.terasology.was.system.hand.recipe.CraftInHandRecipe;
import org.terasology.was.system.hand.recipe.SimpleConsumingCraftInHandRecipe;
import org.terasology.was.system.hand.recipe.behaviour.ConsumeItemCraftBehaviour;
import org.terasology.was.system.hand.recipe.behaviour.DoNothingCraftBehaviour;
import org.terasology.was.system.hand.recipe.behaviour.ReduceItemDurabilityCraftBehaviour;
import org.terasology.workstation.component.CraftingStationIngredientComponent;
import org.terasology.workstation.system.CraftingStationRecipeRegistry;
import org.terasology.workstation.system.recipe.CraftingStationRecipe;
import org.terasology.workstation.system.recipe.SimpleWorkstationRecipe;
import org.terasology.workstation.system.recipe.UpgradeRecipe;
import org.terasology.world.WorldProvider;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockManager;
import org.terasology.world.block.regions.BlockRegionComponent;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
@RegisterSystem
public class RegisterWoodAndStoneRecipes implements ComponentSystem {
    @In
    private CraftInHandRecipeRegistry recipeRegistry;
    @In
    private CraftingStationRecipeRegistry stationRecipeRegistry;
    @In
    private SlotBasedInventoryManager inventoryManager;

    @Override
    public void initialise() {
        addCraftInHandRecipes();

        addBasicWorkstationRecipes();
        addStandardWorkstationRecipes();

        addBasicStoneWorkstationRecipes();

        addWorkstationUpgradeRecipes();
    }

    private void addBasicStoneWorkstationRecipes() {
        SimpleWorkstationRecipe cobbleRecipe = new SimpleWorkstationRecipe();
        cobbleRecipe.addIngredient("WoodAndStone:stone", 2);
        cobbleRecipe.addRequiredTool("stone", 1);
        cobbleRecipe.setBlockResult("Core:CobbleStone", (byte) 1);

        addBasicStoneworkingRecipe("WoodAndStone:CobbleBlock", cobbleRecipe);

        SimpleWorkstationRecipe sharpStoneRecipe = new SimpleWorkstationRecipe();
        sharpStoneRecipe.addIngredient("WoodAndStone:stone", 1);
        sharpStoneRecipe.addRequiredTool("stone", 1);
        sharpStoneRecipe.setItemResult("WoodAndStone:sharpStone", (byte) 2);

        addBasicStoneworkingRecipe("WoodAndStone:SharpStone", sharpStoneRecipe);

        SimpleWorkstationRecipe stoneAxeRecipe = new SimpleWorkstationRecipe();
        stoneAxeRecipe.addIngredient("WoodAndStone:sharpStone", 2);
        stoneAxeRecipe.addIngredient("WoodAndStone:stick", 1);
        stoneAxeRecipe.addRequiredTool("stone", 1);
        stoneAxeRecipe.setItemResult("WoodAndStone:StoneAxe", (byte) 1);

        addBasicStoneworkingRecipe("WoodAndStone:StoneAxe", stoneAxeRecipe);

        SimpleWorkstationRecipe stoneHammerRecipe = new SimpleWorkstationRecipe();
        stoneHammerRecipe.addIngredient("WoodAndStone:sharpStone", 1);
        stoneHammerRecipe.addIngredient("WoodAndStone:stone", 1);
        stoneHammerRecipe.addIngredient("WoodAndStone:stick", 1);
        stoneHammerRecipe.addRequiredTool("stone", 1);
        stoneHammerRecipe.setItemResult("WoodAndStone:StoneHammer", (byte) 1);

        addBasicStoneworkingRecipe("WoodAndStone:StoneHammer", stoneHammerRecipe);

        SimpleWorkstationRecipe stonePickaxeRecipe = new SimpleWorkstationRecipe();
        stonePickaxeRecipe.addIngredient("WoodAndStone:sharpStone", 2);
        stonePickaxeRecipe.addIngredient("WoodAndStone:stone", 1);
        stonePickaxeRecipe.addIngredient("WoodAndStone:stick", 1);
        stonePickaxeRecipe.addRequiredTool("stone", 1);
        stonePickaxeRecipe.setItemResult("WoodAndStone:StonePickaxe", (byte) 1);

        addBasicStoneworkingRecipe("WoodAndStone:StonePickaxe", stonePickaxeRecipe);
    }

    private void addWorkstationUpgradeRecipes() {
        stationRecipeRegistry.addStationUpgradeRecipe("WoodAndStone:BasicWoodcrafting", "WoodAndStone:StandardWoodStation",
                new UpgradeRecipe() {
                    @Override
                    public boolean isUpgradeComponent(EntityRef item) {
                        CraftingStationIngredientComponent ingredient = item.getComponent(CraftingStationIngredientComponent.class);
                        return ingredient != null && ingredient.type.equals("WoodAndStone:plank");
                    }

                    @Override
                    public UpgradeResult getMatchingUpgradeResult(EntityRef station, final int upgradeSlotFrom, final int upgradeSlotCount) {
                        int planksCount = 0;
                        for (int i = upgradeSlotFrom; i < upgradeSlotFrom + upgradeSlotCount; i++) {
                            EntityRef item = inventoryManager.getItemInSlot(station, i);
                            CraftingStationIngredientComponent ingredient = item.getComponent(CraftingStationIngredientComponent.class);
                            if (ingredient != null && ingredient.type.equals("WoodAndStone:plank")) {
                                planksCount += item.getComponent(ItemComponent.class).stackCount;
                            }
                        }
                        if (planksCount < 10) {
                            return null;
                        }

                        return new UpgradeResult() {
                            @Override
                            public void processUpgrade(EntityRef station) {
                                removeUpgradeIngredients(station);

                                WorldProvider worldProvider = CoreRegistry.get(WorldProvider.class);
                                BlockManager blockManager = CoreRegistry.get(BlockManager.class);
                                EntityManager entityManager = CoreRegistry.get(EntityManager.class);

                                Block block = blockManager.getBlock("WoodAndStone:StandardWoodStation");
                                Region3i region = station.getComponent(BlockRegionComponent.class).region;
                                for (Vector3i location : region) {
                                    worldProvider.setBlock(location, block);
                                }

                                final EntityRef newStation = entityManager.create("WoodAndStone:StandardWoodStation");

                                inventoryManager.moveItem(station, 0, newStation, 0);
                                inventoryManager.moveItem(station, 1, newStation, 2);
                                inventoryManager.moveItem(station, 2, newStation, 4);
                                inventoryManager.moveItem(station, 3, newStation, 5);
                                inventoryManager.moveItem(station, 4, newStation, 6);
                                inventoryManager.moveItem(station, 5, newStation, 10);

                                station.destroy();

                                newStation.addComponent(new BlockRegionComponent(region));
                                newStation.addComponent(new LocationComponent(region.center()));
                            }

                            private void removeUpgradeIngredients(EntityRef station) {
                                int leftToRemote = 10;
                                for (int i = upgradeSlotFrom; i < upgradeSlotFrom + upgradeSlotCount; i++) {
                                    EntityRef item = inventoryManager.getItemInSlot(station, i);
                                    CraftingStationIngredientComponent ingredient = item.getComponent(CraftingStationIngredientComponent.class);
                                    if (ingredient != null && ingredient.type.equals("WoodAndStone:plank")) {
                                        int toRemove = Math.min(inventoryManager.getStackSize(item), leftToRemote);
                                        inventoryManager.removeItem(station, item, toRemove);
                                        leftToRemote -= toRemove;
                                    }
                                    if (leftToRemote == 0) {
                                        break;
                                    }
                                }
                            }
                        };
                    }
                });
    }

    private void addCraftInHandRecipes() {
        addCraftInHandRecipe("WoodAndStone:CrudeHammer",
                new SimpleConsumingCraftInHandRecipe("stick", "binding", "stone", "WoodAndStone:CrudeHammer"));
        addCraftInHandRecipe("WoodAndStone:CrudeAxe",
                new SimpleConsumingCraftInHandRecipe("stick", "binding", "sharpStone", "WoodAndStone:CrudeAxe"));

        addCraftInHandRecipe("WoodAndStone:sharpStone",
                new CompositeTypeBasedCraftInHandRecipe(
                        "stone", new ConsumeItemCraftBehaviour("stone"),
                        "hammer", new ReduceItemDurabilityCraftBehaviour("hammer", 1),
                        null, new DoNothingCraftBehaviour(),
                        "WoodAndStone:sharpStone"));
    }

    private void addBasicWorkstationRecipes() {
        SimpleWorkstationRecipe plankRecipe = new SimpleWorkstationRecipe();
        plankRecipe.addIngredient("WoodAndStone:wood", 1);
        plankRecipe.addRequiredTool("wood", 1);
        plankRecipe.setItemResult("WoodAndStone:WoodPlank", (byte) 2);

        addBasicWoodworkingRecipe("WoodAndStone:WoodPlank", plankRecipe);
    }

    private void addStandardWorkstationRecipes() {
        SimpleWorkstationRecipe plankRecipe = new SimpleWorkstationRecipe();
        plankRecipe.addIngredient("WoodAndStone:wood", 1);
        plankRecipe.addRequiredTool("wood", 1);
        plankRecipe.setItemResult("WoodAndStone:WoodPlank", (byte) 3);

        addStandardWoodworkingRecipe("WoodAndStone:WoodPlank", plankRecipe);

        SimpleWorkstationRecipe plankWallRecipe = new SimpleWorkstationRecipe();
        plankWallRecipe.addIngredient("WoodAndStone:plank", 2);
        plankWallRecipe.addRequiredTool("wood", 1);
        plankWallRecipe.addRequiredTool("stone", 1);
        plankWallRecipe.setBlockResult("Core:Plank", (byte) 4);

        addStandardWoodworkingRecipe("WoodAndStone:PlankWall", plankWallRecipe);

        SimpleWorkstationRecipe woodenTableRecipe = new SimpleWorkstationRecipe();
        woodenTableRecipe.addIngredient("WoodAndStone:plank", 2);
        woodenTableRecipe.addIngredient("WoodAndStone:wood", 1);
        woodenTableRecipe.addRequiredTool("wood", 1);
        woodenTableRecipe.addRequiredTool("stone", 1);
        woodenTableRecipe.setBlockResult("WoodAndStone:WoodenTable", (byte) 1);

        addStandardWoodworkingRecipe("WoodAndStone:WoodenTable", woodenTableRecipe);
    }

    public void addCraftInHandRecipe(String recipeId, CraftInHandRecipe craftInHandRecipe) {
        recipeRegistry.addCraftInHandRecipe(recipeId, craftInHandRecipe);
    }

    public void addBasicWoodworkingRecipe(String recipeId, CraftingStationRecipe recipe) {
        stationRecipeRegistry.addCraftingStationRecipe("WoodAndStone:BasicWoodcrafting", recipeId, recipe);
    }

    public void addStandardWoodworkingRecipe(String recipeId, CraftingStationRecipe recipe) {
        stationRecipeRegistry.addCraftingStationRecipe("WoodAndStone:StandardWoodcrafting", recipeId, recipe);
    }

    public void addBasicStoneworkingRecipe(String recipeId, CraftingStationRecipe recipe) {
        stationRecipeRegistry.addCraftingStationRecipe("WoodAndStone:BasicStonecrafting", recipeId, recipe);
    }

    @Override
    public void shutdown() {
    }
}