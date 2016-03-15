package minecraftflightsimulator.modelrenders;

import minecraftflightsimulator.entities.EntityPropeller;
import minecraftflightsimulator.models.ModelPropeller;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderPropeller extends Render{
	private static final ModelPropeller model = new ModelPropeller();
	
    public RenderPropeller(){
        super();
    }

	@Override
	public void doRender(Entity entity, double x, double y, double z, float yaw,float pitch){		
		EntityPropeller propeller=(EntityPropeller) entity;
		if(propeller.parent != null){
			GL11.glPushMatrix();
			GL11.glTranslated(x, y, z);
			GL11.glRotatef(-propeller.parent.rotationYaw, 0, 1, 0);
			GL11.glRotatef(propeller.parent.rotationPitch, 1, 0, 0);
			GL11.glRotatef(propeller.parent.rotationRoll, 0, 0, 1);
	        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
			if(propeller.propertyCode%10==1){
				Minecraft.getMinecraft().renderEngine.bindTexture(model.tierTwoTexture);
			}else if(propeller.propertyCode%10==2){
				Minecraft.getMinecraft().renderEngine.bindTexture(model.tierThreeTexture);
			}else{
				Minecraft.getMinecraft().renderEngine.bindTexture(model.tierOneTexture);
			}
			model.renderPropellor(propeller.propertyCode%100/10, 70+5*(propeller.propertyCode/1000), propeller.angularPosition);
			GL11.glPopMatrix();
		}
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity propellor){
		if(((EntityPropeller) propellor).propertyCode==1){
			return model.tierTwoTexture;
		}else if(((EntityPropeller) propellor).propertyCode==2){
			return model.tierThreeTexture;
		}else{
			return model.tierOneTexture;
		}
	}
}