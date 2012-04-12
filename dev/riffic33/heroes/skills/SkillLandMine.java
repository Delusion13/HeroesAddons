package dev.riffic33.heroes.skills;

import java.util.HashSet;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillLandMine extends ActiveSkill {
	//List of relative blocks to check when placing stone_plates to trip mine
	private final BlockFace[] bChecks = {
			BlockFace.NORTH, BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH
	};
	
    public SkillLandMine(Heroes plugin) {
        super(plugin, "Landmine");
        setUsage("/skill landmine");
        setArgumentRange(0, 0);
        setIdentifiers("skill landmine");
        setTypes(SkillType.DAMAGING);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();	
        node.set(Setting.COOLDOWN.node(), 30000);
        node.set("ReadiedTime", 5000);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
        int time = SkillConfigManager.getUseSetting(hero, this, "ReadiedTime", 1, false);
        
        return String.format("Place a trip mine, armed after %s seconds.", time/1000);
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	Block setter = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
    	boolean readied = isValidMineLoc(setter);

    	if(readied){
    		long delayTimer = SkillConfigManager.getSetting(hero.getHeroClass(), this, "ReadiedTime", 5000);
    		int tripCount = setTrips(setter, true);
    		ArmMineEffect armEff = new ArmMineEffect(this, delayTimer, setter, tripCount);
    		hero.addEffect(armEff);
    		return SkillResult.NORMAL;
    	}else{
    		Messaging.send(player, "Mine can't be placed here");
    		return SkillResult.CANCELLED;
    	}
    }
    
    public class ArmMineEffect extends ExpirableEffect{
    	
    	private Block setter = null;
    	private int tripCount = 0;
    	
    	
		public ArmMineEffect(Skill skill, long duration, Block setter, int tripCount) {
			super(skill, "ArmMine", duration);
			this.setter = setter;
			this.tripCount = tripCount;
			this.types.add(EffectType.PHYSICAL);
		}
		
		@Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Messaging.send(hero.getPlayer(), "Setting mine with $1 trip(s) in $2 seconds", tripCount, this.getDuration()/1000);
        }
		
		@Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            setTrips(setter);
            Messaging.send(hero.getPlayer(), "Mine Armed!");
        }	
    }

	private void setTrips(Block block){
    	for(BlockFace faceName : this.bChecks){
    		Block tripBlock = block.getRelative(faceName).getRelative(BlockFace.UP);
			if(tripBlock.getTypeId() == 0){
				tripBlock.setType(Material.STONE_PLATE);
			}
		}
    	block.setType(Material.TNT);
    }
    
    private int setTrips(Block block, boolean bool){
    	int tripCount = 0;
    	for(BlockFace faceName : this.bChecks){
    		Block tripBlock = block.getRelative(faceName).getRelative(BlockFace.UP);
			if(tripBlock.getTypeId() == 0){
				tripCount++;
			}
		}
    	return tripCount;
    }
    
    private boolean isValidMineLoc(Block block){
    	final HashSet<Material> naturalMats = new HashSet<Material>(4);
						    	naturalMats.add(Material.COBBLESTONE);
						    	naturalMats.add(Material.DIRT);
						    	naturalMats.add(Material.GRASS);
						    	naturalMats.add(Material.GRAVEL);
						    	naturalMats.add(Material.LOG);
						    	naturalMats.add(Material.NETHERRACK);
						    	naturalMats.add(Material.SAND);
						    	naturalMats.add(Material.SANDSTONE);
						    	naturalMats.add(Material.SNOW_BLOCK);
						    	naturalMats.add(Material.STONE);
    	
    	if(naturalMats.contains(block.getType())){
    		int tripCount = 0;
    		for(BlockFace faceName : this.bChecks){
    			if(!naturalMats.contains(block.getRelative(faceName).getType()) ){
        			return false;
        		}
    			if(block.getRelative(faceName).getRelative(BlockFace.UP).getTypeId() == 0){
    				tripCount++;
    			}
    		}
    		return tripCount > 0 ? true : false;
    	}else{
    		return false;
    	}
    }
    
   
    
    
}
