package dev.riffic33.heroes.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicDamageEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;


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
            plugin.getHeroManager().getHero((Player) target).addEffect(ae);
            return SkillResult.NORMAL;
        } else if (target instanceof LivingEntity) {
            LivingEntity creature = (LivingEntity) target;
            plugin.getEffectManager().addEntityEffect(creature, ae);
            return SkillResult.NORMAL;
        } else 
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
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());    
        }
        
        @Override
        public void apply(LivingEntity entity) {
            super.apply(entity);
            broadcast(entity.getLocation(), applyText, entity.getClass().getSimpleName().substring(5));    
        }
       
        @Override
        public void remove(Hero hero) {
        	Player player = hero.getPlayer();
        	broadcast(player.getLocation(), expireText, player.getDisplayName()); 
        	if(maxJumps-1 <= 0){
        		super.remove(hero);
        		return;
        	}else{
	        	AfflictionEffect ae = new AfflictionEffect(skill, this.getPeriod(), this.getDuration(), this.getTickDamage(), this.getApplier(), this.maxJumps-1);
	        	passEffect(this.applyHero, player, ae);
	            super.remove(hero); 
        	}
        }
        
        @Override
        public void remove(LivingEntity entity) {
        	broadcast(entity.getLocation(), expireText, entity.getClass().getSimpleName()); 
        	if(maxJumps-1 <= 0){
        		super.remove(entity);
        		return;
        	}else{
	        	AfflictionEffect ae = new AfflictionEffect(skill, this.getPeriod(), this.getDuration(), this.getTickDamage(), this.getApplier(), this.maxJumps-1);
	        	passEffect(this.applyHero, entity, ae);
	            super.remove(entity);  
        	}
        }
        	
        private void passEffect(Hero hero, LivingEntity entity, AfflictionEffect eff){
        	int radius = (int) SkillConfigManager.getUseSetting(hero, this.getSkill(), "MaxJumpDistance", 5, false);
        	for(Entity newTarget : entity.getNearbyEntities(radius, radius, radius)){
        		if(!(newTarget instanceof LivingEntity) || newTarget == eff.getApplier()){
        			continue;
        		}
            	if (newTarget instanceof Player) {
                    plugin.getHeroManager().getHero((Player) newTarget).addEffect(eff);
                    break;
                } else if (newTarget instanceof LivingEntity) {
                    LivingEntity creature = (LivingEntity) newTarget;
                    plugin.getEffectManager().addEntityEffect(creature, eff);
                    break;
                }
        	}
        }

        @Override
        public void tick(Hero hero) {
            super.tick(hero);
        }
        
        @Override
        public void tick(LivingEntity entity) {
            super.tick(entity);
        }
	    
    }

    

}
