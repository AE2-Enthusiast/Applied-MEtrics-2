package stone.am2.block;

import appeng.block.AEBaseTileBlock;
import net.minecraft.block.material.Material;
import stone.am2.tile.ProductionExposerTile;

public class ProductionExposerBlock extends AEBaseTileBlock {
	public ProductionExposerBlock() {
		super(Material.BARRIER);
		this.setRegistryName("appliedmetrics2", "production_exposer");
		this.setTranslationKey("appliedmetrics2.production_exposer");
		this.setTileEntity(ProductionExposerTile.class);
	}

}
