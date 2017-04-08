package minecrafttransportsimulator.items;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import minecrafttransportsimulator.MTS;
import minecrafttransportsimulator.entities.core.EntityCore;
import minecrafttransportsimulator.entities.core.EntityPlane;
import minecrafttransportsimulator.minecrafthelpers.AABBHelper;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

public class ItemPlane extends Item{
	private Class<? extends EntityPlane> plane;
	private int numberTypes;
	private IIcon[] icons;
	
	public ItemPlane(Class<? extends EntityPlane> plane, int numberSubtypes){
		super();
		this.setUnlocalizedName(plane.getSimpleName().substring(6).toLowerCase());
		this.plane = plane;
		this.numberTypes = numberSubtypes;
		this.icons = new IIcon[numberSubtypes];
	}
	
	@Override
	public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int p_77648_7_, float hitX, float hitY, float hitZ){
		if(!world.isRemote){
			EntityPlane newPlane;
			try{
				newPlane = this.plane.getConstructor(World.class, float.class, float.class, float.class, float.class, int.class).newInstance(world, x, y + 1, z, player.rotationYaw, stack.getItemDamage());
				float minHeight = 0;
				for(float[] coreCoords : newPlane.getCoreLocations()){
					minHeight = -coreCoords[1] > minHeight ? -coreCoords[1] : minHeight;
				}
				newPlane.posY += minHeight;
				newPlane.ownerName = player.getDisplayName();
				if(canSpawnPlane(world, newPlane)){
					if(!player.capabilities.isCreativeMode){
						--stack.stackSize;
					}
					return true;//INS190
					/*INS190
					return EnumActionResult.PASS;
					INS190*/
				}
			}catch(Exception e){
				System.err.println("ERROR SPAWING PLANE!");
				e.printStackTrace();
			}
		}
		return false;//INS190
		/*INS190
		return EnumActionResult.FAIL;
		INS190*/
	}

	public boolean canSpawnPlane(World world, EntityPlane plane){
		float[][] coreLocations = plane.getCoreLocations();
		EntityCore[] spawnedCores = new EntityCore[coreLocations.length];
		for(int i=0; i<coreLocations.length; ++i){
			EntityCore core = new EntityCore(world, plane, plane.UUID, coreLocations[i][0], coreLocations[i][1], coreLocations[i][2], coreLocations[i][3], coreLocations[i][4]);
			world.spawnEntityInWorld(core);
			spawnedCores[i] = core;
			if(!AABBHelper.getCollidingBlockBoxes(world, core.getBoundingBox(), core.collidesWithLiquids()).isEmpty()){
				for(int j=0; j<=i; ++j){
					spawnedCores[j].setDead();
				}
				plane.setDead();
				return false;
			}
		}
		world.spawnEntityInWorld(plane);
		return true;
	}
	
	@Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tab, List itemList){
		for(int i=0; i<numberTypes; ++i){
			itemList.add(new ItemStack(item, 1, i));
		}
    }
	//DEL180START
    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register){
    	for(int i=0; i<numberTypes; ++i){
    		icons[i] = register.registerIcon(MTS.MODID + ":" + plane.getSimpleName().substring(6).toLowerCase() + i);
    	}
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage){
        return this.icons[damage >= numberTypes ? 0 : damage];
    }
    //DEL180END
}