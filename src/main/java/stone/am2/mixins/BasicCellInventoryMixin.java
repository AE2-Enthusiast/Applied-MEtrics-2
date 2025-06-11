package stone.am2.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEStack;
import appeng.me.storage.BasicCellInventory;
import appeng.util.item.AEItemStack;
import stone.am2.data.ProductionSavedData;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class BasicCellInventoryMixin {
  @Inject(method = "injectItems", at = @At("RETURN"))
  public <T extends IAEStack<T>> void onInjectItems(T input, Actionable mode,
    IActionSource source, CallbackInfoReturnable<T> cir) {
    if (mode == Actionable.MODULATE) {
      if (input != null && input instanceof AEItemStack stack) {
        T ret = cir.getReturnValue();
        long rejected = ret == null ? 0 : ret.getStackSize();
        ProductionSavedData.INSTANCE.acceptItem(stack.getItem(),
          (short) stack.getItemDamage(), stack.getStackSize() - rejected, true);
      }
    }
  }

  @Inject(method = "extractItems", at = @At("RETURN"))
  public <T extends IAEStack<T>> void onExtractItems(T request, Actionable mode,
    IActionSource src, CallbackInfoReturnable<T> cir) {
    if (mode == Actionable.MODULATE) {
      T ret = cir.getReturnValue();
      if (ret != null && ret instanceof AEItemStack stack) {
        ProductionSavedData.INSTANCE.acceptItem(stack.getItem(),
          (short) stack.getItemDamage(), stack.getStackSize(), false);
      }
    }
  }
}