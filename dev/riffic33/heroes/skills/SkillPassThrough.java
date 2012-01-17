package dev.riffic33.heroes.skills;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import com.herocraftonline.dev.heroes.Heroes;
import com.herocraftonline.dev.heroes.api.SkillResult;
import com.herocraftonline.dev.heroes.hero.Hero;
import com.herocraftonline.dev.heroes.skill.ActiveSkill;
import com.herocraftonline.dev.heroes.skill.SkillType;
import com.herocraftonline.dev.heroes.util.Messaging;
import com.herocraftonline.dev.heroes.util.Util;

public class SkillPassThrough extends ActiveSkill {
	
    public SkillPassThrough(Heroes plugin) {
        super(plugin, "Passthrough");
        setDescription("Move through a wall of blocks");
        setUsage("/skill passthrough");
        setArgumentRange(0, 0);
        setIdentifiers("skill passthrough");
        setTypes(SkillType.MOVEMENT, SkillType.PHYSICAL);  
    }
   
    @Override
    public ConfigurationSection getDefaultConfig() {
        ConfigurationSection node = super.getDefaultConfig();
        return  node;
    }
    
    @Override
    public SkillResult use(Hero hero, String[] args) {
    	Player player = hero.getPlayer();
    	int distance = 3;
    	
        boolean passBlock = false;
        boolean tpPlayer = false;
        Integer blockCount = 0;
        Block b = null;
        BlockIterator iter = null;
        try {
            iter = new BlockIterator(player, distance);
        } catch (IllegalStateException e) {
            Messaging.send(player, "There was an error getting your pass through location!");
            return SkillResult.INVALID_TARGET_NO_MSG;
        }
        while (iter.hasNext()) {
            b = iter.next();
            if ((!Util.transparentBlocks.contains(b.getType()) || !Util.transparentBlocks.contains(b.getType())) && blockCount == 1) {
                passBlock = true; 
            } else {
            	if(blockCount == 2 && Util.transparentBlocks.contains(b.getType()) && Util.transparentBlocks.contains(b.getRelative(BlockFace.DOWN).getType())){
            		tpPlayer = true;
            		break;
            	}
            }
            blockCount++;
        }
        if (passBlock) {
        	if(tpPlayer){
	            broadcastExecuteText(hero);
	           
	            Location tpLoc = b.getLocation().clone();
	            tpLoc.setPitch(player.getLocation().getPitch());
	            tpLoc.setYaw(player.getLocation().getYaw());
	            player.teleport(tpLoc);
	            return SkillResult.NORMAL;
        	}else{
        		Messaging.send(player, "This is a thicker wall than you can pass through");
        		return SkillResult.FAIL;
        	}
        } else {
        	Messaging.send(player, "Invalid selection to pass through");
            return SkillResult.FAIL;
        }
    }
    
    @Override
    public String getDescription(Hero hero) {
        return getDescription();
    }
    
}