package stone.am2.tile;

import java.util.HashMap;
import java.util.Map;

import appeng.api.AEApi;
import appeng.api.networking.events.MENetworkEventSubscribe;
import appeng.api.networking.events.MENetworkStorageEvent;
import appeng.api.storage.IStorageChannel;
import appeng.api.storage.channels.IFluidStorageChannel;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;
import appeng.tile.grid.AENetworkTile;
import io.prometheus.metrics.core.datapoints.GaugeDataPoint;
import io.prometheus.metrics.core.metrics.Gauge;
import io.prometheus.metrics.model.snapshots.Unit;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

public class TileExposer extends AENetworkTile {

	private static Map<String, Map<Object, Map<Integer, GaugeDataPoint>>> datapoints = new HashMap<>();
	private static Map<String, Gauge> gauges = new HashMap<>();

	@MENetworkEventSubscribe
	public void onStorageEvent(MENetworkStorageEvent event) {
		String channelStr = channel2String(event.channel);
		if (channelStr == null)
			return;
		Map<Object, Map<Integer, GaugeDataPoint>> gaugeMap = datapoints.computeIfAbsent(channelStr, (channel) -> {
			return new HashMap<Object, Map<Integer, GaugeDataPoint>>();
		});

		switch (channelStr) {
		case "item":
			handleItems(gaugeMap, event.monitor.getStorageList());
			break;
		}
	}

	private void handleItems(Map<Object, Map<Integer, GaugeDataPoint>> itemMap, IItemList<IAEItemStack> storageList) {
		for (IAEItemStack stack : storageList) {
			if (stack.isMeaningful()) {
				Item itemType = stack.getItem();
				if (itemType.isDamageable())
					continue;
				int meta = stack.getItemDamage();
				GaugeDataPoint datapoint = itemMap.computeIfAbsent(itemType, item -> {
					return new HashMap<Integer, GaugeDataPoint>();
				}).computeIfAbsent(meta, m -> {
					Gauge gauge = gauges.computeIfAbsent("item", channel -> {
						return Gauge.builder().name("AE2_item_count").help("The counts of each item in the ME system")
								.labelNames("modid", "id", "meta").unit(new Unit("items")).register();
					});
					ResourceLocation name = itemType.getRegistryName();
					return gauge.labelValues(name.getNamespace(), name.getPath(), String.valueOf(m));
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

	private static String channel2Unit(String str) {
		switch (str) {
		case "item":
			return "items";
		case "fluid":
			return "milliBuckets";
		default:
			return null;
		}
	}
}
