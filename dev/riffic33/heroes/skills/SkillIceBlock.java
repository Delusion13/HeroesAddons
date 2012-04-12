package dev.riffic33.heroes.skills;

import java.util.HashSet;
import java.util.Iterator;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import com.herocraftonline.heroes.Heroes;
import com.herocraftonline.heroes.api.SkillResult;
import com.herocraftonline.heroes.characters.Hero;
import com.herocraftonline.heroes.characters.Monster;
import com.herocraftonline.heroes.characters.effects.Effect;
import com.herocraftonline.heroes.characters.effects.EffectType;
import com.herocraftonline.heroes.characters.effects.PeriodicDamageEffect;
import com.herocraftonline.heroes.characters.effects.PeriodicExpirableEffect;
import com.herocraftonline.heroes.characters.skill.Skill;
import com.herocraftonline.heroes.characters.skill.SkillConfigManager;
import com.herocraftonline.heroes.characters.skill.SkillType;
import com.herocraftonline.heroes.characters.skill.TargettedSkill;
import com.herocraftonline.heroes.util.Messaging;
import com.herocraftonline.heroes.util.Setting;


public class SkillIceBlock extends TargettedSkill {
	
    public SkillIceBlock(Heroes plugin) {
        super(plugin, "IceBlock");
        setUsage("/skill iceblock");
        setArgumentRange(0, 0);
        setIdentifiers("skill iceblock");
        setTypes(SkillType.SILENCABLE, SkillType.ICE, SkillType.DEBUFF);  
        
        Bukkit.getServer().getPluginManager().registerEvents(new SkillListener(), plugin);
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        node.set("BaseTickDamage", 0);
        node.set("LevelMultiplier", 0.0);
        node.set(Setting.DURATION.node(), 12000);
        node.set(Setting.PERIOD.node(), 4000);
        return  node;
    }
    
    @Override
    public String getDescription(Hero hero) {
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 0, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0, false);
    	long duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	long period 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	
        String base =  String.format("Encapsulate your target in ice for %s seconds. ", duration/1000D);
        
        return tickDmg > 0 ? base.concat("Deals " + tickDmg + " every " + period + " seconds.") : base;
    }
    
    @Override
    public SkillResult use(Hero hero, LivingEntity target, String[] args) {
    	Player player = hero.getPlayer();
    	
    	if (player.equals(target) || hero.getSummons().contains(target) || !damageCheck(player, target)) {
            Messaging.send(player, "Can't freeze the target");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
    	int bDmg 		= (int) SkillConfigManager.getUseSetting(hero, this, "BaseTickDamage", 0, false);
    	float bMulti 	= (float) SkillConfigManager.getUseSetting(hero, this, "LevelMultiplier", 0.0, false);
    	long duration 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.DURATION, 12000, false);
    	long period 	= (int) SkillConfigManager.getUseSetting(hero, this, Setting.PERIOD.node(), 4000, false);
    	int tickDmg = (int) (bMulti <= 0L ? bDmg : bDmg + bMulti*hero.getLevel());
    	IceBlockEffect ibe = new IceBlockEffect(this,  duration);
    	if( tickDmg > 0){
    		IceBlockDmgEffect ibde = new IceBlockDmgEffect(this, period, duration, tickDmg, player);
	    	if (target instanceof Player) {
	    		plugin.getCharacterManager().getHero( (Player) target ).addEffect(ibe);
	    		plugin.getCharacterManager().getHero( (Player) target ).addEffect(ibde);
	            return SkillResult.NORMAL;
	        } else if (target instanceof LivingEntity) {
	        	//POSSIBLE FUTURE IMPLEMENTATION
	            return SkillResult.INVALID_TARGET;
	        } else 
	            return SkillResult.INVALID_TARGET;
    	}else{
    		if (target instanceof Player) {
    			plugin.getCharacterManager().getHero( (Player) target ).addEffect(ibe);
	            return SkillResult.NORMAL;
	        } else if (target instanceof LivingEntity) {
	        	//POSSIBLE FUTURE IMPLEMENTATION
	            return SkillResult.INVALID_TARGET;
	        } else 
	            return SkillResult.INVALID_TARGET;
    	}
    }
    
    
    
    public class IceBlockEffect extends PeriodicExpirableEffect{
    
    	private HashSet<Block> blocks;
    	private final String applyText = "$1 has been put in an Ice Block";
    	private final String expireText = "Ice block Removed from $1";
    	private Location loc;
    	
	    public IceBlockEffect(Skill skill, long duration) {
				super(skill, "IceBlockEffect", 100, duration);
				this.types.add(EffectType.DISABLE);
				this.types.add(EffectType.STUN);
				this.types.add(EffectType.ICE);
		}  

        @Override
        public void applyToHero(Hero hero) {
            super.applyToHero(hero);
            Player player = hero.getPlayer();
            broadcast(player.getLocation(), applyText, player.getDisplayName());
            Location pLoc = player.getLocation();
            Location pBlockLoc= pLoc.getBlock().getLocation();
            Location tpLoc = new Location(pLoc.getWorld(), pBlockLoc.getX()+0.5D, pBlockLoc.getY(), pBlockLoc.getZ()+0.5D);
            		tpLoc.setYaw(pLoc.getYaw());
            		tpLoc.setPitch(pLoc.getPitch());
            player.teleport(tpLoc);
            this.loc = tpLoc;
            this.blocks = placeIceBlock(player);
        }

        @Override
        public void removeFromHero(Hero hero) {
            super.removeFromHero(hero);
            Player player = hero.getPlayer();
            Iterator<Block> iceIter = blocks.iterator();
            while(iceIter.hasNext()){
            	Block bChange = iceIter.next();
            	if(bChange.getType() == Material.ICE){
            		bChange.setType(Material.AIR);
            	}
            }
            if( hero.hasEffect("IceBlockDmgEffect") ){
            	Effect eff = hero.getEffect("IceBlockDmgEffect");
            	hero.removeEffect(eff);
            }
            broadcast(player.getLocation(), expireText, player.getDisplayName());
        }
        
        @Override
        public void tickHero(Hero hero) {
            Player p = hero.getPlayer();
            Location location = p.getLocation();
            if (location == null)
                return;
            if (location.getX() != loc.getX() || location.getY() != loc.getY() || location.getZ() != loc.getZ()) {
                loc.setYaw(location.getYaw());
                loc.setPitch(location.getPitch());
                p.teleport(loc);
            }
        }

		@Override
		public void tickMonster(Monster arg0) {
		}
	    
    }
    
    public class IceBlockDmgEffect extends PeriodicDamageEffect{
    	
    	public IceBlockDmgEffect( Skill skill, long period, long duration, int tickDmg, Player applier ) {
			super(skill, "IceBlockDmgEffect", period, duration, tickDmg, applier);
			this.types.add(EffectType.ICE);
			this.types.add(EffectType.HARMFUL);
    	}  

        @Override
        public void removeFromHero( Hero hero ) {
            super.removeFromHero(hero);
            if( hero.hasEffect("IceBlockEffect") ){
            	Effect eff = hero.getEffect("IceBlockEffect");
            	hero.removeEffect(eff);
            }
            
        }	
    	
    }
    
    private HashSet<Block> placeIceBlock( LivingEntity target ){
    	HashSet<Block> blocks = new HashSet<Block>(20);
    	Block iceLoc = target.getLocation().getBlock();
    	for(int y=0; y<2; y++){
    		for(int x=-1; x<2; x++){
    			for(int z=-1; z<2; z++){
        			if(iceLoc.getRelative( x, y, z ).isEmpty()){
    		    			Block iBlock = iceLoc.getRelative( x, y, z );
    						iBlock.setType(Material.ICE);
    						blocks.add(iBlock);
    				}
        		}
    		}	
    	}
    	return blocks;
    }
    
    public class SkillListener implements Listener{
    	
    	@EventHandler
    	public void onBlockBreak( BlockBreakEvent event ){
    		Player player 	= event.getPlayer();
    		Hero hero 		= plugin.getCharacterManager().getHero( player );
    		if(hero.hasEffect("IceBlockEffect")){
    			event.setCancelled(true);
    		}
    	}
  
    }
    
   

}
