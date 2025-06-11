package stone.am2;

import java.util.ArrayList;
import java.util.List;

import zone.rong.mixinbooter.ILateMixinLoader;

public class Plugin implements ILateMixinLoader {

  @Override
  public List<String> getMixinConfigs() {
    List<String> configs = new ArrayList<>();
    configs.add("mixins.appliedmetrics2.json");
    return configs;
  }

}
