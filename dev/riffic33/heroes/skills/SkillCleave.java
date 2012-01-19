package dev.riffic33.heroes.skills;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ComplexLivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityListener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.party.HeroParty;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;


public class SkillCleave extends ActiveSkill {
	
    public SkillCleave(Heroes plugin) {
        super(plugin, "Cleave");
        setDescription("Attack up to $1 enemies within $2 radius in front of you for $3 damage.");
        setUsage("/skill cleave");
        setArgumentRange(0, 0);
        setIdentifiers("skill cleave");
        setTypes(SkillType.DAMAGING, SkillType.PHYSICAL);  
        
        registerEvent(Type.ENTITY_DAMAGE, new SkillCleaveEvent(this), Priority.Normal);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("MaxTargets", 3);
        node.set(Setting.DURATION.node(), 5000);
        node.set(Setting.DAMAGE.node(), 5);
        node.set(Setting.RADIUS.node(), 5);
        return  node;
    }
    
    @Override
	public SkillResult use(Hero hero, String[] args) {
    	int duration = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.DURATION.node(), 5000);
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
		
		@Override
        public void apply(Hero hero) {
            super.apply(hero);
            Messaging.send(hero.getPlayer(), "Cleaving available on your next attack for $1 seconds", duration/1000);
            Messaging.send(hero.getPlayer(), Util.swords.toString());
        }
		
        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Messaging.send(hero.getPlayer(), "Can no longer cleave");
        }
	
    }
    
    public class SkillCleaveEvent extends EntityListener{
    		
    	private final Skill skill;
        
        public SkillCleaveEvent(Skill skill) {
            this.skill = skill;
        }
        
        @Override
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
            Hero hero = plugin.getHeroManager().getHero(player);
            if (hero.hasEffect("CleaveBuff")) {
            	
                ItemStack item = player.getItemInHand();
                if (!Util.swords.contains(item.getType().name())) {
                    return;
                }   
                CleaveBuff cb = (CleaveBuff) hero.getEffect("CleaveBuff");
                
                hero.removeEffect(cb);
                event.setDamage((int) (SkillConfigManager.getSetting(hero.getHeroClass(), skill, Setting.DAMAGE.node(), 5)));
                int hitAmount = damageAround(player, initTarg) + 1;
                
                broadcast(player.getLocation(),"$1 hit $2 entities with cleave", player.getDisplayName(), hitAmount);
            }
        }	
    }
    
    private int damageAround(Player player, Entity exception){
    	Hero hero = plugin.getHeroManager().getHero(player);
    	int MaxTargets = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, "MaxTargets", 3) - 1;
    	int radius = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.RADIUS.node(), 5);
    	int damage = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.DAMAGE.node(), 5);
    	
    	int Hits = 0; 
    	List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
    	HeroParty hParty = hero.getParty();
    	if(hParty != null){
	    	for(Entity entity : nearby){
	    		if(Hits >= MaxTargets) break;
	    		if((entity instanceof Player && hParty.isPartyMember((Player) entity))){
	    			continue;
	    		}
	    		if(!(entity.equals(exception)) && (entity instanceof Monster || entity instanceof ComplexLivingEntity || entity instanceof Player) && isInFront(player, entity)){
	    			damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);
	    			Hits += 1;
	    		}
	    	}
    	}else{
    		for(Entity entity : nearby){
    			if(Hits >= MaxTargets || entity.equals(exception)) break;
    			if(!(entity.equals(exception)) && (entity instanceof Monster || entity instanceof ComplexLivingEntity || entity instanceof Player) && isInFront(player, entity)){
	    			damageEntity((LivingEntity) entity, player, damage, DamageCause.ENTITY_ATTACK);
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
    
    
    @Override
    public String getDescription(Hero hero) {
    	int MaxTargets = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, "MaxTargets", 10);
    	int radius = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.RADIUS.node(), 3);
    	int damage = (int) SkillConfigManager.getSetting(hero.getHeroClass(), this, Setting.DAMAGE.node(), 5);
        return getDescription().replace("$1", MaxTargets + "").replace("$2", radius + "").replace("$3", damage + "");
    }

}