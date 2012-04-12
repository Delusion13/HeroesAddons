package dev.riffic33.heroes.skills;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.effects.ExpirableEffect;
import com.herocraftonline.heroes.characters.party.HeroParty;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;
import com.herocraftonline.heroes.util.Util;

public class SkillCleave extends ActiveSkill {
	
    public SkillCleave(Heroes plugin) {
        super(plugin, "Cleave");
        setUsage("/skill cleave");
        setArgumentRange(0, 0);
        setIdentifiers("skill cleave");
        setTypes(SkillType.DAMAGING, SkillType.PHYSICAL);  
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("MaxTargets", 3);
        node.set(Setting.DURATION.node(), 5000);
        node.set("BaseDamage", 3);
        node.set("LevelMultiplier", 0.5);
        node.set(Setting.RADIUS.node(), 5);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int MaxTargets = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, "MaxTargets", 10);
    	int radius = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.RADIUS.node(), 3);
    	int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, this, "BaseDamage", 3, false);
    	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int newDmg 		= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
        return String.format("Attack up to %s enemies within a %s block radius in front of you for %s damage.", MaxTargets, radius, newDmg); 
    }
    
    @Override
	public SkillResult use(Hero hero, String[] args) {
    	int duration = (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	
    	CleaveBuff cb = new CleaveBuff(this, duration);
    	hero.addEffect(cb);
    	
    	return SkillResult.NORMAL;
	}
 
    public class CleaveBuff extends  ExpirableEffect{
    	
    	private long duration = 0;
    		
		public CleaveBuff(Skill skill,  long duration) {
			super(skill, "CleaveBuff", duration);
			this.duration = duration;
		}
		
		public void apply(Hero hero) {
            super.apply(hero);
            Messaging.send(hero.getPlayer(), "Cleaving available on your next attack for $1 seconds", duration/1000);
        }
		
        public void remove(Hero hero) {
            super.remove(hero);
            Messaging.send(hero.getPlayer(), "Can no longer cleave");
        }
	
    }
    
    public class SkillListener implements Listener{
    		
    	private final Skill skill;
        
        public SkillListener(Skill skill) {
            this.skill = skill;
        }
        
        @EventHandler(priority = EventPriority.HIGHEST)
        public void onEntityDamage(EntityDamageEvent event) {
        	if (event.isCancelled() || !(event instanceof EntityDamageByEntityEvent)) {
                return;
            }
            
        	Entity initTarg = event.getEntity();
            if (!(initTarg instanceof LivingEntity) && !(initTarg instanceof Player)) {
                return;
            }

            EntityDamageByEntityEvent subEvent = (EntityDamageByEntityEvent) event;
            if (!(subEvent.getDamager() instanceof Player)) {
                return;
            }
            
            Player player = (Player) subEvent.getDamager();
            Hero hero = plugin.getCharacterManager().getHero(player);
            if (hero.hasEffect("CleaveBuff")) {
            	
                ItemStack item = player.getItemInHand();
                if (!Util.swords.contains(item.getType().name()) && !Util.axes.contains(item.getType().name())) {
                    return;
                }   
                CleaveBuff cb = (CleaveBuff) hero.getEffect("CleaveBuff");
                
                hero.removeEffect(cb);
                int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, skill, "BaseDamage", 3, false);
            	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, skill, "LevelMultiplier", 0.5, false);
            	int newDmg 			= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
                event.setDamage( newDmg);
                int hitAmount = damageAround(player, initTarg, skill, newDmg) + 1;
                
                broadcast(player.getLocation(),"$1 hit $2 enemies with cleave", player.getDisplayName(), hitAmount);
            }
        }	
    }
    
    private int damageAround(Player player, Entity exception, Skill skill, int newDmg){
    	
    	Hero hero = plugin.getCharacterManager().getHero(player);
    	int MaxTargets = (int) SkillConfigManager.getUseSetting(hero, this, "MaxTargets", 3, false) - 1;
    	int radius = (int) SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 5, false);

    	int Hits = 0; 
    	List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
    	HeroParty hParty = hero.getParty();
    	if(hParty != null){
	    	for(Entity entity : nearby){
	    		if(Hits >= MaxTargets) break;
	    		if(entity.equals(exception)){
    				continue;
    			}
	    		if((entity instanceof Player && hParty.isPartyMember((Player) entity))){
	    			continue;
	    		}
	    		if( (entity instanceof Monster || entity instanceof ComplexLivingEntity || entity instanceof Player) && isInFront(player, entity)){
	    			damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	    			Hits += 1;
	    		}
	    	}
    	}else{
    		player.sendMessage("No Party");
    		for(Entity entity : nearby){
    			if(Hits >= MaxTargets) break;
    			if(entity.equals(exception)){
    				continue;
    			}
    			if( (entity instanceof Monster || entity instanceof ComplexLivingEntity || entity instanceof Player) && isInFront(player, entity)){
	    			damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
		    		Hits += 1;
    			}
	    	}
    	}
    	return Hits;

    }
    
    private boolean isInFront(Player player, Entity target){
	    	if (!target.getWorld().equals(player.getWorld())) {
	    		return false;
	        }
    		Location pLoc = player.getLocation();
    		Location tLoc = target.getLocation();
    		Vector u = player.getLocation().getDirection().normalize();
    		Vector v = new Vector(tLoc.getX()-pLoc.getX(), 0, tLoc.getZ()-pLoc.getZ());
    		double magU 	= Math.sqrt(Math.pow(u.getX(), 2) + Math.pow(u.getZ(), 2));
    		double magV 	= Math.sqrt(Math.pow(v.getX(), 2) + Math.pow(v.getZ(), 2));
    		double angle 	= Math.acos( (u.dot(v)) / (magU * magV));
    			   angle 	= angle*180D/Math.PI;
    	    return angle < 90D ? true : false;
    }
    
    
    

}