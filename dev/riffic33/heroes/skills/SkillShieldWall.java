package dev.riffic33.heroes.skills;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;


public class SkillShieldWall extends ActiveSkill {
	
    public SkillShieldWall(Heroes plugin) {
        super(plugin, "Shieldwall");
        setUsage("/skill shieldwall");
        setArgumentRange(0, 0);
        setIdentifiers("skill shieldwall");
        setTypes(SkillType.ILLUSION, SkillType.KNOWLEDGE, SkillType.PHYSICAL); 
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
    	ConfigurationSection node = super.getDefaultConfig();
        node.set("Height", 3);
        node.set("Width", 2);
        node.set(Setting.MAX_DISTANCE.node(), 5);
        node.set(Setting.DURATION.node(), 5000);
        node.set("BlockType", "STONE");
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int height 		= (int) SkillConfigManager.getUseSetting(hero, this, "Height", 3, false);
    	int width 		= (int) SkillConfigManager.getUseSetting(hero, this, "width", 2, false);
    	int maxDist 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 5, false);
    	String type 	= SkillConfigManager.getUseSetting(hero, this, "BlockType", "STONE");
    	
        return String.format("Makes a wall of %s which is %s wide by %s high up to %s blocks away (Targetted)", type, width, height, maxDist);
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	int height 		= (int) SkillConfigManager.getUseSetting(hero, this, "Height", 3, false);
    	int width 		= (int) SkillConfigManager.getUseSetting(hero, this, "width", 2, false);
    	int maxDist 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.MAX_DISTANCE, 5, false);
    	long duration 	= (long) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	Material setter = Material.valueOf(SkillConfigManager.getUseSetting(hero, this, "BlockType", "STONE"));
    
    	Block tBlock = player.getTargetBlock(null, maxDist);
    	if(tBlock.getType() == Material.AIR) 
    		return SkillResult.INVALID_TARGET;
    	
    	shieldwallEffect swe = new shieldwallEffect(this, duration, tBlock, width, height, setter);
    	hero.addEffect(swe);
    	
    	return SkillResult.NORMAL;
    }
    
    public class shieldwallEffect extends ExpirableEffect{
    	private final Block tBlock;
    	private final int width;
    	private final int height;
    	private final String applyText = "$1 made a wall";
    	private final String expireText = "$1's wall has been removed";
    	private HashSet<Block> wBlocks;
    	private Material setter;
    	
		public shieldwallEffect(Skill skill, long duration, Block tBlock, int width, int height, Material setter){
			super(skill, "sheildWallEffect", duration);
			this.tBlock	= tBlock;
			this.width 	= width;
			this.height	= height;
			this.setter	= setter;
			this.wBlocks= new HashSet<Block>(width*height*2);
		}
		
		@Override
		public void applyToHero( Hero hero ){
			super.applyToHero( hero );
			
			Player player = hero.getPlayer();
			if(is_X_Direction(player)){
	    		for(int yDir=0; yDir<height; yDir++){
	    			for(int xDir=-width; xDir<width+1; xDir++){
	    				Block chBlock = tBlock.getRelative(xDir, yDir, 0);
	    				if(chBlock.getType() == Material.AIR || chBlock.getType() == Material.SNOW){
	    					chBlock.setType(setter);
	    					wBlocks.add(chBlock);
	    				}
	    			}
	    		}
	    	}else{
	    		for(int yDir=0; yDir<height; yDir++){
	    			for(int zDir=-width; zDir<width+1; zDir++){
	    				Block chBlock = tBlock.getRelative(0, yDir,  zDir);
	    				if(chBlock.getType() == Material.AIR || chBlock.getType() == Material.SNOW){
	    					chBlock.setType(setter);
	    					wBlocks.add(chBlock);
	    				}
	    			}
	    		}
	    	}
			broadcast(player.getLocation(), applyText, player.getDisplayName());
		}
		
		@Override
	    public void removeFromHero( Hero hero ) {
	        super.removeFromHero( hero );
	        Player player = hero.getPlayer();
            Iterator<Block> bIter = wBlocks.iterator();
            while(bIter.hasNext()){
            	Block bChange = bIter.next();
            	if(bChange.getType() == setter){
            		bChange.setType(Material.AIR);
            	}
            }
            broadcast(player.getLocation(), expireText, player.getDisplayName());
	    }	
    }
    
    private boolean is_X_Direction(Player player){
		Vector u = player.getLocation().getDirection();
			   u = new Vector(u.getX(), 0, u.getZ()).normalize();
		Vector v = new Vector(0, 0, -1);
		double magU 	= Math.sqrt(Math.pow(u.getX(), 2) + Math.pow(u.getZ(), 2));
		double magV 	= Math.sqrt(Math.pow(v.getX(), 2) + Math.pow(v.getZ(), 2));
		double angle 	= Math.acos( (u.dot(v)) / (magU * magV));
			   angle 	= angle*180D/Math.PI;
			   angle 	= Math.abs(angle-180);
	
	    return (angle <= 45D || angle > 135D) ? true : false;
    }
    
   
    
}