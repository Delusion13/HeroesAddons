package dev.riffic33.heroes.skills;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.PeriodicExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;


public class SkillBlizzard extends ActiveSkill{
	
    public SkillBlizzard(Heroes plugin) {
        super(plugin, "Blizzard");
        setDescription("Rapid fire snowballs for $1 seconds");
        setUsage("/skill blizzard");
        setArgumentRange(0, 0);
        setIdentifiers("skill blizzard");
        setTypes(SkillType.ICE, SkillType.HARMFUL, SkillType.SILENCABLE);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);
        node.set("DurationLevelMultiplier", 0.1);
        return  node;
    }
    
    @Override
	public String getDescription(Hero hero) {
		int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	int durMulti 		= (int) SkillConfigManager.getUseSetting(hero, this, "DurationLevelMultiplier", 100, false);
    	int newDur 		= (int) (durMulti <= 0L ? duration : duration + durMulti*hero.getLevel());
    	
		return String.format("Rapid fire snowballs for %s seconds", newDur/1000);
	}
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	int durMulti 		= (int) SkillConfigManager.getUseSetting(hero, this, "DurationLevelMultiplier", 100, false);
    	int newDur 		= (int) (durMulti <= 0L ? duration : duration + durMulti*hero.getLevel());
    	
    	BlizzardEffect be = new BlizzardEffect(this, newDur, player);
    	hero.addEffect(be);
        return SkillResult.NORMAL;
    }
    
    public class BlizzardEffect extends PeriodicExpirableEffect{
    	Player user;
    
    	public BlizzardEffect(Skill skill, long duration, Player user) {
			super(skill, "BlizzardEffect", 200, duration);		
			this.types.add(EffectType.DISPELLABLE);
			this.types.add(EffectType.HARMFUL);
			this.types.add(EffectType.ICE);
			this.user = user;
    	}
    	
    	@Override
    	public void apply(Hero hero) {
            super.apply(hero);
            broadcast(user.getLocation(), "$1 gained Blizzard", user.getDisplayName());    
        }
    	
    	 @Override
         public void remove(Hero hero) {
         	broadcast(user.getLocation(), "$1 lost Blizzard", user.getDisplayName()); 
         }
    	 
    	 @Override
         public void tick(Hero hero) {
             super.tick(hero);
             user.throwSnowball();
         }
    	
    }

	
	
	
}
