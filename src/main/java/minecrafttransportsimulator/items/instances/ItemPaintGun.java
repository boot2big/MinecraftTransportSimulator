package minecrafttransportsimulator.items.instances;

import java.util.List;

import minecrafttransportsimulator.baseclasses.Point3i;
import minecrafttransportsimulator.blocks.components.ABlockBase.Axis;
import minecrafttransportsimulator.blocks.tileentities.components.ATileEntityBase;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityDecor;
import minecrafttransportsimulator.guis.instances.GUIPaintGun;
import minecrafttransportsimulator.items.components.AItemBase;
import minecrafttransportsimulator.items.components.IItemVehicleInteractable;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.packets.instances.PacketPlayerChatMessage;
import minecrafttransportsimulator.vehicles.main.EntityVehicleF_Physics;
import minecrafttransportsimulator.vehicles.parts.APart;

public class ItemPaintGun extends AItemBase implements IItemVehicleInteractable{
	
	@Override
	public void addTooltipLines(List<String> tooltipLines, IWrapperNBT data){
		tooltipLines.add(MasterLoader.coreInterface.translate("info.item.paintgun.use"));
	}
	
	@Override
	public CallbackType doVehicleInteraction(EntityVehicleF_Physics vehicle, APart part, IWrapperPlayer player, PlayerOwnerState ownerState, boolean rightClick){
		//If the player isn't the owner of the vehicle, they can't interact with it.
		if(!ownerState.equals(PlayerOwnerState.USER)){
			if(rightClick){
				if(vehicle.world.isClient()){
					MasterLoader.guiInterface.openGUI(new GUIPaintGun(vehicle, player));
				}else{
					return CallbackType.PLAYER;
				}
			}
		}else{
			player.sendPacket(new PacketPlayerChatMessage("interact.failure.vehicleowned"));
		}
		return CallbackType.NONE;
	}
	
	@Override
	public boolean onBlockClicked(IWrapperWorld world, IWrapperPlayer player, Point3i point, Axis axis){
		if(world.isClient()){
			ATileEntityBase<?> tile = world.getTileEntity(point);
			if(tile instanceof TileEntityDecor){
				MasterLoader.guiInterface.openGUI(new GUIPaintGun((TileEntityDecor) tile, player));
				return true;
			}
		}
		return false;
	}
}
