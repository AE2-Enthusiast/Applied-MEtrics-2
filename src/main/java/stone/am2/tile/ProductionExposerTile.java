package stone.am2.tile;

import java.util.HashMap;
import java.util.Map;

import appeng.api.AEApi;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.storage.IBaseMonitor;
import appeng.api.storage.IMEMonitorHandlerReceiver;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.GridAccessException;
import appeng.tile.grid.AENetworkTile;
import io.prometheus.metrics.core.datapoints.CounterDataPoint;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.snapshots.Unit;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMaps;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.Constants.NBT;
import stone.am2.AM2;

public class ProductionExposerTile extends AENetworkTile {

    public final Items ITEMS = new Items();
    public final Fluids FLUIDS = new Fluids();
    private Map<String, Counter> gauges = new HashMap<>();

    @Override
    public void onReady() {
        super.onReady();
        try {
            this.getProxy()
                .getStorage()
                .getInventory(AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class))
                .addListener(ITEMS, null);
        } catch (GridAccessException e) {
            // :P
        }
    }
    @Override
    public void readFromNBT(NBTTagCompound data) {
        super.readFromNBT(data);
        NBTTagCompound tags = data.getCompoundTag("am2");
        ITEMS.readFromNBT(tags);
        FLUIDS.readFromNBT(tags);
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound data) {
        super.writeToNBT(data);
        NBTTagCompound tags = new NBTTagCompound();
        tags.setTag("items", ITEMS.writeToNBT());
        tags.setTag("fluids", FLUIDS.writeToNBT(new NBTTagCompound()));
        data.setTag("am2", tags);
        return data;
    }
    
    private class Items implements IMEMonitorHandlerReceiver<IAEItemStack> {
        private static final Counter productionCounter = Counter.builder()
            .name("AE2_production")
            .help("The total number of items that have entered the system")
            .unit(new Unit("items"))
            .labelNames("modid", "id", "meta", "unlocalized")
            .register();
        private static final Counter consumptionCounter = Counter.builder()
            .name("AE2_consumption")
            .help("The total number of items that have left the system")
            .unit(new Unit("items"))
            .labelNames("modid", "id", "meta", "unlocalized")
            .register();
        private final Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> productionMap = new Reference2ReferenceOpenHashMap<>();
        private final Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> consumptionMap = new Reference2ReferenceOpenHashMap<>();
        @Override
        public boolean isValid(Object token) {
            // if we're not valid we're not even connected and not receiving this anyway
            return ProductionExposerTile.this.getProxy().isReady();
            // return true;
        }

        public void readFromNBT(NBTTagCompound data) {
            fromList(data.getTagList("production", NBT.TAG_COMPOUND), true);
            fromList(data.getTagList("consumption", NBT.TAG_COMPOUND), false);
        }

        public NBTTagCompound writeToNBT() {
            NBTTagCompound data = new NBTTagCompound();
            data.setTag("production", toList(productionMap));
            data.setTag("consumption", toList(consumptionMap));
            return data;
        }

        private NBTTagList toList(Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> map) {
            NBTTagList list = new NBTTagList();
            for (var e : map.reference2ReferenceEntrySet()) {
                for (var ee : e.getValue().short2ReferenceEntrySet()) {
                    NBTTagCompound stack = new NBTTagCompound();
                    stack.setString("item", Item.REGISTRY.getNameForObject(e.getKey()).getNamespace());
                    stack.setShort("meta", ee.getShortKey());
                    stack.setLong("count", ee.getValue().getLongValue());
                    list.appendTag(stack);
                }
            }
            return list;
        }

        private void fromList(NBTTagList list, boolean isProduction) {
            for (var $ : list) {
                NBTTagCompound itemstack = (NBTTagCompound) $;
                Item item = Item.getByNameOrId(itemstack.getString("item"));
                short meta = itemstack.getShort("meta");
                long count = itemstack.getLong("count");
                handleStack(item, meta, isProduction ? count : count * -1);
            }
        }

		private void handleStack(Item item, short meta, long count) {
        AM2.LOGGER.info("Handling stack: " + new ItemStack(item, (int) Math.abs(count), meta));
        Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> item2meta2counter = count > 0 ? productionMap : consumptionMap;
        Short2ReferenceMap<CounterDataPoint> meta2counter = item2meta2counter.computeIfAbsent(item, $ -> new Short2ReferenceAVLTreeMap<>());
        CounterDataPoint datapoint = meta2counter.computeIfAbsent(meta, $ -> {
                String translationKey;
                try {
                    translationKey = item.getTranslationKey(new ItemStack(item, 1, meta));
                } catch (Exception e) {
                    translationKey = item.getTranslationKey();
                }

                ResourceLocation id = Item.REGISTRY.getNameForObject(item);
                return (count > 0 ? productionCounter : consumptionCounter).labelValues(id.getNamespace(), id.getPath(), Short.toString(meta), translationKey);
            });
        datapoint.inc(Math.abs(count));
		}

		// the entire network storage changed for some reason (power outage)
        // doesn't affect production so idc
        @Override
        public void onListUpdate() {
            
        }

        // incremental change, stack size is negative for stacks taken from network
        @Override
        public void postChange(IBaseMonitor<IAEItemStack> monitor, Iterable<IAEItemStack> changes, IActionSource src) {
            AM2.LOGGER.info("posting change");
            for (IAEItemStack stack : changes) {
                if (stack.isMeaningful()) {
                    this.handleStack(stack.getItem(), (short) stack.getItemDamage(), stack.getStackSize());
                }
            }
        }
    }
    
    private class Fluids implements IMEMonitorHandlerReceiver<IAEFluidStack> {
        @Override
        public boolean isValid(Object token) {
            return true;
        }

        public void readFromNBT(NBTTagCompound data) {
            //return data;
		}

        public NBTTagCompound writeToNBT(NBTTagCompound data) {
            return data;
        }

		@Override
        public void onListUpdate() {
        }

        @Override
        public void postChange(IBaseMonitor<IAEFluidStack> monitor, Iterable<IAEFluidStack> changes, IActionSource src) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("Unimplemented method 'postChange'");
        }

    }

    
}
