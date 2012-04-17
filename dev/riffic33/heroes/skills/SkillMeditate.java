package dev.riffic33.heroes.skills;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.api.events.HeroEnterCombatEvent;
import com.herocraftonline.heroes.api.events.HeroRegainHealthEvent;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.ActiveSkill;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.util.Setting;


public class SkillMeditate extends ActiveSkill {
	
    public SkillMeditate(Heroes plugin) {
        super(plugin, "Meditate");
        setUsage("/skill meditate");
        setArgumentRange(0, 0);
        setIdentifiers("skill meditate");
        setTypes(SkillType.BUFF);  
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set(Setting.DURATION.node(), 10000);
        node.set(Setting.PERIOD.node(), 2000);
        node.set("MovingCancels", true);
        node.set("CombatCancels", true);
        node.set("BaseHeals", 10);
        node.set("LevelMultiplier", 0.5);
        
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	long duration 	= (long) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
    	long period 	= (long) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 2000, false);
    	long interval 	= duration / period;
    	
    	int bHeal 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseHeals", 10, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int newHeal 	= (int) (bMulti <= 0L ? bHeal : bHeal + bMulti*hero.getLevel());
    	
    	boolean moveCancels		= (boolean) SkillConfigManager.getUseSetting(hero, this, "MovingCancels", true);
    	boolean combatCancels	= (boolean) SkillConfigManager.getUseSetting(hero, this, "CombatCancels", true);
    	String cancelsMsg = "";
    	
    	if( moveCancels || combatCancels ){
    		cancelsMsg = "Effect is canceled ";
    		
    		if( moveCancels && combatCancels ){
    			cancelsMsg += "with movement or on entering combat";
    		}else{
    			cancelsMsg += moveCancels ? "with movement" : "on entering combat";
    		}	
    	}
    	
    	return String.format("You meditate, healing %s health every %s second(s) over %s seconds. %s", newHeal/interval, period/1000, duration/1000, cancelsMsg);
    }
    
    @Override
	public SkillResult use(Hero hero, String[] arg1) {
    	
    	long duration 	= (long) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 10000, false);
    	long period 	= (long) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD, 2000, false);
    	
    	int bHeal 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseHeals", 10, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.5, false);
    	int newHeal 	= (int) (bMulti <= 0L ? bHeal : bHeal + bMulti*hero.getLevel());
    	
    	if( !hero.isInCombat() ){
    		hero.addEffect(new MeditateBuff(this, period,  duration, newHeal, hero.getPlayer().getLocation().getBlock().getLocation() ));
    		return SkillResult.NORMAL;
    	}else{
    		return SkillResult.NO_COMBAT;
    	}
		
	}

    public class MeditateBuff extends PeriodicExpirableEffect{
    
    	private final String applyText = "$1 is meditating";
    	private final String expireText = "$1 stopped meditating";
    	private final int healAmount;
    	private final Location startLoc;
    	
	    public MeditateBuff(Skill skill, long period, long duration, int healAmount, Location start){
				super(skill, "MeditateBuff", period, duration);
				this.types.add(EffectType.BENEFICIAL);
				this.types.add(EffectType.HEAL);
				this.healAmount = healAmount;
				this.startLoc = start;
		}  

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast( startLoc, applyText, player.getDisplayName());
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }

		@Override
		public void tickHero(Hero hero) {
            int newHealth = (int) (healAmount / ( this.getDuration() / this.getPeriod() ) );
			
			HeroRegainHealthEvent regain = new HeroRegainHealthEvent(hero, (int) newHealth, skill);
            plugin.getServer().getPluginManager().callEvent(regain);
            
            if (regain.isCancelled()) {
                return;
            }
            
            if( !hero.getPlayer().getLocation().getBlock().getLocation().equals( startLoc ) || hero.isInCombat() ){
            	hero.removeEffect( this );
            }else{
            	hero.setHealth( hero.getHealth() + regain.getAmount() );
            	hero.syncHealth();
            }
            
		}

		@Override
		public void tickMonster(Monster arg0) {
		}
        
    }

    public class SkillListener implements Listener{
        
        @EventHandler(priority = EventPriority.NORMAL)
        public void onEnterCombat(HeroEnterCombatEvent  event) {
        	
        	Hero hero = event.getHero();
        	MeditateBuff mb = (MeditateBuff) hero.getEffect("MeditateBuff");
        	if( mb != null ){
        		hero.removeEffect( mb );
        	}
        }	
    }

}
