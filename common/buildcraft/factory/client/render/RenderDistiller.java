package buildcraft.factory.client.render;

import java.util.EnumMap;
import java.util.Map;

import org.lwjgl.opengl.GL11;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.Vec3d;

import net.minecraftforge.fluids.FluidStack;

import buildcraft.factory.BCFactoryBlocks;
import buildcraft.factory.tile.TileDistiller_BC8;
import buildcraft.lib.block.BlockBCBase_Neptune;
import buildcraft.lib.client.render.fluid.FluidRenderer;
import buildcraft.lib.client.render.fluid.FluidSpriteType;
import buildcraft.lib.fluids.Tank;
import buildcraft.lib.misc.VecUtil;

public class RenderDistiller extends TileEntitySpecialRenderer<TileDistiller_BC8> {
    private static final Map<EnumFacing, TankRenderSizes> TANK_SIZES = new EnumMap<>(EnumFacing.class);

    static {
        EnumFacing face = EnumFacing.WEST;
        Size tankIn = new Size(0, 0, 4, 8, 16, 12).shrink(1 / 64.0);
        Size tankOutGas = new Size(8, 8, 0, 16, 16, 16).shrink(1 / 64.0);
        Size tankOutLiquid = new Size(8, 0, 0, 16, 8, 16).shrink(1 / 64.0);
        TankRenderSizes sizes = new TankRenderSizes(tankIn, tankOutGas, tankOutLiquid);
        for (int i = 0; i < 4; i++) {
            TANK_SIZES.put(face, sizes);
            face = face.rotateY();
            sizes = sizes.rotateY();
        }
    }

    @Override
    public void renderTileEntityAt(TileDistiller_BC8 tile, double x, double y, double z, float partialTicks, int destroyStage) {
        super.renderTileEntityAt(tile, x, y, z, partialTicks, destroyStage);

        IBlockState state = tile.getWorld().getBlockState(tile.getPos());
        if (state.getBlock() != BCFactoryBlocks.distiller) {
            return;
        }

        Minecraft.getMinecraft().mcProfiler.startSection("bc");
        Minecraft.getMinecraft().mcProfiler.startSection("distiller");

        int combinedLight = tile.getWorld().getCombinedLight(tile.getPos(), 0);
        EnumFacing face = state.getValue(BlockBCBase_Neptune.PROP_FACING);
        TankRenderSizes sizes = TANK_SIZES.get(face);

        // gl state setup
        RenderHelper.disableStandardItemLighting();
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);

        // buffer setup
        VertexBuffer vb = Tessellator.getInstance().getBuffer();
        vb.begin(GL11.GL_QUADS, DefaultVertexFormats.BLOCK);
        vb.setTranslation(x, y, z);

        renderTank(sizes.tankIn, tile.tankIn, combinedLight, vb);
        renderTank(sizes.tankOutGas, tile.tankOutGas, combinedLight, vb);
        renderTank(sizes.tankOutLiquid, tile.tankOutLiquid, combinedLight, vb);

        // buffer finish
        vb.setTranslation(0, 0, 0);
        Tessellator.getInstance().draw();

        // gl state finish
        RenderHelper.enableStandardItemLighting();

        Minecraft.getMinecraft().mcProfiler.endSection();
        Minecraft.getMinecraft().mcProfiler.endSection();
    }

    private static void renderTank(Size size, Tank tank, int combinedLight, VertexBuffer vb) {
        FluidStack fluid = tank.getFluidForRender();
        if (fluid == null || fluid.amount <= 0) {
            return;
        }
        int blockLight = fluid.getFluid().getLuminosity(fluid) & 0xF;
        combinedLight |= blockLight << 4;
        FluidRenderer.vertex.lighti(combinedLight);
        FluidRenderer.renderFluid(FluidSpriteType.STILL, fluid, tank.getCapacity(), size.min, size.max, vb, null);
    }

    static class TankRenderSizes {
        final Size tankIn, tankOutGas, tankOutLiquid;

        public TankRenderSizes(Size tankIn, Size tankOutGas, Size tankOutLiquid) {
            this.tankIn = tankIn;
            this.tankOutGas = tankOutGas;
            this.tankOutLiquid = tankOutLiquid;
        }

        public TankRenderSizes rotateY() {
            return new TankRenderSizes(tankIn.rotateY(), tankOutGas.rotateY(), tankOutLiquid.rotateY());
        }
    }

    static class Size {
        final Vec3d min, max;

        public Size(int sx, int sy, int sz, int ex, int ey, int ez) {
            this(new Vec3d(sx, sy, sz).scale(1 / 16.0), new Vec3d(ex, ey, ez).scale(1 / 16.0));
        }

        public Size(Vec3d min, Vec3d max) {
            this.min = min;
            this.max = max;
        }

        public Size shrink(double by) {
            return new Size(min.addVector(by, by, by), max.subtract(by, by, by));
        }

        public Size rotateY() {
            Vec3d _min = rotateY(min);
            Vec3d _max = rotateY(max);
            return new Size(VecUtil.min(_min, _max), VecUtil.max(_min, _max));
        }

        private static Vec3d rotateY(Vec3d vec) {
            return new Vec3d(//
                1 - vec.zCoord,//
                vec.yCoord,//
                vec.xCoord//
            );
        }
    }
}
