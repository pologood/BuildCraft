package buildcraft.lib.client.guide.parts;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import buildcraft.lib.client.guide.GuiGuide;

@FunctionalInterface
@SideOnly(Side.CLIENT)
public interface GuidePartFactory {
    GuidePart createNew(GuiGuide gui);
}
