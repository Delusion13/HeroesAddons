package dev.riffic33.heroes.skills;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.effects.EffectType;
import com.herocraftonline.dev.heroes.effects.ExpirableEffect;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.Skill;
import com.herocraftonline.dev.heroes.skill.SkillConfigManager;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Setting;
import com.herocraftonline.dev.heroes.util.Util;


public class SkillEmpower extends ActiveSkill {
	
    public SkillEmpower(Heroes plugin) {
        super(plugin, "Empower");
        setUsage("/skill empower");
        setArgumentRange(0, 0);
        setIdentifiers("skill empower");
        setTypes(SkillType.BUFF, SkillType.PHYSICAL);  
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(this), plugin);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 5000);	
        node.set("BaseDamage", 2);
        node.set("LevelMultiplier", 0.5);
        node.set("Percentage", 10);
        node.set("LevelPercentageIncrease", 0.1);
        
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseDamage", 3, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int newDmg 		= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
    	int bPercent 		= (int) SkillConfigManager.getUseSetting(hero, this, "Percentage", 10, false);
    	float bPerMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelPercentageIncrease", 2, false);
    	float newPercent 	= (int) (bPerMulti <= 0L ? bPercent : bPercent + bPerMulti*hero.getLevel());
    	
    	return String.format("Your next physical attack deals %s damage and takes %s of the target's health. Buff lasts for %s seconds", newDmg, newPercent+"%", duration/1000);
    }
    
    @Override
	public SkillResult use(Hero hero, String[] arg1) {
    	
    	int duration 		= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 5000, false);
    	hero.addEffect(new EmpowerBuff(this, duration));
    	
		return SkillResult.NORMAL;
	}

    public class EmpowerBuff extends ExpirableEffect{
    
    	private final String applyText = "$1 has been Empowered";
    	private final String expireText = "$1 lost Empowerment";
    	
	    public EmpowerBuff(Skill skill, long duration){
				super(skill, "EmpowerBuff", duration);
				this.types.add(EffectType.BENEFICIAL);
				this.types.add(EffectType.PHYSICAL);
		}  

        @Override
        public void apply(Hero hero) {
            super.apply(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
        }

        @Override
        public void remove(Hero hero) {
            super.remove(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
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
            Hero hero = plugin.getHeroManager().getHero(player);
            if (hero.hasEffect("EmpowerBuff")) {
            	
                ItemStack item = player.getItemInHand();
                if (!Util.swords.contains(item.getType().name()) && !Util.axes.contains(item.getType().name())) {
                    return;
                }   
                EmpowerBuff eb = (EmpowerBuff) hero.getEffect("EmpowerBuff");
                
                hero.removeEffect(eb);
                
                int bDmg 			= (int) SkillConfigManager.getUseSetting(hero, skill, "BaseDamage", 3, false);
            	float bMulti 		= (float) SkillConfigManager.getUseSetting(hero, skill, "LevelMultiplier", 0.5, false);
            	int newDmg 			= (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
            	
            	int bPercent 		= (int) SkillConfigManager.getUseSetting(hero, skill, "Percentage", 10, false);
            	float bPerMulti 	= (float) SkillConfigManager.getUseSetting(hero, skill, "LevelPercentageIncrease", 2, false);
            	float newPercent 		= (int) (bPerMulti <= 0L ? bPercent : bPercent + bPerMulti*hero.getLevel());
            	
            	int addPercentDamage = 0;
            	if(initTarg instanceof Player){
            		Hero targHero = plugin.getHeroManager().getHero((Player) initTarg);
            		addPercentDamage = (int) ((int) targHero.getMaxHealth()*(newPercent/100f));
            	}else{
            		addPercentDamage = (int) (((LivingEntity) initTarg).getMaxHealth()*(newPercent/100f));
            	}

                event.setDamage( newDmg+addPercentDamage);
               
                broadcast(player.getLocation(),"$1 used an Empowered attack", player.getDisplayName());
            }
        }	
    }

}
