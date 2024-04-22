package stone.am2;

import java.io.IOException;

import io.prometheus.metrics.exporter.httpserver.HTTPServer;
import io.prometheus.metrics.instrumentation.jvm.JvmMetrics;
import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import stone.am2.block.BlockExposer;
import stone.am2.tile.TileExposer;

@Mod(modid = AM2.MODID, name = AM2.NAME, version = AM2.VERSION, dependencies = "required:appliedenergistics2")
public class AM2 {

	public static class ServerProxy {
		@SubscribeEvent
		public void registerBlocks(RegistryEvent.Register<Block> event) {
			event.getRegistry().register(EXPOSER);

		}

		@SubscribeEvent
		public void registerItems(RegistryEvent.Register<Item> event) {
			event.getRegistry().register(EXPOSER_ITEM);
		}

		public void preInit(FMLPreInitializationEvent event) {
			GameRegistry.registerTileEntity(TileExposer.class, new ResourceLocation(MODID, "exposer"));
		}
	}

	public static class ClientProxy extends ServerProxy {
		@Override
		public void preInit(FMLPreInitializationEvent event) {
			super.preInit(event);
			ModelResourceLocation itemModelResourceLocation = new ModelResourceLocation("appliedmetrics2:exposer",
					"inventory");
			ModelLoader.setCustomModelResourceLocation(EXPOSER_ITEM, 0, itemModelResourceLocation);
		}
	}

	@SidedProxy
	public static ServerProxy proxy;

	public static final String MODID = "appliedmetrics2";
	public static final String NAME = "Applied MEtrics 2";
	public static final String VERSION = "1.0.0";

	public static final Block EXPOSER = new BlockExposer();
	public static final Item EXPOSER_ITEM = new ItemBlock(EXPOSER).setRegistryName(MODID, "exposer");

	@EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(proxy);
		proxy.preInit(event);
	}

	@EventHandler
	public void init(FMLInitializationEvent event) {
		if (event.getSide() == Side.SERVER) {
			System.out.println("starting HTTP server");
			JvmMetrics.builder().register();

			try {
				HTTPServer server = HTTPServer.builder().port(25564).buildAndStart();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
