/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.dans.plugins.scatter.executors;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import pl.dans.plugins.scatter.DansScatter;
import pl.dans.plugins.scatter.ScatterPosition;

/**
 *
 * @author DanielWegner
 */
public class ScatterCommandExecutor implements CommandExecutor, Listener {

    private final DansScatter dansScatter;
    private int currentLoadingPosition;
    private int currentScatterPosition;
    
    private boolean allowChunkUnloading = true;

    public ScatterCommandExecutor(DansScatter dansScatter) {
        this.dansScatter = dansScatter;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if ("scatterall".equals(command.getName())) {

            if ("Console".equals(sender.getName())) {
                sender.sendMessage(ChatColor.RED + "You can't do that from the console!");
                return false;
            }

            final String worldName = args[0];

            final int radius;
            final int minScatterDistance;

            try {
                radius = Integer.parseInt(args[1]) - 1;
                minScatterDistance = Integer.parseInt(args[2]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Radius and minimum scatter distance have to be positive numbers!");
                return true;
            }

            World world = Bukkit.getServer().getWorld(worldName);

            if (world == null) {
                sender.sendMessage(ChatColor.RED + "World " + worldName + " does not exist!");
                return true;
            }
            
            

            final int posCount = Bukkit.getServer().getOnlinePlayers().length;
            
            
            
            Bukkit.broadcastMessage(getMessageStart() + "WARNING! This plugin is a work in progress!");
            Bukkit.broadcastMessage(getMessageStart() + "Starting the scatter!");
            Bukkit.broadcastMessage(getMessageStart() + "Number of players: " + posCount);
            Bukkit.broadcastMessage(getMessageStart() + "Radius: " + radius);
            Bukkit.broadcastMessage(getMessageStart() + "Minimum distance between teams/players: " + minScatterDistance);

            final List<ScatterPosition> scatterPositions = new ArrayList<ScatterPosition>();
            final List<String> players = new ArrayList<String>();
            for (Player player : Bukkit.getServer().getOnlinePlayers()) {
                players.add(player.getName());
            }

            //find positions
            Random random = new Random(new Date().getTime());
            for (int i = 0; i < posCount; i++) {

                int maxTries = 1001;

                while (--maxTries > 0) {
                    int x = random.nextInt(2 * radius) - radius;
                    int z = random.nextInt(2 * radius) - radius;

                    int y = world.getHighestBlockAt(x, z).getY();
                    
                    //verify if that is a proper location
                    Material blockType = world.getBlockAt(x, y - 1, z).getType();

                    
                    //check block type
                    if (Material.WATER.equals(blockType)
                            || Material.STATIONARY_WATER.equals(blockType)
                            || Material.BEDROCK.equals(blockType)
                            || Material.LAVA.equals(blockType)
                            || Material.STATIONARY_LAVA.equals(blockType)
                            || Material.CACTUS.equals(blockType)
                            || y < 60) {

                        continue;

                    }

                    boolean accept = true;
                    //calculate distances from previous locations
                    for (ScatterPosition previousPosition : scatterPositions) {
                        double prevX = previousPosition.getX();
                        double prevZ = previousPosition.getZ();

                        int distance = (int) Math.sqrt((double) ((prevX - x) * (prevX - x) + (prevZ - z) * (prevZ - z)));
                        if (distance < minScatterDistance) {
                            accept = false;
                            break;
                        }
                    }

                    //stop trying if this is a correct position
                    if (accept) {
                        
                        scatterPositions.add(new ScatterPosition(x, y, z));
                        break;
                    }
                }
                if (maxTries == 0) {
                    sender.sendMessage(ChatColor.RED + "Could not get proper positions!");
                    return true;
                }

            }
            
            Bukkit.broadcastMessage(getMessageStart() + "Positions found!");
            Bukkit.broadcastMessage(getMessageStart() + "Loading chunks! This will take about " + (double) posCount * 0.5 + " seconds!");
            
            
            currentLoadingPosition = 0;

            final String senderName = sender.getName();
            allowChunkUnloading = false;

            //load chunks(tp player every 10 ticks)
            BukkitTask loadingTask = new BukkitRunnable() {

                @Override
                public void run() {


                    if (currentLoadingPosition >= scatterPositions.size()) {
                        Bukkit.broadcastMessage(getMessageStart() + "Chunks loaded!");
                        Bukkit.broadcastMessage(getMessageStart() + "Scattering!");
                        this.cancel();
                    }
                    ScatterPosition scatterPosition = scatterPositions.get(currentLoadingPosition);

                    Bukkit.getServer().getPlayer(senderName)
                            .teleport(new Location(Bukkit.getWorld(worldName),
                                            scatterPosition.getX() + 0.5f,
                                            scatterPosition.getY() + 1,
                                            scatterPosition.getZ() + 0.5f));

                    currentLoadingPosition++;

                }
            }.runTaskTimer(dansScatter, 0, 10);
            
            

            currentScatterPosition = 0;
            BukkitTask scatterTask = new BukkitRunnable() {

                @Override
                public void run() {

                    

                    if (currentScatterPosition >= scatterPositions.size()) {
                        allowChunkUnloading = true;
                        Bukkit.getServer().broadcastMessage(getMessageStart() + "All players scattered!");
                        this.cancel();
                    }

                    Player player = Bukkit.getServer().getPlayer(players.get(currentScatterPosition));
                    
                    

                    ScatterPosition scatterPosition = scatterPositions.get(currentScatterPosition);
                    if (player != null) {
                        
                        Bukkit.broadcastMessage(getMessageStart() + "Scattering " + player.getName() + " [" + currentScatterPosition + "/" + scatterPositions.size() + "]");
                        
                        player.teleport(new Location(Bukkit.getWorld(worldName),
                                scatterPosition.getX() + 0.5f,
                                scatterPosition.getY() + 1,
                                scatterPosition.getZ() + 0.5f));
                    }

                    currentScatterPosition++;

                }
            }.runTaskTimer(dansScatter, 10 * scatterPositions.size() + 40, 10);

            

            return true;
        } else {        
            return false;
        }
    }
    
    @EventHandler(ignoreCancelled = true)
    public void onChunkUnload(final ChunkUnloadEvent event) {
        event.setCancelled(!allowChunkUnloading);
    }
    
    private String getMessageStart() {
        return ChatColor.RED + "[" + ChatColor.LIGHT_PURPLE + "DansScatter"
                + ChatColor.RED + "] " + ChatColor.YELLOW;
    }
    
    

}
