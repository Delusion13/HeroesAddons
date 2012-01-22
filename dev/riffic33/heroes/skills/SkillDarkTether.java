package dev.riffic33.heroes.skills;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.skill.TargettedSkill;
import com.herocraftonline.dev.heroes.util.Setting;


public class SkillDarkTether extends TargettedSkill {
	
    public SkillDarkTether(Heroes plugin) {
        super(plugin, "Darktether");
        setDescription("Put a damage over time effect on the target dealing $1 damage times how $2 target is to the caster every $3 seconds over $4 seconds. Max effective distance of $5 blocks");
        setUsage("/skill darktether");
        setArgumentRange(0, 0);
        setIdentifiers("skill darktether");
        setTypes(SkillType.SILENCABLE, SkillType.DEBUFF, SkillType.HARMFUL, SkillType.DARK);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseTickDamage", 3);
        node.set("LevelMultiplier", 0.5);
        node.set(Setting.DURATION.node(), 12000);
        node.set(Setting.PERIOD.node(), 4000);
        node.set("DistanceMultiplier", 0.5);
        node.set("MaxDistance", 10);
        node.set("DamageByCloseness", false);
        
        return  node;
    }
    
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
    	
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	long period 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	float distMod 		= (float) SkillConfigManager.getUseSetting(hero, this, "DistanceMultiplier", 0.5, false);
    	int maxDist 		= (int) SkillConfigManager.getUseSetting(hero, this, "MaxDistance", 3, false);
    		maxDist			= maxDist <= 0 ? 1 : maxDist;
    	boolean closeness 	= (boolean) SkillConfigManager.getUseSetting(hero, this, "DamageByCloseness", false);
    	int tickDmg 		= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	DarkTetherEffect dte = new DarkTetherEffect(this, period, duration, tickDmg, player, closeness, distMod, maxDist);
    	if (target instanceof Player) {
            plugin.getHeroManager().getHero((Player) target).addEffect(dte);
            return SkillResult.NORMAL;
        } else if (target instanceof LivingEntity) {
            LivingEntity creature = (LivingEntity) target;
            plugin.getEffectManager().addEntityEffect(creature, dte);
            return SkillResult.NORMAL;
        } else 
            return SkillResult.INVALID_TARGET;
    }
    
    public class DarkTetherEffect extends PeriodicExpirableEffect{
    	private String applyText;
    	private String expireText;
    	private Skill skill;
    	
    	private final boolean closeness;
    	private final float distMod;
    	private final int maxDist;
    	protected int baseDmg;
    	protected Player applier;
        protected Hero applyHero;
    	
	    public DarkTetherEffect(Skill skill, long period, long duration, int tickDmg, Player applier, boolean closeness, float distMod, int maxDist){
				super(skill, "Affliction", period, duration);
				this.types.add(EffectType.DISPELLABLE);
				this.types.add(EffectType.DARK);
				this.types.add(EffectType.HARMFUL);
				this.skill = skill;
				this.closeness = closeness;
				this.distMod = distMod;
				this.maxDist = maxDist;
				this.baseDmg = tickDmg;
				this.applier = applier;
				this.applyHero = plugin.getHeroManager().getHero(applier);
				this.applyText = "DarkTether cast on $1";
				this.expireText = "DarkTether removed from $1";
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
        	super.remove(hero);
        	Player player = hero.getPlayer();
        	broadcast(player.getLocation(), expireText, player.getDisplayName()); 	
        }
        
        @Override
        public void remove(LivingEntity entity) {
        	super.remove(entity);  
        	broadcast(entity.getLocation(), expireText, entity.getClass().getSimpleName()); 
        }
        
        @Override
        public void tick(Hero hero) {
            super.tick(hero);
            Player player = hero.getPlayer();

            if (!skill.damageCheck(applier, player))
                return;
            
            skill.addSpellTarget(player, applyHero);
            skill.damageEntity(player, applier, calcDmg(baseDmg, closeness, maxDist, distMod, applier, player), DamageCause.ENTITY_ATTACK);
        }
        
        @Override
        public void tick(LivingEntity entity) {
        	super.tick(entity);
            skill.addSpellTarget(entity, applyHero);
            skill.damageEntity(entity, applier, calcDmg(baseDmg, closeness, maxDist, distMod, applier, entity), DamageCause.ENTITY_ATTACK);
        }
	    
    }
    
    private int calcDmg(int baseDmg, boolean closeness, int maxDist, float distMod, Player applier, LivingEntity entity){
    	Location targLoc = entity.getLocation();
    	int rDmg = baseDmg;
    	float dist = (float) Math.abs(applier.getLocation().distance(targLoc));
    		  dist = dist > maxDist ? maxDist : dist;
    	rDmg += closeness ? (int) ((float)(maxDist - dist) * distMod) : (int) ((float)(dist) * distMod);
		return rDmg;	
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 3, false);
    	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	long duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	long period 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	int maxDisti 		= (int) SkillConfigManager.getUseSetting(hero, this, "MaxDistance", 3, false);
    	int maxDist			= maxDisti <= 0 ? 1 : maxDisti;
    	boolean closeNess 	= (boolean) SkillConfigManager.getUseSetting(hero, this, "DamageByCloseness", false);
    	int tickDmg 		= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
        return getDescription().replace("$1", tickDmg + "").replace("$2", closeNess ? "close" : "far away").replace("$3", period/1000 + "").replace("$4", duration/1000 + "").replace("$5", maxDist + "");
    }

}
