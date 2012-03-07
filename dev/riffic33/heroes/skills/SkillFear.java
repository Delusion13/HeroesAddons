package dev.riffic33.heroes.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Setting;


public class SkillFear extends TargettedSkill {
	
    public SkillFear(Heroes plugin) {
        super(plugin, "Fear");
        setUsage("/skill fear");
        setArgumentRange(0, 0);
        setIdentifiers("skill fear");
        setTypes(SkillType.SILENCABLE, SkillType.DARK, SkillType.DEBUFF);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);	
        node.set("FearStrength", 0.5);
        node.set("FearStrengthPerLevel", 0.1);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int duration = (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	float fearStrength 	= (float) SkillConfigManager.getUseSetting(hero, this, "FearStrength", 0.5, false);
    	float strengthMulti = (float) SkillConfigManager.getUseSetting(hero, this, "FearStrengthPerLevel", 0.1, false);
    	float newStrength 	= (float) (strengthMulti <= 0L ? fearStrength : fearStrength + strengthMulti*hero.getLevel());
    	return String.format("Causes your target to flee for %s seconds at a strength of %s", duration/1000, newStrength);
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
    	
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            Messaging.send(player, "Can't fear the target");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	
    	int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	float fearStrength 	= (float) SkillConfigManager.getUseSetting(hero, this, "FearStrength", 0.5, false);
    	float strengthMulti = (float) SkillConfigManager.getUseSetting(hero, this, "FearStrengthPerLevel", 0.1, false);
    	float newStrength 	= (float) (strengthMulti <= 0L ? fearStrength : fearStrength + strengthMulti*hero.getLevel());
    	
    	FearEffect fe = new FearEffect(this,  duration, player, newStrength);
    	if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(fe);
            return SkillResult.NORMAL;
        } else if (target instanceof LivingEntity) {
            LivingEntity creature = (LivingEntity) target;
            plugin.getEffectManager().addEntityEffect(creature, fe);
            return SkillResult.NORMAL;
        } else 
            return SkillResult.INVALID_TARGET;
    }
    
    
    
    public class FearEffect extends PeriodicExpirableEffect{
    
    	private final String applyText = "$1 has been feared";
    	private final String expireText = "Fear Removed from $1";
    	private final Vector mover;
    	private final float yaw;
    	private int swtch;
    	
	    public FearEffect(Skill skill, long duration, Player applier, float strength){
				super(skill, "FearEffect", 100, duration);
				this.types.add(EffectType.DISABLE);
				this.types.add(EffectType.DARK);
				this.types.add(EffectType.DISPELLABLE);
				Vector tempVec = applier.getLocation().getDirection();
				Vector tempVecX = new Vector(tempVec.getX(),0, 0);
				Vector tempVecZ = new Vector(0, 0, tempVec.getZ());
				this.mover = tempVecX.add(tempVecZ).normalize().multiply(strength);
				this.yaw = applier.getLocation().getYaw();
				this.swtch = 1;	
		}  

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }
        
        @Override
        public void apply(LivingEntity e) {
            super.apply(e);
            broadcast(e.getLocation(), applyText, e.getClass().getSimpleName().substring(5));
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
        
        @Override
        public void remove(LivingEntity e) {
            super.apply(e);
            broadcast(e.getLocation(), expireText, e.getClass().getSimpleName().substring(5));
        }
        
        @Override
        public void tick(Hero hero) {
            super.tick(hero);
            Player p = hero.getPlayer(); 
			if(swtch > 0){
            	p.setVelocity(mover);
			}else{
            	Location lkDir = p.getLocation();
            			 lkDir.setYaw(yaw);
            			 p.teleport(lkDir);
            }
            swtch = -swtch;
        }
        
        @Override
        public void tick(LivingEntity e) {
            super.tick(e);
            if(swtch > 0){
            	e.setVelocity(mover);
			}else{
            	Location lkDir = e.getLocation();
            			 lkDir.setYaw(yaw);
            			 e.teleport(lkDir);
            }
            swtch = -swtch;
        }
    }

   

}
