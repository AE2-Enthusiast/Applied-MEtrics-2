package stone.am2.data;

import appeng.fluids.util.AEFluidStack;
import io.prometheus.metrics.core.datapoints.CounterDataPoint;
import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.snapshots.Unit;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceAVLTreeMap;
import it.unimi.dsi.fastutil.shorts.Short2ReferenceMap;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.util.Constants.NBT;
import stone.am2.AM2;

public class ProductionSavedData extends WorldSavedData {
  private static final String NAME = AM2.MODID + ":production_data";

  public static ProductionSavedData INSTANCE = null;

  public final Items ITEMS = new Items();
  public final Fluids FLUIDS = new Fluids();

  public ProductionSavedData(String name) {
    super(name);
  }

  public ProductionSavedData() {
    this(NAME);
  }

  @Override
  public void readFromNBT(NBTTagCompound data) {
    AM2.LOGGER.info("Reading from NBT @ {}!", this);
    AM2.LOGGER.info("Reading: {}", data);
    NBTTagCompound tags = data.getCompoundTag("am2");
    ITEMS.readFromNBT(tags.getCompoundTag("items"));
    FLUIDS.readFromNBT(tags.getCompoundTag("fluids"));
  }

  @Override
  public NBTTagCompound writeToNBT(NBTTagCompound data) {
    AM2.LOGGER.info("Writing to NBT @ {}!", this);
    NBTTagCompound tags = new NBTTagCompound();
    tags.setTag("items", ITEMS.writeToNBT());
    tags.setTag("fluids", FLUIDS.writeToNBT(new NBTTagCompound()));
    data.setTag("am2", tags);
    AM2.LOGGER.info("Writing: {}", data);
    return data;
  }

  public void acceptItem(Item item, short meta, long count,
    boolean isProduction) {
    ITEMS.handleStack(item, meta, isProduction ? count : -count);
    this.markDirty();
  }

  public void acceptFluid(AEFluidStack stack) {

    this.markDirty();
  }

  public static ProductionSavedData get(World world) {
    MapStorage storage = world.getMapStorage();
    ProductionSavedData instance = (ProductionSavedData) storage
      .getOrLoadData(ProductionSavedData.class, NAME);

    if (instance == null) {
      instance = new ProductionSavedData();
      storage.setData(NAME, instance);
    }
    return instance;
  }

  private class Items {
    private static final Counter productionCounter = Counter.builder()
      .name("AE2_production")
      .help("The total number of items that have entered the system")
      .unit(new Unit("items")).labelNames("modid", "id", "meta", "unlocalized")
      .register();
    private static final Counter consumptionCounter = Counter.builder()
      .name("AE2_consumption")
      .help("The total number of items that have left the system")
      .unit(new Unit("items")).labelNames("modid", "id", "meta", "unlocalized")
      .register();
    private final Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> productionMap = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> consumptionMap = new Reference2ReferenceOpenHashMap<>();

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

    private NBTTagList toList(
      Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> map) {
      NBTTagList list = new NBTTagList();
      for (var e : map.reference2ReferenceEntrySet()) {
        String id = e.getKey().getRegistryName().toString();
        for (var ee : e.getValue().short2ReferenceEntrySet()) {
          NBTTagCompound stack = new NBTTagCompound();
          stack.setString("item", id);
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
        handleStack(item, meta, isProduction ? count : -count);
      }
    }

    private void handleStack(Item item, short meta, long count) {
      AM2.LOGGER.info("Handling stack: {}{}", count < 0 ? '-' : "",
        new ItemStack(item, (int) Math.abs(count), meta));
      Reference2ReferenceMap<Item, Short2ReferenceMap<CounterDataPoint>> item2meta2counter = count > 0
        ? productionMap
        : consumptionMap;
      Short2ReferenceMap<CounterDataPoint> meta2counter = item2meta2counter
        .computeIfAbsent(item, $ -> new Short2ReferenceAVLTreeMap<>());
      CounterDataPoint datapoint = meta2counter.computeIfAbsent(meta, $ -> {
        String translationKey;
        try {
          translationKey = item.getTranslationKey(new ItemStack(item, 1, meta));
        } catch (Exception e) {
          translationKey = item.getTranslationKey();
        }

        ResourceLocation id = Item.REGISTRY.getNameForObject(item);
        return (count > 0 ? productionCounter : consumptionCounter).labelValues(
          id.getNamespace(), id.getPath(), Short.toString(meta),
          translationKey);
      });
      datapoint.inc(Math.abs(count));
    }
  }

  private class Fluids {
    public void readFromNBT(NBTTagCompound data) {
      // return data;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound data) {
      return data;
    }

  }

  public static class ProductionNullData extends ProductionSavedData {

    @Override
    public void acceptItem(Item item, short meta, long count,
      boolean isProduction) {}

    @Override
    public void acceptFluid(AEFluidStack stack) {}

  }
}
