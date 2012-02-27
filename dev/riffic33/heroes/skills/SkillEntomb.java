package dev.riffic33.heroes.skills;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.Effect;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicDamageEffect;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;


public class SkillEntomb extends TargettedSkill {
	
    public SkillEntomb(Heroes plugin) {
        super(plugin, "Entomb");
        setUsage("/skill entomb");
        setArgumentRange(0, 0);
        setIdentifiers("skill entomb");
        setTypes(SkillType.SILENCABLE, SkillType.LIGHT, SkillType.DEBUFF);  
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseTickDamage", 0);
        node.set("LevelMultiplier", 0.5);
        node.set(Setting.DURATION.node(), 12000);
        node.set(Setting.PERIOD.node(), 4000);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	long period 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
        
        String base = String.format("Encase your target in glass for $1 seconds. ", duration/1000);
        
        return tickDmg > 0 ? base.concat("Deals " + tickDmg + " every " + period + " seconds.") : base; 
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
    	
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            Messaging.send(player, "Can't entomb the target");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	long period 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	EntombEffect ete = new EntombEffect(this, duration);
    	if( tickDmg > 0){
    		EntombDmgEffect etde = new EntombDmgEffect(this, period, duration, tickDmg, player);
	    	if (target instanceof Player) {
	            plugin.getHeroManager().getHero((Player) target).addEffect(ete);
	            plugin.getHeroManager().getHero((Player) target).addEffect(etde);
	            return SkillResult.NORMAL;
	        } else if (target instanceof LivingEntity) {
	        	//POSSIBLE FUTURE IMPLEMENTATION
	            return SkillResult.INVALID_TARGET;
	        } else 
	            return SkillResult.INVALID_TARGET;
    	}else{
    		if (target instanceof Player) {
	            plugin.getHeroManager().getHero((Player) target).addEffect(ete);
	            return SkillResult.NORMAL;
	        } else if (target instanceof LivingEntity) {
	        	//POSSIBLE FUTURE IMPLEMENTATION
	            return SkillResult.INVALID_TARGET;
	        } else 
	            return SkillResult.INVALID_TARGET;
    	}
    }
    
    
    
    public class EntombEffect extends PeriodicExpirableEffect{
    
    	private HashSet<Block> blocks;
    	private final String applyText = "$1 has been entombed";
    	private final String expireText = "Entomb removed from $1";
    	private Location loc;
    	
	    public EntombEffect(Skill skill,  long duration) {
				super(skill, "EntombEffect", 100, duration);
				this.types.add(EffectType.DISABLE);
				this.types.add(EffectType.STUN);
				this.types.add(EffectType.LIGHT);
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
            this.blocks = placeGlassBlock(player);
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            Iterator<Block> glsIter = blocks.iterator();
            while(glsIter.hasNext()){
            	Block bChange = glsIter.next();
            	if(bChange.getType() == Material.GLASS){
            		bChange.setType(Material.AIR);
            	}
            }
            if( hero.hasEffect("EntombDmgEffect") ){
            	Effect eff = hero.getEffect("EntombDmgEffect");
            	hero.removeEffect(eff);
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
    
    public class EntombDmgEffect extends PeriodicDamageEffect{
    	
    	public EntombDmgEffect(Skill skill, long period, long duration, int tickDmg, Player applier) {
			super(skill, "EntombDmg", period, duration, tickDmg, applier);
			this.types.add(EffectType.LIGHT);
    	}  

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            if( hero.hasEffect("EntombEffect") ){
            	Effect eff = hero.getEffect("EntombEffect");
            	hero.removeEffect(eff);
            }
        }	
    	
    }
    
    private HashSet<Block> placeGlassBlock(LivingEntity target){
    	HashSet<Block> blocks = new HashSet<Block>(20);
    	Block glsLoc = target.getLocation().getBlock();
    	for(int y=0; y<2; y++){
    		for(int x=-1; x<2; x++){
    			for(int z=-1; z<2; z++){
        			if(glsLoc.getRelative(x, y, z).isEmpty()){
    		    			Block iBlock = glsLoc.getRelative(x, y, z);
    						iBlock.setType(Material.GLASS);
    						blocks.add(iBlock);
    				}
        		}
    		}	
    	}
    	return blocks;
    }
    
    public class SkillListener implements Listener{
    	
    	@EventHandler
    	public void onBlockBreak(BlockBreakEvent event){
    		Player player 	= event.getPlayer();
    		Hero hero 		= plugin.getHeroManager().getHero(player);
    		if(hero.hasEffect("Entomb")){
    			event.setCancelled(true);
    		}
    	}
  
    }
    
   

}
