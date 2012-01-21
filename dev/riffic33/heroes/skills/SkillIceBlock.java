package dev.riffic33.heroes.skills;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockListener;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;


public class SkillIceBlock extends TargettedSkill {
	
    public SkillIceBlock(Heroes plugin) {
        super(plugin, "IceBlock");
        setDescription("Encapsulate your target in ice for $1 seconds.");
        setUsage("/skill iceblock");
        setArgumentRange(0, 0);
        setIdentifiers("skill iceblock");
        setTypes(SkillType.SILENCABLE, SkillType.ICE, SkillType.DEBUFF);  
        
        registerEvent(Type.BLOCK_BREAK, new IceBreakerListener(), Priority.Highest);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);	
        return  node;
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
   
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            Messaging.send(player, "Can't freeze the target");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	int duration = (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	IceBlockEffect ibe = new IceBlockEffect(this,  duration);
    	if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(ibe);
            return SkillResult.NORMAL;
        } else if (target instanceof LivingEntity) {
        	//TODO
            //LivingEntity creature = (LivingEntity) target;
            //UPDATING TO addCreatureEffect soon
            //plugin.getEffectManager().addEntityEffect(creature, ibe);
            return SkillResult.INVALID_TARGET;
        } else 
            return SkillResult.INVALID_TARGET;
    }
    
    
    
    public class IceBlockEffect extends PeriodicExpirableEffect{
    
    	private HashSet<Block> blocks;
    	private final String applyText = "$1 has been put in an Ice Block";
    	private final String expireText = "Ice block Removed from $1";
    	private Location loc;
    	
	    public IceBlockEffect(Skill skill, long duration) {
				super(skill, "IceBlockFreeze", 100, duration);		
		}  

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
            Location pLoc = player.getLocation();
            Location pBlockLoc= pLoc.getBlock().getLocation();
            Location tpLoc = new Location(pLoc.getWorld(), pBlockLoc.getX()+0.5D, pBlockLoc.getY(), pBlockLoc.getZ()+0.5D);
            		tpLoc.setYaw(pLoc.getYaw());
            		tpLoc.setPitch(pLoc.getPitch());
            player.teleport(tpLoc);
            this.loc = tpLoc;
            this.blocks = placeIceBlock(player);
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            Iterator<Block> iceIter = blocks.iterator();
            while(iceIter.hasNext()){
            	Block bChange = iceIter.next();
            	if(bChange.getType() == Material.ICE){
            		bChange.setType(Material.AIR);
            	}
            }
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
        
        @Override
        public void tick(Hero hero) {
            super.tick(hero);
            Player p = hero.getPlayer();
            Location location = p.getLocation();
            if (location == null)
                return;
            if (location.getX() != loc.getX() || location.getY() != loc.getY() || location.getZ() != loc.getZ()) {
                loc.setYaw(location.getYaw());
                loc.setPitch(location.getPitch());
                p.teleport(loc);
            }
        }
	    
    }
    
    private HashSet<Block> placeIceBlock(LivingEntity target){
    	HashSet<Block> blocks = new HashSet<Block>(20);
    	Block iceLoc = target.getLocation().getBlock();
    	for(int y=0; y<2; y++){
    		for(int x=-1; x<2; x++){
    			for(int z=-1; z<2; z++){
        			if(iceLoc.getRelative(x, y, z).isEmpty()){
    		    			Block iBlock = iceLoc.getRelative(x, y, z);
    						iBlock.setType(Material.ICE);
    						blocks.add(iBlock);
    				}
        		}
    		}	
    	}
    	return blocks;
    }
    
    public class IceBreakerListener extends BlockListener{
    	
    	public void onBlockBreak(BlockBreakEvent event){
    		Player player 	= event.getPlayer();
    		Hero hero 		= plugin.getHeroManager().getHero(player);
    		if(hero.hasEffect("IceBlockFreeze")){
    			event.setCancelled(true);
    		}
    	}
  
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int duration = (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
        return getDescription().replace("$1", duration/1000 + "");
    }

}
