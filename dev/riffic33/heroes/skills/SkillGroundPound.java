package dev.riffic33.heroes.skills;

import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.util.Vector;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.party.HeroParty;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;


public class SkillGroundPound extends ActiveSkill {
	
    public SkillGroundPound(Heroes plugin) {
        super(plugin, "Groundpound");
        setDescription();
        setUsage("/skill groundpound");
        setArgumentRange(0, 0);
        setIdentifiers("skill groundpound");
        setTypes(SkillType.PHYSICAL, SkillType.DAMAGING, SkillType.HARMFUL, SkillType.FORCE);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseDamage", 3);
        node.set("LevelMultiplier", 0.5);
        node.set("Targets", 4);
        node.set(Setting.RADIUS.node(), 5);
        node.set("JumpMultiplier", 1.2);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, this, "BaseDamage", 3, false);
    	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int targets 		= (int) SkillConfigManager.getUseSetting(hero, this, "Targets", 10, false);
    	int newDmg 		= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	return String.format("Hit the ground dealing %s damage and sending %s nearby enemies into the air", newDmg, targets);
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, this, "BaseDamage", 3, false);
    	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int targets 		= (int) SkillConfigManager.getUseSetting(hero, this, "Targets", 10, false);
    	int radius 			= (int) SkillConfigManager.getUseSetting(hero, this, Setting.RADIUS, 5, false);
    	float jMod 			= (float) SkillConfigManager.getUseSetting(hero, this, "JumpMultiplier", 0.6, false);
    	int newDmg 		= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	List<Entity> nearby = player.getNearbyEntities(radius, radius, radius);
    	HeroParty hParty = hero.getParty();
    	int hitsLeft = targets;
    	Vector flyer = new Vector(0, jMod, 0);
    	if(hParty != null){
	    	for(Entity entity : nearby){
	    		if(hitsLeft <= 0) break;
	    		if((entity instanceof Player && hParty.isPartyMember((Player) entity))){
	    			continue;
	    		}
	    		if( entity instanceof Monster){
	                addSpellTarget(entity, hero);
	                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	                entity.setVelocity(flyer);
	                hitsLeft -= 1;
	    		}
	    		if( entity instanceof Player){
	    			if (damageCheck(player, (LivingEntity) entity)){
	    				addSpellTarget(entity, hero);
		                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	    			}
	    			entity.setVelocity(flyer);
	    			hitsLeft -= 1;
	    		} 
	    	}
    	}else{
    		for(Entity entity : nearby){
    			if(hitsLeft <= 0) break;
	    		if( entity instanceof Monster){
	                addSpellTarget(entity, hero);
	                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	                entity.setVelocity(flyer);
	                hitsLeft -= 1;
	    		}
	    		if( entity instanceof Player){
	    			if (damageCheck(player, (LivingEntity) entity)){
	    				addSpellTarget(entity, hero);
		                damageEntity((LivingEntity) entity, player, newDmg, DamageCause.ENTITY_ATTACK);
	    			}
	    			entity.setVelocity(flyer);
	    			hitsLeft -= 1;
	    		} 
	    	}
    	}
    	broadcast(player.getLocation(), "$1 used ground pound", player.getDisplayName());
    	return SkillResult.NORMAL;
    }
    
   
    
}