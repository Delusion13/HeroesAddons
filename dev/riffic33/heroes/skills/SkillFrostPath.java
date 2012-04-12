package dev.riffic33.heroes.skills;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;

public class SkillFrostPath extends ActiveSkill {
	private BlockFace[] bCheck = {BlockFace.SELF, BlockFace.NORTH_WEST, BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST};
	private HashSet<Block> iceBlocks = new HashSet<Block>(30);
	private boolean icePersists = true;
	
    public SkillFrostPath(Heroes plugin) {
        super(plugin, "Frostpath");
        setUsage("/skill frostpath");
        setArgumentRange(0, 0);
        setIdentifiers("skill frostpath");
        setTypes(SkillType.BUFF);
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillDmgListener(this), plugin);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 60000);
        node.set("AttackCancels", true);
        node.set("IcePersists", true);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	long duration = (Integer) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.DURATION.node(), 60000);
    	boolean cancels = (boolean) SkillConfigManager.getSetting(hero.getHeroClass(), this, "AttackCancels", true);
    	
    	String base = String.format("Turn water to ice as you walk for %s seconds.", duration/1000D);
    	
        return cancels ? base : base.concat("Damage removes this buff.");
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	long 	duration = (Integer) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.DURATION.node(), 60000);
    			icePersists = SkillConfigManager.getSetting(hero.getHeroClass(), this, "IcePersists", true);
    	FrostPath fpEff = new FrostPath(this, duration);
    	hero.addEffect(fpEff);
    	if(player.getLocation().getBlock().isLiquid() && player.getLocation().getBlock().getRelative(BlockFace.UP).getTypeId() == 0){
    		player.setVelocity(player.getVelocity().add(new Vector(0, 1, 0)));
    	}
    	
    	return SkillResult.NORMAL;
    }
    

    private void clearOldBlocks(Player player, boolean effTimeUp){
		Iterator<Block> iceIter = null;
		final HashSet<Block> nearPlayer = new HashSet<Block>(9);
		Block startBlock = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
		for(BlockFace bFace : bCheck){
			nearPlayer.add(startBlock.getRelative(bFace));
		}
		Block b = null;
        try {
            iceIter = iceBlocks.iterator();
        } catch (IllegalStateException e) {
            Messaging.send(player, "There was an error with the frost path!");
        }
        while (iceIter.hasNext()){
            b = iceIter.next();
            if(!nearPlayer.contains(b) || effTimeUp){
            	b.setType(Material.WATER);
            }
        }
	}
    
    public class FrostPath extends PeriodicExpirableEffect{
    	
		public FrostPath(Skill skill,long duration) {
			super(skill, "FrostPath", 50, duration);
			this.types.add(EffectType.BENEFICIAL);
		}
		
		@Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Messaging.send(hero.getPlayer(), "Now Frost pathing");
        }
		
		@Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            if(!icePersists){
            	clearOldBlocks(hero.getPlayer(), true);
            }
            Messaging.send(hero.getPlayer(), "No longer frost pathing");
        }
		
		@Override
		public void tickHero(Hero hero){
			Player player = hero.getPlayer();
			Block bLoc = player.getLocation().getBlock().getRelative(BlockFace.DOWN);
			for(BlockFace bFace : bCheck){
				Block chgBlock = bLoc.getRelative(bFace);
				if(chgBlock.getType() == Material.WATER || chgBlock.getType() == Material.STATIONARY_WATER){
    				chgBlock.setType(Material.ICE);
    				if(!icePersists){
    					iceBlocks.add(chgBlock);
    				}
    			}		
			}
			if(!icePersists){
				clearOldBlocks(hero.getPlayer(), false);
			}
		}

		@Override
		public void tickMonster(Monster arg0) {
		}
    }
    
    public class SkillDmgListener implements Listener{
    	
    	private Skill skill;
    	public SkillDmgListener(Skill skill){
    		this.skill = skill;
    	}
    	
    	@EventHandler
    	public void onEntityDamage(EntityDamageEvent event){
    		
    		if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }
    		Entity player = event.getEntity();
    		if(player instanceof Player && plugin.getCharacterManager().getHero( (Player) player ).hasEffect("FrostPath")){
    			Hero hero = plugin.getCharacterManager().getHero( (Player) player );
    			if(SkillConfigManager.getUseSetting(hero, skill, "AttackCancels", true)){
	    			FrostPath playFp = (FrostPath) hero.getEffect("FrostPath");
	    			hero.removeEffect(playFp);
    			}
    		}   
    	}
    }
    
   
    
}
