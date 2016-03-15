package minecraftflightsimulator.entities;

import net.minecraft.world.World;

public class EntityEngineSmall extends EntityEngine{

	public EntityEngineSmall(World world){
		super(world);
		this.setSize(1.0F, 1.0F);
	}

	public EntityEngineSmall(World world, EntityParent parent, String parentUUID, float offsetX, float offsetY, float offsetZ, int propertyCode){
		super(world, parent, parentUUID, offsetX, offsetY, offsetZ, propertyCode);
	}
	
	@Override
	protected void entityInit(){
		this.starterPower = 50;
		this.starterIncrement = 4;
		this.engineRunningSoundName = "small_engine_running";
		this.engineCrankingSoundName = "small_engine_cranking";
		this.engineStartingSoundName = "small_engine_starting";
	}
}
