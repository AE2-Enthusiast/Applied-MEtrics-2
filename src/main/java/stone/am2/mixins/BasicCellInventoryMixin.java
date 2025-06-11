package stone.am2.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.me.storage.BasicCellInventory;
import stone.am2.data.ProductionSavedData;

@Mixin(value = BasicCellInventory.class, remap = false)
public abstract class BasicCellInventoryMixin {
  @Inject(method = "injectItems", at = @At("RETURN"))
  public <T extends IAEStack<T>> void onInjectItems(T input, Actionable mode,
    IActionSource source, CallbackInfoReturnable<T> cir) {
    if (mode == Actionable.MODULATE) {
      if (input != null) {
        if (input instanceof IAEItemStack stack) {
          T ret = cir.getReturnValue();
          long rejected = ret == null ? 0 : ret.getStackSize();
          ProductionSavedData.INSTANCE.acceptItem(stack.getItem(),
            (short) stack.getItemDamage(), stack.getStackSize() - rejected,
            true);
        } else if (input instanceof IAEFluidStack stack) {
          T ret = cir.getReturnValue();
          long rejected = ret == null ? 0 : ret.getStackSize();
          ProductionSavedData.INSTANCE.acceptFluid(stack.getFluid(),
            stack.getStackSize() - rejected, true);
        }
      }
    }
  }

  @Inject(method = "extractItems", at = @At("RETURN"))
  public <T extends IAEStack<T>> void onExtractItems(T request, Actionable mode,
    IActionSource src, CallbackInfoReturnable<T> cir) {
    if (mode == Actionable.MODULATE) {
      T ret = cir.getReturnValue();
      if (ret != null) {
        if (ret instanceof IAEItemStack stack)
          ProductionSavedData.INSTANCE.acceptItem(stack.getItem(),
            (short) stack.getItemDamage(), stack.getStackSize(), false);
        else if (ret instanceof IAEFluidStack stack) {
          ProductionSavedData.INSTANCE.acceptFluid(stack.getFluid(),
            stack.getStackSize(), false);
        }
      }
    }
  }
}