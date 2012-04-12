package dev.riffic33.heroes.skills;

import java.util.logging.Level;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Setting;

public class SkillAffliction extends TargettedSkill {
	
    public SkillAffliction(Heroes plugin) {
        super(plugin, "Affliction");
        setUsage("/skill affliction");
        setArgumentRange(0, 0);
        setIdentifiers("skill affliction");
        setTypes(SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.HARMFUL);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseTickDamage", 3);
        node.set("LevelMultiplier", 0.5);
        node.set(Setting.DURATION.node(), 12000);
        node.set(Setting.PERIOD.node(), 4000);
        node.set("MaxJumps", 3);
        node.set("MaxJumpDistance", 5);
        
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long period 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	long duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	int jumps 		= (int) SkillConfigManager.getUseSetting(hero, this, "MaxJumps", 3, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	String dJump = jumps > 0 ? "Jumps " +jumps+ " times":"";
    	
    	String base = String.format("Put a damage over time effect on the target dealing %s damage every %s seconds over %s seconds.", tickDmg, period/1000L, duration/1000L);
    	
        return base + dJump;
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
    	
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	long period 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	int maxJumps 	= (int) SkillConfigManager.getUseSetting(hero, this, "MaxJumps", 3, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	AfflictionEffect ae = new AfflictionEffect(this, period, duration, tickDmg-1, player, maxJumps);	//-1 QUICKFIX FOR HEROES BUG
    	if (target instanceof Player) {
    		plugin.getCharacterManager().getHero((Player) target).addEffect(ae);
    		
            return SkillResult.NORMAL;
        } else if (target instanceof LivingEntity) {
        	Monster mstr = plugin.getCharacterManager().getMonster( target );
        			mstr.addEffect( ae );
        	
            return SkillResult.NORMAL;
        } else 
        	Heroes.log(Level.WARNING, target+"");
            return SkillResult.INVALID_TARGET;
    }
    
    public class AfflictionEffect extends PeriodicDamageEffect{
    	//Future ?
    	//private HashSet<LivingEntity> tracked = new HashSet<LivingEntity>(30);
    
    	private String applyText = "Affiction cast on $1";
    	private String expireText = "Affiction removed from $1";
    	private int maxJumps;
    	private Skill skill;
    	
	    public AfflictionEffect(Skill skill, long period, long duration, int tickDmg, Player applier, int maxJumps){
				super(skill, "Affliction", period, duration, tickDmg, applier);
				this.types.add(EffectType.DISPELLABLE);
				this.types.add(EffectType.DARK);
				this.types.add(EffectType.HARMFUL);
				this.skill = skill;
				this.maxJumps = maxJumps;
		}  
	    
	    @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());    
        }
        
	    @Override
        public void applyToMonster(Monster entity) {
            super.applyToMonster(entity);
            broadcast( entity.getEntity().getLocation(), applyText, entity.getEntity().getClass().getSimpleName().substring(5));    
        }
       
        public void removeFromHero(Hero hero) {
        	Player player = hero.getPlayer();
        	broadcast(player.getLocation(), expireText, player.getDisplayName()); 
        	if(maxJumps-1 <= 0){
        		super.removeFromHero(hero);
        		return;
        	}else{
	        	AfflictionEffect ae = new AfflictionEffect(skill, this.getPeriod(), this.getDuration(), this.getTickDamage(), this.getApplier(), this.maxJumps-1);
	        	passEffect(this.applyHero, player, ae);
	            super.removeFromHero(hero); 
        	}
        }
        
        @Override
        public void removeFromMonster(Monster entity) {
        	broadcast( entity.getEntity().getLocation(), expireText, entity.getEntity().getClass().getSimpleName()); 
        	if(maxJumps-1 <= 0){
        		super.removeFromMonster(entity);
        		return;
        	}else{
	        	AfflictionEffect ae = new AfflictionEffect(skill, this.getPeriod(), this.getDuration(), this.getTickDamage(), this.getApplier(), this.maxJumps-1);
	        	passEffect(this.applyHero, entity, ae);
	            super.removeFromMonster(entity);  
        	}
        }
        	
        private void passEffect(Hero hero, Player entity, AfflictionEffect eff){
        	int radius = (int) SkillConfigManager.getUseSetting(hero, this.getSkill(), "MaxJumpDistance", 5, false);
        	for(Entity newTarget : ((Entity) entity).getNearbyEntities(radius, radius, radius)){
        		if(!(newTarget instanceof LivingEntity) || newTarget == eff.getApplier()){
        			continue;
        		}
            	if (newTarget instanceof Player) {
            		plugin.getCharacterManager().getHero((Player) newTarget).addEffect(eff);
                    break;
                }
        	}
        }
        
        private void passEffect(Hero hero, Monster entity, AfflictionEffect eff){
        	int radius = (int) SkillConfigManager.getUseSetting(hero, this.getSkill(), "MaxJumpDistance", 5, false);
        	for(Entity newTarget : entity.getEntity().getNearbyEntities(radius, radius, radius)){
        		if(!(newTarget instanceof LivingEntity) || newTarget == eff.getApplier()){
        			continue;
        		}
            	if (newTarget instanceof LivingEntity) {
                	Monster creature = plugin.getCharacterManager().getMonster( (LivingEntity) newTarget );
                			creature.addEffect(eff);
                    break;
                }
        	}
        }

        @Override
        public void tickHero(Hero hero) {
            super.tickHero(hero);
        }
        
        @Override
        public void tickMonster(Monster entity) {
            super.tickMonster(entity);
        }
	    
    }

    

}
