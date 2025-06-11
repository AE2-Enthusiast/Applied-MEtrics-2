package stone.am2;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import stone.am2.block.BlockExposer;
import stone.am2.block.ProductionExposerBlock;
import stone.am2.command.MetricsCommand;
import stone.am2.data.ProductionSavedData;
import stone.am2.tile.ProductionExposerTile;
import stone.am2.tile.TileExposer;

@Mod(modid = AM2.MODID, name = AM2.NAME, version = AM2.VERSION, dependencies = "required:appliedenergistics2")
public class AM2 {

  public static class ServerProxy {
    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> event) {
      event.getRegistry().register(EXPOSER);
      event.getRegistry().register(PRODUCTION_EXPOSER);

    }

    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
      event.getRegistry().register(EXPOSER_ITEM);
      event.getRegistry().register(PRODUCTION_EXPOSER_ITEM);
    }

    public void preInit(FMLPreInitializationEvent event) {
      GameRegistry.registerTileEntity(TileExposer.class,
        new ResourceLocation(MODID, "exposer"));
      GameRegistry.registerTileEntity(ProductionExposerTile.class,
        new ResourceLocation(MODID, "production_exposer"));
    }

    @SubscribeEvent
    public void onWorldLoad(WorldEvent.Load event) {
      // if (ProductionSavedData.INSTANCE == null) {
      if (ProductionSavedData.INSTANCE == null && !event.getWorld().isRemote) {
        ProductionSavedData.INSTANCE = ProductionSavedData
          .get(event.getWorld());
        AM2.LOGGER.info("Loaded ProductionSavedData {}!",
          ProductionSavedData.get(event.getWorld()));
      }
      // }
    }

  }

  public static class ClientProxy extends ServerProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
      super.preInit(event);
      ModelLoader.setCustomModelResourceLocation(EXPOSER_ITEM, 0,
        new ModelResourceLocation("appliedmetrics2:exposer", "inventory"));
      ModelLoader.setCustomModelResourceLocation(PRODUCTION_EXPOSER_ITEM, 0,
        new ModelResourceLocation("appliedmetrics2:production_exposer",
          "inventory"));
    }
  }

  @SidedProxy
  public static ServerProxy proxy;

  public static final String MODID = "appliedmetrics2";
  public static final String NAME = "Applied MEtrics 2";
  public static final String VERSION = "1.1.0";
  public static final Logger LOGGER = LogManager.getLogger(MODID);

  public static final Block EXPOSER = new BlockExposer();
  public static final Item EXPOSER_ITEM = new ItemBlock(EXPOSER)
    .setRegistryName(MODID, "exposer");

  public static final Block PRODUCTION_EXPOSER = new ProductionExposerBlock();
  public static final Item PRODUCTION_EXPOSER_ITEM = new ItemBlock(
    PRODUCTION_EXPOSER).setRegistryName(MODID, "production_exposer");

  public static HTTPServer SERVER;

  @EventHandler
  public void preInit(FMLPreInitializationEvent event) {
    MinecraftForge.EVENT_BUS.register(proxy);
    proxy.preInit(event);
  }

  @EventHandler
  public void init(FMLInitializationEvent event) {
    if (event.getSide() == Side.SERVER) {
      LOGGER.info("Starting HTTP server");
      JvmMetrics.builder().register();

      try {
        SERVER = HTTPServer.builder().port(25564).buildAndStart();
      } catch (IOException e) {
        LOGGER.error("HTTP server could not start. Metrics won't work!", e);
      }
    }
  }

  @EventHandler
  public void onServerStart(FMLServerStartingEvent event) {
    event.registerServerCommand(new MetricsCommand());
  }
}
