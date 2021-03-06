package buildcraft.silicon.container;

import net.minecraft.entity.player.EntityPlayer;

import buildcraft.lib.gui.ContainerBCTile;
import buildcraft.lib.gui.slot.SlotBase;
import buildcraft.silicon.tile.TileAssemblyTable;

public class ContainerAssemblyTable extends ContainerBCTile<TileAssemblyTable> {
    public ContainerAssemblyTable(EntityPlayer player, TileAssemblyTable tile) {
        super(player, tile);
        addFullPlayerInventory(123);

        for(int y = 0; y < 4; y++) {
            for(int x = 0; x < 3; x++) {
                addSlotToContainer(new SlotBase(tile.inv, x + y * 3, 8 + x * 18, 36 + y * 18));
            }
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return true;
    }
}
