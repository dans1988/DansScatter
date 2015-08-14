/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package pl.dans.plugins.scatter;

import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.plugin.java.JavaPlugin;
import pl.dans.plugins.scatter.executors.ScatterCommandExecutor;

/**
 *
 * @author Dans
 */
public class DansScatter extends JavaPlugin {
    

    @Override
    public void onEnable() {
        getLogger().log(Level.INFO, "{0}onEnable", ChatColor.RED);
        
        ScatterCommandExecutor executor = new ScatterCommandExecutor(this);
        
        getCommand("scatterall").setExecutor(executor);
        getCommand("scatterplayer").setExecutor(executor);
        
        getServer().getPluginManager().registerEvents(executor, this);
        
    }

    
    
    
}
