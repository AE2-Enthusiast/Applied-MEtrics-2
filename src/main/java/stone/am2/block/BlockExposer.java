package stone.am2.block;

import appeng.block.AEBaseTileBlock;
import net.minecraft.block.material.Material;
import stone.am2.tile.TileExposer;

public class BlockExposer extends AEBaseTileBlock {
	public BlockExposer() {
		super(Material.BARRIER);
		this.setRegistryName("appliedmetrics2", "exposer");
		this.setTranslationKey("appliedmetrics2.exposer");
		this.setTileEntity(TileExposer.class);
	}
}
