package minecrafttransportsimulator.entities.core;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableList;

import cpw.mods.fml.common.Loader;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.baseclasses.MTSVector;
import minecrafttransportsimulator.dataclasses.MTSEntity;
import minecrafttransportsimulator.dataclasses.MTSRegistry;
import minecrafttransportsimulator.entities.parts.EntitySeat;
import minecrafttransportsimulator.items.ItemPlane;
import minecrafttransportsimulator.minecrafthelpers.AABBHelper;
import minecrafttransportsimulator.minecrafthelpers.EntityHelper;
import minecrafttransportsimulator.minecrafthelpers.ItemStackHelper;
import minecrafttransportsimulator.minecrafthelpers.PlayerHelper;
import minecrafttransportsimulator.packets.general.ServerSyncPacket;
import minecrafttransportsimulator.systems.ConfigSystem;
import minecrafttransportsimulator.systems.RotationSystem;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public abstract class EntityParent extends EntityMultipartBase{
	public boolean rendered;
	public byte numberChildren;
	public float rotationRoll;
	public float prevRotationRoll;
	public float pitchCorrection;
	public float yawCorrection;
	public float rollCorrection;
	
	/**
	 * Map that contains child mappings.  Keyed by child's UUID.
	 * Note that this is for moving and linking children, and will be empty until
	 * children get linked.
	 */
	private Map<String, EntityChild> children = new HashMap<String, EntityChild>();
	
	/**
	 * Array containing locations of all parts.
	 * All parts should be initialized in entity's {@link initPartData} method.
	 * Note that core entities should NOT be put here, as they're
	 * directly linked to the parent and can't be added manually.
	 */
	protected List<PartData> partData;
	
	public EntityParent(World world){
		super(world);
		this.setSize(0.75F, 0.75F);
		this.preventEntitySpawning = false;
	}
	
	public EntityParent(World world, float posX, float posY, float posZ, float playerRotation){
		this(world);
		this.setPositionAndRotation(posX, posY, posZ, playerRotation-90, 0);
		this.UUID=String.valueOf(this.getUniqueID());
		this.numberChildren=(byte) this.getCoreLocations().length;
	}
	
	@Override
	protected void entityInit(){
		partData = new ArrayList<PartData>();
		this.initPartData();
	}
	
	@Override
	public boolean performRightClickAction(MTSEntity clicked, EntityPlayer player){
		//No in-use changes for sneaky sneaks!
		if(player.ridingEntity instanceof EntitySeat){
			if(this.equals(((EntitySeat) player.ridingEntity).parent)){
				return false;
			}
		}
		if(!worldObj.isRemote){
			ItemStack heldStack = PlayerHelper.getHeldStack(player);
			if(heldStack != null){
				if(ItemStackHelper.getItemFromStack(heldStack).equals(MTSRegistry.wrench)){
					return false;
				}
				//Verify that the item is registered as a spawnable part.
				Class<? extends EntityChild> childClassToSpawn = MTSRegistry.entityItems.get(ItemStackHelper.getItemFromStack(heldStack));
				if(childClassToSpawn != null){
					//Now find the closest spot to put it.
					EntityChild childClicked = (EntityChild) clicked;
					PartData dataToSpawn = null;
					float closestPosition = 9999;
					for(PartData data : partData){
						for(Class<? extends EntityChild> dataClass : data.acceptableClasses){
							if(dataClass.equals(childClassToSpawn)){
								float distance = (float) Math.hypot(childClicked.offsetX - data.offsetX, childClicked.offsetZ - data.offsetZ);
								if(distance < closestPosition){
									//Make sure a part doesn't exist already.
									boolean childPresent = false;
									for(EntityChild child : children.values()){
										if(child.offsetX == data.offsetX && child.offsetY == data.offsetY && child.offsetZ == data.offsetZ){
											childPresent = true;
											break;
										}else if(child instanceof EntityGroundDevice){
											if(child.offsetX == data.alternateOffsetX && child.offsetY == data.alternateOffsetY && child.offsetZ == data.alternateOffsetZ){
												childPresent = true;
												break;
											}
										}
									}
									if(!childPresent){
										closestPosition = distance;
										dataToSpawn = data;
									}
								}
							}
						}					
					}
					if(dataToSpawn != null){
						//We have the correct class, now time to spawn it.
						try{
							Constructor<? extends EntityChild> construct = childClassToSpawn.getConstructor(World.class, EntityParent.class, String.class, float.class, float.class, float.class, int.class);
							EntityChild newChild = construct.newInstance(worldObj, this, this.UUID, dataToSpawn.offsetX, dataToSpawn.offsetY, dataToSpawn.offsetZ, ItemStackHelper.getItemDamage(heldStack));
							newChild.setNBTFromStack(heldStack);
							if(newChild instanceof EntityGroundDevice){
								float[] extendedCoords = new float[]{dataToSpawn.offsetX, dataToSpawn.offsetY, dataToSpawn.offsetZ};
								float[] retractedCoords = new float[]{dataToSpawn.alternateOffsetX, dataToSpawn.alternateOffsetY, dataToSpawn.alternateOffsetZ};
								boolean retractable = !Arrays.equals(retractedCoords, extendedCoords);
								((EntityGroundDevice) newChild).setExtraProperties(dataToSpawn.rotatesWithYaw, retractable, extendedCoords, retractedCoords);
							}else if(dataToSpawn.isController){
								((EntitySeat) newChild).setController();
							}
							this.addChild(newChild.UUID, newChild, true);
							if(!PlayerHelper.isPlayerCreative(player)){
								PlayerHelper.removeItemFromHand(player, 1);
							}
							return true;
						}catch(Exception e){
							System.err.println("ERROR SPAWING PART!");
						}
					}
				}
			}
		}else{
			ItemStack heldStack = PlayerHelper.getHeldStack(player);
			if(heldStack != null){
				if(ItemStackHelper.getItemFromStack(heldStack).equals(MTSRegistry.wrench)){
					MTS.proxy.openGUI(this, player);
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public void onEntityUpdate(){
		super.onEntityUpdate();
		if(!this.hasUUID()){return;}
		if(!linked){
			linked = children.size() == numberChildren;
			//Sometimes parts don't load right.  Need to reset the number of children then.
			if(!linked && ticksExisted == 100){
				if(children.size() == numberChildren - 1){
					System.err.println("A PART HAS FAILED TO LOAD!  SKIPPNG!");
				}else if(children.size() == numberChildren + 1){
					System.err.println("AN EXTRA PART HAS BEEN LOADED!  ADDING!");
				}else{
					return;
				}
				numberChildren = (byte) children.size();
				linked = true;
			}	
		}else if(!worldObj.isRemote && this.ticksExisted%ConfigSystem.getIntegerConfig("SyncDelay")==0){
			MTS.MFSNet.sendToAll(new ServerSyncPacket(getEntityId(), posX, posY, posZ, motionX, motionY, motionZ, rotationPitch, rotationRoll, rotationYaw));
		}
		prevRotationRoll = rotationRoll + rollCorrection;
		prevRotationPitch = rotationPitch + pitchCorrection;
		prevRotationYaw = rotationYaw + yawCorrection;
		rollCorrection = pitchCorrection = yawCorrection = 0;
	}
	
	@Override
	public void setDead(){
		super.setDead();
		for(EntityChild child : getChildren()){
			removeChild(child.UUID, false);
		}
	}
	
	//Start of custom methods
	/**
	 * Add new core sets here.  Cores are used to determine collision and are not removable
	 * (extend {@link EntityChild} to make removable things).  Core sets consist of
	 * a x, y, z, width, and height parameters.  All parameters are passed into the 
	 * {@link EntityCore} constructor upon spawn.  This method is used
	 * by {@link ItemPlane} during use to determine spawn requirements and such.
	 * @return
	 */
	public abstract float[][] getCoreLocations();
	
	/**
	 * Called from {@link EntityInit}, this is where all part data sets should be added.
	 * Method should be called by the final subclass of this class.
	 * Note that the order in which engines are added determines their number in the 
	 * HUD and other areas.
	 */
	protected abstract void initPartData();
	
	/**
	 * Spawns a child and adds a child to all appropriate mappings.
	 * Set newChild to true if parent needs to keep track of an additional child.
	 * @param childUUID
	 * @param child
	 * @param newChild
	 */
	public void addChild(String childUUID, EntityChild child, boolean newChild){
		if(!children.containsKey(childUUID)){
			children.put(childUUID, child);
			if(newChild){
				++numberChildren;
				if(!AABBHelper.getCollidingBlockBoxes(worldObj, AABBHelper.getEntityBoundingBox(child), child.collidesWithLiquids()).isEmpty()){
					float boost = Math.max(0, -child.offsetY);
					this.rotationRoll = 0;
					this.setPositionAndRotation(posX, posY + boost, posZ, rotationYaw, 0);
					child.setPosition(posX + child.offsetX, posY + child.offsetY + boost, posZ + child.offsetZ);
					
					//Sometimes children can break off if the vehicle rotates and shoves something under the ground.
					for(EntityChild testChild : this.children.values()){
						if(!AABBHelper.getCollidingBlockBoxes(worldObj, AABBHelper.getOffsetEntityBoundingBox(testChild, 0, boost, 0), testChild.collidesWithLiquids()).isEmpty()){
							this.setPositionAndRotation(posX, posY + 1, posZ, rotationYaw, 0);
							break;
						}
					}
				}
				worldObj.spawnEntityInWorld(child);
			}
		}
	}
	
	/**
	 * Removes a child from mappings, setting it dead in the process. 
	 * @param childUUID
	 */
	public void removeChild(String childUUID, boolean playBreakSound){
		if(children.containsKey(childUUID)){
			children.remove(childUUID).setDead();
			--numberChildren;
		}
		if(playBreakSound){
			MTS.proxy.playSound(this, "random.break", 2, 1);
		}
	}

	public void moveChildren(){
		for(EntityChild child : getChildren()){
			if(child.isDead){
				removeChild(child.UUID, false);
			}else{
				MTSVector offset = RotationSystem.getRotatedPoint(child.offsetX, child.offsetY, child.offsetZ, rotationPitch, rotationYaw, rotationRoll);
				child.setPosition(posX + offset.xCoord, posY + offset.yCoord, posZ + offset.zCoord);
				Entity rider = EntityHelper.getRider(child);
				if(rider != null){
					if(Loader.MC_VERSION.equals("1.7.10")){
						MTSVector posVec = RotationSystem.getRotatedPoint(child.offsetX, (float) (child.offsetY + rider.getYOffset()), (float) child.offsetZ, this.rotationPitch, this.rotationYaw, this.rotationRoll);
						rider.setPosition(this.posX + posVec.xCoord, this.posY + posVec.yCoord, this.posZ + posVec.zCoord);
					}else{
						MTSVector posVec = RotationSystem.getRotatedPoint(child.offsetX, (float) (child.offsetY + rider.getYOffset() + rider.height), (float) child.offsetZ, this.rotationPitch, this.rotationYaw, this.rotationRoll);
						rider.setPosition(this.posX + posVec.xCoord, this.posY + posVec.yCoord - rider.height, this.posZ + posVec.zCoord);
					}
					
				}
			}
		}
	}
	
	public EntityChild[] getChildren(){return ImmutableList.copyOf(children.values()).toArray(new EntityChild[children.size()]);}
		
    @Override
	public void readFromNBT(NBTTagCompound tagCompound){
		super.readFromNBT(tagCompound);
		this.numberChildren=tagCompound.getByte("numberChildren");
		this.rotationRoll=tagCompound.getFloat("rotationRoll");
	}
    
	@Override
	public void writeToNBT(NBTTagCompound tagCompound){
		super.writeToNBT(tagCompound);
		tagCompound.setByte("numberChildren", this.numberChildren);
		tagCompound.setFloat("rotationRoll", this.rotationRoll);
	}
	
	/**This class contains data for parts that can be attached to or are attached to this parent.
	 * A set of these classes must be added upon init of the parent.
	 * The parent then looks to see if any linked parts match this list, and note it as so.
	 * 
	 *@author don_bruce
	 */
	protected class PartData{
		public final boolean rotatesWithYaw;
		public final boolean isController;
		public final float offsetX;
		public final float offsetY;
		public final float offsetZ;
		public final float alternateOffsetX;
		public final float alternateOffsetY;
		public final float alternateOffsetZ;
		public final Class<? extends EntityChild>[] acceptableClasses;
		
		public PartData(float offsetX, float offsetY, float offsetZ, boolean rotatesWithYaw, boolean isController, float alternateOffsetX, float alternateOffsetY, float alternateOffsetZ, Class<? extends EntityChild>... acceptableClasses){
			this.rotatesWithYaw = rotatesWithYaw;
			this.isController = isController;
			this.offsetX = offsetX;
			this.offsetY = offsetY;
			this.offsetZ = offsetZ;
			this.alternateOffsetX = alternateOffsetX;
			this.alternateOffsetY = alternateOffsetY;
			this.alternateOffsetZ = alternateOffsetZ;
			this.acceptableClasses = acceptableClasses;
		}
		
		public PartData(float offsetX, float offsetY, float offsetZ, boolean rotatesWithYaw, boolean isController, Class<? extends EntityChild>... acceptableClasses){
			this(offsetX, offsetY, offsetZ, rotatesWithYaw, isController, offsetX, offsetY, offsetZ, acceptableClasses);
		}
		
		public PartData(float offsetX, float offsetY, float offsetZ, Class<? extends EntityChild>... acceptableClasses){
			this(offsetX, offsetY, offsetZ, false, false, acceptableClasses);
		}
	}
}