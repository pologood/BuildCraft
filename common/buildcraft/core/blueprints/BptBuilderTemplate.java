/**
 * Copyright (c) 2011-2015, SpaceToad and the BuildCraft Team
 * http://www.mod-buildcraft.com
 *
 * BuildCraft is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */
package buildcraft.core.blueprints;

import java.util.LinkedList;

import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import net.minecraftforge.common.util.ForgeDirection;

import buildcraft.api.blueprints.BuilderAPI;
import buildcraft.api.blueprints.SchematicBlockBase;
import buildcraft.api.core.BlockIndex;
import buildcraft.api.core.BuildCraftAPI;
import buildcraft.api.core.IInvSlot;
import buildcraft.core.builders.BuildingSlot;
import buildcraft.core.builders.BuildingSlotBlock;
import buildcraft.core.builders.BuildingSlotBlock.Mode;
import buildcraft.core.builders.BuildingSlotIterator;
import buildcraft.core.builders.TileAbstractBuilder;
import buildcraft.core.inventory.InventoryIterator;
import buildcraft.core.utils.BlockUtils;

public class BptBuilderTemplate extends BptBuilderBase {

	private LinkedList<BuildingSlotBlock> clearList = new LinkedList<BuildingSlotBlock>();
	private LinkedList<BuildingSlotBlock> buildList = new LinkedList<BuildingSlotBlock>();
	private BuildingSlotIterator iteratorBuild, iteratorClear;

	public BptBuilderTemplate(BlueprintBase bluePrint, World world, int x, int y, int z) {
		super(bluePrint, world, x, y, z);
	}

	@Override
	protected void internalInit () {
		if (blueprint.excavate) {
			for (int j = blueprint.sizeY - 1; j >= 0; --j) {
				for (int i = 0; i < blueprint.sizeX; ++i) {
					for (int k = 0; k < blueprint.sizeZ; ++k) {
						int xCoord = i + x - blueprint.anchorX;
						int yCoord = j + y - blueprint.anchorY;
						int zCoord = k + z - blueprint.anchorZ;

						if (yCoord < 0 || yCoord >= context.world.getHeight()) {
							continue;
						}

						SchematicBlockBase slot = blueprint.contents[i][j][k];

						if (slot == null
								&& !clearedLocations.contains(new BlockIndex(
										xCoord, yCoord, zCoord))) {
							BuildingSlotBlock b = new BuildingSlotBlock();

							b.schematic = null;
							b.x = xCoord;
							b.y = yCoord;
							b.z = zCoord;
							b.mode = Mode.ClearIfInvalid;
							b.buildStage = 0;

							clearList.add(b);
						}
					}
				}
			}
		}

		for (int j = 0; j < blueprint.sizeY; ++j) {
			for (int i = 0; i < blueprint.sizeX; ++i) {
				for (int k = 0; k < blueprint.sizeZ; ++k) {
					int xCoord = i + x - blueprint.anchorX;
					int yCoord = j + y - blueprint.anchorY;
					int zCoord = k + z - blueprint.anchorZ;

					if (yCoord < 0 || yCoord >= context.world.getHeight()) {
						continue;
					}

					SchematicBlockBase slot = blueprint.contents[i][j][k];

					if (slot != null && !builtLocations.contains(new BlockIndex(xCoord, yCoord, zCoord))) {
						BuildingSlotBlock b = new BuildingSlotBlock();

						b.schematic = slot;
						b.x = xCoord;
						b.y = yCoord;
						b.z = zCoord;

						b.mode = Mode.Build;
						b.buildStage = 1;

						buildList.add(b);
					}
				}
			}
		}

		iteratorBuild = new BuildingSlotIterator(buildList);
		iteratorClear = new BuildingSlotIterator(clearList);
	}

	private void checkDone() {
		if (buildList.size() == 0 && clearList.size() == 0) {
			done = true;
		} else {
			done = false;
		}
	}

	@Override
	public BuildingSlot reserveNextBlock(World world) {
		return null;
	}

	@Override
	public BuildingSlot getNextBlock(World world, TileAbstractBuilder inv) {
		if (buildList.size() != 0 || clearList.size() != 0) {
			BuildingSlotBlock slot = internalGetNextBlock(world, inv);
			checkDone();

			if (slot != null) {
				return slot;
			}
		}

		checkDone();

		return null;
	}

	private BuildingSlotBlock internalGetNextBlock(World world, TileAbstractBuilder builder) {
		BuildingSlotBlock result = null;

		IInvSlot firstSlotToConsume = null;

		for (IInvSlot invSlot : InventoryIterator.getIterable(builder, ForgeDirection.UNKNOWN)) {
			if (!builder.isBuildingMaterialSlot(invSlot.getIndex())) {
				continue;
			}

			ItemStack stack = invSlot.getStackInSlot();

			if (stack != null && stack.stackSize > 0) {
				firstSlotToConsume = invSlot;
				break;
			}
		}

		// Step 1: Check the cleared
		iteratorClear.startIteration();
		while (iteratorClear.hasNext()) {
			BuildingSlotBlock slot = iteratorClear.next();

			if (slot.buildStage > clearList.getFirst().buildStage) {
				iteratorClear.reset();
				break;
			}

			if (BlockUtils.isUnbreakableBlock(world, slot.x, slot.y, slot.z)
					|| isBlockBreakCanceled(world, slot.x, slot.y, slot.z)
					|| BuildCraftAPI.isSoftBlock(world, slot.x, slot.y, slot.z)) {
				iteratorClear.remove();
				clearedLocations.add(new BlockIndex(slot.x, slot.y, slot.z));
			} else if (canDestroy(builder, context, slot)) {
				consumeEnergyToDestroy(builder, slot);
				createDestroyItems(slot);

				result = slot;
				iteratorClear.remove();
				clearedLocations.add(new BlockIndex(slot.x, slot.y, slot.z));

				break;
			}
		}

		if (result != null) {
			return result;
		}

		// Step 2: Check the built, but only if we have anything to place and enough energy
		if (firstSlotToConsume == null || builder.getBattery().getEnergyStored() < BuilderAPI.BUILD_ENERGY) {
			return null;
		}

		iteratorBuild.startIteration();

		while (iteratorBuild.hasNext()) {
			BuildingSlotBlock slot = iteratorBuild.next();

			if (slot.buildStage > buildList.getFirst().buildStage) {
				iteratorBuild.reset();
				break;
			}

			if (BlockUtils.isUnbreakableBlock(world, slot.x, slot.y, slot.z)
					|| isBlockPlaceCanceled(world, x, y, z, slot.schematic)
					|| !BuildCraftAPI.isSoftBlock(world, slot.x, slot.y, slot.z)) {
				iteratorBuild.remove();
				builtLocations.add(new BlockIndex(slot.x, slot.y, slot.z));
			} else if (builder.consumeEnergy(BuilderAPI.BUILD_ENERGY)) {
				slot.addStackConsumed(firstSlotToConsume.decreaseStackInSlot(1));
				result = slot;
				iteratorBuild.remove();
				builtLocations.add(new BlockIndex(slot.x, slot.y, slot.z));

				break;
			}
		}

		return result;
	}
}
