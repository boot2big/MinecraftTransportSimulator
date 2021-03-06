package minecrafttransportsimulator.packets.instances;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad;
import minecrafttransportsimulator.blocks.tileentities.instances.TileEntityRoad.RoadComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.mcinterface.IWrapperNBT;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.packets.components.APacketTileEntity;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Packet sent to roads to change their states.  This gets sent when a player clicks a road on the client.
 * Packet is sent to the server to change the road state to match what item the player is holding.
 * If the player isn't holding an item, they may have wreneched the component to remove it.
 * 
 * @author don_bruce
 */
public class PacketTileEntityRoadChange extends APacketTileEntity<TileEntityRoad>{
	private final RoadComponent componentType;
	private final ItemRoadComponent componentItem;
	
	public PacketTileEntityRoadChange(TileEntityRoad road, RoadComponent componentType, ItemRoadComponent componentItem){
		super(road);
		this.componentType = componentType;
		this.componentItem = componentItem;
	}
	
	public PacketTileEntityRoadChange(ByteBuf buf){
		super(buf);
		this.componentType = RoadComponent.values()[buf.readByte()];
		if(buf.readBoolean()){
			this.componentItem = PackParserSystem.getItem(readStringFromBuffer(buf), readStringFromBuffer(buf));
		}else{
			this.componentItem = null;
		}
	}
	
	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeByte(componentType.ordinal());
		if(componentItem != null){
			buf.writeBoolean(true);
			writeStringToBuffer(componentItem.definition.packID, buf);
			writeStringToBuffer(componentItem.definition.systemName, buf);
		}else{
			buf.writeBoolean(false);
		}
	}
	
	@Override
	protected boolean handle(IWrapperWorld world, IWrapperPlayer player, TileEntityRoad road){
		if(componentItem != null){
			//Player clicked with a component.  Add/change it.
			road.components.put(componentType, componentItem);
			if(!player.isCreative()){
				player.getInventory().removeStack(player.getHeldStack(), 1);
			}
			return true;
		}else{
			//Player clicked with a wrench, try to remove the component.
			if(road.components.containsKey(componentType)){
				ItemRoadComponent component = road.components.get(componentType);
				IWrapperNBT data = null;
				if(world.isClient() || player.isCreative() || player.getInventory().addItem(component, data)){
					road.components.remove(component);
					return true;
				}
			}
		}
		return false;
	}
}
