package minecrafttransportsimulator.packets.components;

import io.netty.buffer.ByteBuf;
import minecrafttransportsimulator.mcinterface.IWrapperPlayer;
import minecrafttransportsimulator.mcinterface.IWrapperWorld;
import minecrafttransportsimulator.mcinterface.MasterLoader;
import minecrafttransportsimulator.vehicles.main.AEntityBase;

/**Packet class that includes a default implementation for transmitting an entity
 * to allow entity-specific interactions on the other side of the network.
 *
 * @author don_bruce
 */
public abstract class APacketEntity extends APacketBase{
	private final int entityID;
	
	public APacketEntity(AEntityBase entity){
		super(null);
		this.entityID = entity.lookupID;
	}
	
	public APacketEntity(ByteBuf buf){
		super(buf);
		this.entityID = buf.readInt();
	};

	@Override
	public void writeToBuffer(ByteBuf buf){
		super.writeToBuffer(buf);
		buf.writeInt(entityID);
	}
	
	@Override
	public void handle(IWrapperWorld world, IWrapperPlayer player){
		boolean sendReturnPacket = false;
		for(AEntityBase entity : (world.isClient() ? AEntityBase.createdClientEntities : AEntityBase.createdServerEntities)){
			if(entity.lookupID == entityID){
				if(handle(world, player, entity) && !world.isClient()){
					sendReturnPacket = true;
				}
			}
		}
		if(sendReturnPacket){
			MasterLoader.networkInterface.sendToAllClients(this);
		}
	}
	
	/**
	 *  Handler method with an extra parameter for the entity that this packet
	 *  is associated with. If the entity is null,  then this method won't be called.
	 *  Saves having to do null checks for every packet type.  If this is handled on the 
	 *  server, and a packet shouldn't be sent to all clients (like if the action failed due
	 *   to an issue) return false.  Otherwise, return true to send this packet on to all clients.  
	 *   Return method has no function on clients.
	 */
	protected abstract boolean handle(IWrapperWorld world, IWrapperPlayer player, AEntityBase entity);
}
