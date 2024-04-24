package stone.am2.tile;

import java.util.HashMap;
import java.util.Map;

import appeng.api.AEApi;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkStorageEvent;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.tile.grid.AENetworkTile;
import io.prometheus.metrics.core.datapoints.GaugeDataPoint;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.snapshots.Unit;
import net.minecraft.item.Item;
import net.minecraftforge.fluids.Fluid;

public class TileExposer extends AENetworkTile {

	private static Map<Item, Map<Integer, GaugeDataPoint>> itemDatapoints = new HashMap<>();
	private static Map<Fluid, GaugeDataPoint> fluidDatapoints = new HashMap<>();
	private static Map<String, Gauge> gauges = new HashMap<>();

	private long lastTimeStamp = -1;

	@MENetworkEventSubscribe
	public void onStorageEvent(MENetworkStorageEvent event) {
		long current = System.currentTimeMillis();
		if (lastTimeStamp + 10000 < current) {
			lastTimeStamp = current;
		} else {
			return;
		}
		String channelStr = channel2String(event.channel);
		if (channelStr == null)
			return;

		switch (channelStr) {
		case "item":
			handleItems(itemDatapoints, event.monitor.getStorageList());
			break;
		case "fluid":
			handleFluids(fluidDatapoints, event.monitor.getStorageList());
			break;
		}
	}

	private void handleFluids(Map<Fluid, GaugeDataPoint> fluidMap, IItemList<IAEFluidStack> storageList) {
		for (IAEFluidStack stack : storageList) {
			if (stack.isMeaningful()) {
				Fluid fluidType = stack.getFluid();
				GaugeDataPoint datapoint = fluidMap.computeIfAbsent(fluidType, fluid -> {
					Gauge gauge = gauges.computeIfAbsent("fluid", channel -> {
						return Gauge.builder().name("AE2_fluid_count").help("The counts of each fluid in the ME system")
								.labelNames("unlocalized").unit(new Unit("milliBuckets")).register();
					});
					return gauge.labelValues(fluidType.getUnlocalizedName());
				});

				datapoint.set(stack.getStackSize());
			}
		}
	}

	private void handleItems(Map<Item, Map<Integer, GaugeDataPoint>> itemMap, IItemList<IAEItemStack> storageList) {
		for (IAEItemStack stack : storageList) {
			if (stack.isMeaningful()) {
				Item itemType = stack.getItem();
				if (itemType.isDamageable())
					continue;
				if (stack.hasTagCompound())
					continue;
				int meta = stack.getItemDamage();
				GaugeDataPoint datapoint = itemMap.computeIfAbsent(itemType, item -> {
					return new HashMap<Integer, GaugeDataPoint>();
				}).computeIfAbsent(meta, m -> {
					Gauge gauge = gauges.computeIfAbsent("item", channel -> {
						return Gauge.builder().name("AE2_item_count").help("The counts of each item in the ME system")
								.labelNames("unlocalized").unit(new Unit("items")).register();
					});
					try {
						return gauge.labelValues(itemType.getTranslationKey(stack.createItemStack()));
					} catch (Exception e) {
						return gauge.labelValues(itemType.getTranslationKey());
					}
				});

				datapoint.set(stack.getStackSize());
			}
		}
	}

	private static String channel2String(IStorageChannel<?> channel) {
		if (channel == AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class)) {
			return "item";
		} else if (channel == AEApi.instance().storage().getStorageChannel(IFluidStorageChannel.class)) {
			return "fluid";
		} else {
			return null;
		}
	}
}
