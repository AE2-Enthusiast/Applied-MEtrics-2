package stone.am2.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.Item;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fluids.Fluid;
import stone.am2.data.ProductionSavedData;

public class MetricsCommand extends CommandBase {
  private ProductionSavedData currentProduction = null;
  private boolean isStopped = false;

  @Override
  public void execute(MinecraftServer server, ICommandSender sender,
    String[] args) throws CommandException {
    if (args.length > 0) {
      switch (args[0]) {
      case "pause":
        if (isStopped)
          throw new CommandException("Metrics are already paused!");
        this.currentProduction = ProductionSavedData.INSTANCE;
        this.isStopped = true;
        ProductionSavedData.INSTANCE = new ProductionSavedData() {
          @Override
          public void acceptItem(Item item, short meta, long count,
            boolean isProduction) {}

          @Override
          public void acceptFluid(Fluid fluid, long count,
            boolean isProduction) {
            // TODO Auto-generated method stub
            super.acceptFluid(fluid, count, isProduction);
          }
        };
        sender.sendMessage(new TextComponentString("Paused metric gathering"));
        break;
      case "resume":
        if (!isStopped)
          throw new CommandException("Metrics aren't paused!");
        ProductionSavedData.INSTANCE = this.currentProduction;
        this.isStopped = false;
        sender.sendMessage(new TextComponentString("Resumed metric gathering"));
        break;
      default:
        throw new CommandException(
          "Arguments should be either \"pause\" or \"resume\"");
      }
    } else {
      throw new CommandException("Argument length must be greater than 1!");
    }
  }

  @Override
  public String getName() {
    return "metrics";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 2;
  }

  @Override
  public String getUsage(ICommandSender arg0) {
    return "Pauses/resumes metric gathering";
  }

}
