package org.dynmap.towny;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.scheduling.TaskScheduler;
import com.palmergames.bukkit.towny.scheduling.impl.BukkitTaskScheduler;
import com.palmergames.bukkit.towny.scheduling.impl.FoliaTaskScheduler;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapAPI;
import org.dynmap.DynmapWebChatEvent;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerIcon;
import org.dynmap.markers.MarkerSet;
import org.dynmap.markers.PlayerSet;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.Version;
import com.palmergames.util.StringMgmt;
import com.palmergames.bukkit.TownyChat.Chat;
import org.dynmap.towny.events.*;
import org.dynmap.towny.objects.TownStyle;

public class DynmapTownyPlugin extends JavaPlugin {
	
	private static final Version requiredTownyVersion = Version.fromString("0.99.0.6");
    private static Logger log;
    private static final String DEF_INFOWINDOW = "<div class=\"infowindow\"><span style=\"font-size:120%;\">%regionname% (%nation%)</span><br /> Mayor <span style=\"font-weight:bold;\">%playerowners%</span><br /> Associates <span style=\"font-weight:bold;\">%playermanagers%</span><br/>Flags<br /><span style=\"font-weight:bold;\">%flags%</span></div>";
    Plugin dynmap;
    DynmapAPI api;
    MarkerAPI markerapi;
    int townblocksize;
    Chat townychat;
    boolean reload = false;
    private boolean playersbytown;
    private boolean playersbynation;
    private boolean dynamicNationColorsEnabled;
    private boolean dynamicTownColorsEnabled;
        
    FileConfiguration cfg;
    MarkerSet set;
    long updperiod;
    boolean use3d;
    String infowindow;
    AreaStyle defstyle;
    Map<String, AreaStyle> cusstyle;
    Map<String, AreaStyle> nationstyle;
    Set<String> visible;
    Set<String> hidden;
    boolean show_shops;
    boolean show_arenas;
    boolean show_embassies;
    boolean show_wilds;
    boolean stop;
    boolean using_townychat = false;
    boolean chat_sendlogin;
    boolean chat_sendquit;
    String chatformat;
    private final TaskScheduler scheduler;

    public DynmapTownyPlugin() {
        this.scheduler = isFoliaClassPresent() ? new FoliaTaskScheduler(this) : new BukkitTaskScheduler(this);
    }
    
    @Override
    public void onLoad() {
        log = this.getLogger();
    }
    
    private static class AreaStyle {
        int strokecolor;
        double strokeopacity;
        int strokeweight;
        int fillcolor;
        double fillopacity;
        int fillcolor_shops;
        int fillcolor_embassies;
        int fillcolor_arenas;
        int fillcolor_wilds;
        String homemarker;
        String capitalmarker;
        String outpostmarker;
        MarkerIcon homeicon;
        MarkerIcon capitalicon;
        MarkerIcon ruinicon;
        MarkerIcon outposticon;
        int yc;
        boolean boost;

        AreaStyle(FileConfiguration cfg, String path, MarkerAPI markerapi) {
            String sc = cfg.getString(path+".strokeColor", null);
            strokeopacity = cfg.getDouble(path+".strokeOpacity", -1);
            strokeweight = cfg.getInt(path+".strokeWeight", -1);
            String fc = cfg.getString(path+".fillColor", null);
            String fcs = cfg.getString(path+".fillColorShops", null);
            String fca = cfg.getString(path+".fillColorArenas", null);
            String fce = cfg.getString(path+".fillColorEmbassies", null);
            String fcw = cfg.getString(path+".fillColorWilds", null);
            yc = cfg.getInt(path+".y", -1);
            boost = cfg.getBoolean(path+".boost", false);
            
            strokecolor = -1;
            fillcolor = -1;
            fillcolor_shops = -1;
            fillcolor_arenas = -1;
            fillcolor_embassies = -1;
            fillcolor_wilds = -1;
            try {
            	if(sc != null)
            		strokecolor = Integer.parseInt(sc.substring(1), 16);
            	if(fc != null)
            		fillcolor = Integer.parseInt(fc.substring(1), 16);
                if(fcs != null)
                    fillcolor_shops = Integer.parseInt(fcs.substring(1), 16);
                if(fca != null)
                    fillcolor_arenas = Integer.parseInt(fca.substring(1), 16);
                if(fce != null)
                    fillcolor_embassies = Integer.parseInt(fce.substring(1), 16);
                if(fcw != null)
                    fillcolor_wilds = Integer.parseInt(fcw.substring(1), 16);
            } catch (NumberFormatException nfx) {
            }

            fillopacity = cfg.getDouble(path+".fillOpacity", -1);
            homemarker = cfg.getString(path+".homeicon", null);
            if(homemarker != null) {
                homeicon = markerapi.getMarkerIcon(homemarker);
                if(homeicon == null) {
                    severe("Invalid homeicon: " + homemarker);
                    homeicon = markerapi.getMarkerIcon("blueicon");
                }
            }
            capitalmarker = cfg.getString(path+".capitalicon", null);
            if(capitalmarker != null) {
                capitalicon = markerapi.getMarkerIcon(capitalmarker);
                if(capitalicon == null) {
                    severe("Invalid capitalicon: " + capitalmarker);
                    capitalicon = markerapi.getMarkerIcon("king");
                }
            }
            ruinicon = markerapi.getMarkerIcon("warning");

			outpostmarker = cfg.getString(path + ".outposticon", "tower");
			outposticon = markerapi.getMarkerIcon(outpostmarker);
			if (outposticon == null)
				outposticon = markerapi.getMarkerIcon("tower");
        }
        
        public int getStrokeColor(AreaStyle cust, AreaStyle nat) {
        	if((cust != null) && (cust.strokecolor >= 0))
        		return cust.strokecolor;
        	else if((nat != null) && (nat.strokecolor >= 0))
        		return nat.strokecolor;
        	else if(strokecolor >= 0)
        		return strokecolor;
        	else
        		return 0xFF0000;
        }
        public double getStrokeOpacity(AreaStyle cust, AreaStyle nat) {
        	if((cust != null) && (cust.strokeopacity >= 0))
        		return cust.strokeopacity;
        	else if((nat != null) && (nat.strokeopacity >= 0))
        		return nat.strokeopacity;
        	else if(strokeopacity >= 0)
        		return strokeopacity;
        	else
        		return 0.8;
        }
        public int getStrokeWeight(AreaStyle cust, AreaStyle nat) {
        	if((cust != null) && (cust.strokeweight >= 0))
        		return cust.strokeweight;
        	else if((nat != null) && (nat.strokeweight >= 0))
        		return nat.strokeweight;
        	else if(strokeweight >= 0)
        		return strokeweight;
        	else
        		return 3;
        }
        public int getFillColor(TownBlockType btype) {
        	if (btype == TownBlockType.COMMERCIAL)
        		return fillcolor_shops;
        	if (btype == TownBlockType.ARENA) 
        		return fillcolor_arenas;
        	if (btype == TownBlockType.EMBASSY) 
        		return fillcolor_embassies;
        	if (btype == TownBlockType.WILDS) 
        		return fillcolor_wilds;
        	return -1;
        }
        public int getFillColor(AreaStyle cust, AreaStyle nat, TownBlockType btype) {
            if(btype == TownBlockType.COMMERCIAL) {
                if((cust != null) && (cust.fillcolor_shops >= 0))
                    return cust.fillcolor_shops;
                else if((nat != null) && (nat.fillcolor_shops >= 0))
                    return nat.fillcolor_shops;
                else if(fillcolor_shops >= 0)
                    return fillcolor_shops;
                else
                    return 0xFF0000;
            }
            else if(btype == TownBlockType.ARENA) {
                if((cust != null) && (cust.fillcolor_arenas >= 0))
                    return cust.fillcolor_shops;
                else if((nat != null) && (nat.fillcolor_arenas >= 0))
                    return nat.fillcolor_arenas;
                else if(fillcolor_arenas >= 0)
                    return fillcolor_arenas;
                else
                    return 0xFF0000;
            }
            else if(btype == TownBlockType.EMBASSY) {
                if((cust != null) && (cust.fillcolor_embassies >= 0))
                    return cust.fillcolor_embassies;
                else if((nat != null) && (nat.fillcolor_embassies >= 0))
                    return nat.fillcolor_embassies;
                else if(fillcolor_embassies >= 0)
                    return fillcolor_embassies;
                else
                    return 0xFF0000;
            }
            else if(btype == TownBlockType.WILDS) {
                if((cust != null) && (cust.fillcolor_wilds >= 0))
                    return cust.fillcolor_wilds;
                else if((nat != null) && (nat.fillcolor_wilds >= 0))
                    return nat.fillcolor_wilds;
                else if(fillcolor_wilds >= 0)
                    return fillcolor_wilds;
                else
                    return 0xFF0000;
            }
        	if((cust != null) && (cust.fillcolor >= 0))
        		return cust.fillcolor;
        	else if((nat != null) && (nat.fillcolor >= 0))
        		return nat.fillcolor;
        	else if(fillcolor >= 0)
        		return fillcolor;
        	else
        		return 0xFF0000;
        }
        public double getFillOpacity(AreaStyle cust, AreaStyle nat) {
        	if((cust != null) && (cust.fillopacity >= 0))
        		return cust.fillopacity;
        	else if((nat != null) && (nat.fillopacity >= 0))
        		return nat.fillopacity;
        	else if(fillopacity >= 0)
        		return fillopacity;
        	else
        		return 0.35;
        }
        public MarkerIcon getHomeMarker(AreaStyle cust, AreaStyle nat) {
        	if((cust != null) && (cust.homeicon != null))
        		return cust.homeicon;
        	else if((nat != null) && (nat.homeicon != null))
        		return nat.homeicon;
        	else
        		return homeicon;
        }
        public MarkerIcon getCapitalMarker(AreaStyle cust, AreaStyle nat) {
        	if((cust != null) && (cust.capitalicon != null))
        		return cust.capitalicon;
        	else if((nat != null) && (nat.capitalicon != null))
        		return nat.capitalicon;
        	else if(capitalicon != null)
        		return capitalicon;
        	else
        		return getHomeMarker(cust, nat);
        }
        public int getY(AreaStyle cust, AreaStyle nat) {
            if((cust != null) && (cust.yc >= 0))
                return cust.yc;
            else if((nat != null) && (nat.yc >= 0))
                return nat.yc;
            else if(yc >= 0)
                return yc;
            else
                return 64;
        }
        public boolean getBoost(AreaStyle cust, AreaStyle nat) {
            if((cust != null) && cust.boost)
                return cust.boost;
            else if((nat != null) && nat.boost)
                return nat.boost;
            else 
                return boost;
        }
    }
    
    public static void info(String msg) {
        log.log(Level.INFO, msg);
    }
    public static void severe(String msg) {
        log.log(Level.SEVERE, msg);
    }

    private class TownyUpdate implements Runnable {
        public void run() {
            if(!stop) {
            	if (TownyUniverse.getInstance().getDataSource() == null) {
            		stop = true;
            	} else {
	                updateTowns();
	                updateTownPlayerSets();
	                updateNationPlayerSets();
            	}
            }
        }
    }

    private void updateTown(Town town) {
        Set<String> plids = town.getResidents().stream().map(r -> r.getName()).collect(Collectors.toSet());
        String setid = "towny.town." + town.getName();
        PlayerSet set = markerapi.getPlayerSet(setid);  /* See if set exists */
        if(set == null) {
            set = markerapi.createPlayerSet(setid, true, plids, false);
            info("Added player visibility set '" + setid + "' for town " + town.getName());
            return;
        }
        set.setPlayers(plids);
    }

    private void updateTownPlayerSets() {
        if(playersbytown)
            TownyAPI.getInstance().getTowns().forEach(t -> updateTown(t));
    }

    private void updateNation(Nation nat) {
        Set<String> plids = nat.getResidents().stream().map(r -> r.getName()).collect(Collectors.toSet());
        String setid = "towny.nation." + nat.getName();
        PlayerSet set = markerapi.getPlayerSet(setid);  /* See if set exists */
        if(set == null) {
            set = markerapi.createPlayerSet(setid, true, plids, false);
            info("Added player visibility set '" + setid + "' for nation " + nat.getName());
            return;
        }
        set.setPlayers(plids);
    }

    private void updateNationPlayerSets() {
        if(playersbynation)
            TownyAPI.getInstance().getNations().forEach(n -> updateNation(n));
    }

    private Map<String, AreaMarker> resareas = new HashMap<String, AreaMarker>();
    private Map<String, Marker> resmark = new HashMap<String, Marker>();
    
    private String formatInfoWindow(Town town, TownBlockType btype) {
        String v = "<div class=\"regioninfo\">"+infowindow+"</div>";
        if(btype != null)
            v = v.replace("%regionname%", town.getName() + " (" + btype.toString() + ")");
        else
            v = v.replace("%regionname%", town.getName());
        v = v.replace("%playerowners%", town.hasMayor()?town.getMayor().getName():"");
        String[] residents = town.getResidents().stream().map(obj -> obj.getName()).collect(Collectors.toList()).toArray(new String[0]);
		if (residents.length > 34) {
			String[] entire = residents;
			residents = new String[35 + 1];
			System.arraycopy(entire, 0, residents, 0, 35);
			residents[35] = "and more...";
		}
		
        String res = String.join(", ", residents);
        v = v.replace("%playermembers%", res);
        String mgrs = "";
        for(Resident r : town.getRank("assistant")) {
            if(mgrs.length()>0) mgrs += ", ";
            mgrs += r.getName();
        }
        v = v.replace("%playermanagers%", res);

        String dispNames = "";
        for (Resident r: town.getResidents()) {
            if(dispNames.length()>0) dispNames += ", ";
            dispNames += r.isOnline() ? r.getPlayer().getDisplayName() : r.getFormattedName();
        }
        v = v.replace("%residentdisplaynames%", dispNames);

        v = v.replace("%residentcount%", town.getResidents().size() + "");
        v = v.replace("%founded%", town.getRegistered() != 0 ? TownyFormatter.registeredFormat.format(town.getRegistered()) : "Not set");
        v = v.replace("%board%", town.getBoard());
        v = v.replace("%towntrusted%", town.getTrustedResidents().isEmpty() ? "None"
                : StringMgmt.join(town.getTrustedResidents().stream().map(trustedRes-> trustedRes.getName()).collect(Collectors.toList()), ", "));

        if (TownySettings.isUsingEconomy() && TownyEconomyHandler.isActive()) {
	        if (town.isTaxPercentage()) {
	            v = v.replace("%tax%", town.getTaxes() + "%");
	        } else {
	            v = v.replace("%tax%", TownyEconomyHandler.getFormattedBalance(town.getTaxes()));
	        }
	
	       	v = v.replace("%bank%", TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()));
            v = v.replace("%upkeep%", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeepCost(town) + TownySettings.getTownPenaltyUpkeepCost(town)));
        }
        String nation = town.hasNation() ? town.getNationOrNull().getName() : "";

        v = v.replace("%nation%", nation);

		String natStatus = "";
        if (town.isCapital()) {
            natStatus = "Capital of " + nation;
        } else if (town.hasNation()) {
            natStatus = "Member of " + nation;
        }

        v = v.replace("%nationstatus%", natStatus);

        v = v.replace("%public%", town.isPublic() ? "true" : "false");
        v = v.replace("%peaceful%", town.isNeutral() ? "true" : "false");
        
        
        /* Build flags */
        List<String> flags = new ArrayList<>();
        flags.add("Has Upkeep: " + town.hasUpkeep());
        flags.add("pvp: " + town.isPVP());
        flags.add("mobs: " + town.hasMobs());
        flags.add("explosion: " + town.isExplosion());
        flags.add("fire: " + town.isFire());
        flags.add("nation: " + nation);

        if (TownySettings.getBoolean(ConfigNodes.TOWN_RUINING_TOWN_RUINS_ENABLED)) {
        	String ruinedString = "ruined: " + town.isRuined(); 
            if (town.isRuined())
            	ruinedString += " (Time left: " + (TownySettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town)) + " hours)";

           	flags.add(ruinedString);
        }

        BuildTownFlagsEvent buildTownFlagsEvent = new BuildTownFlagsEvent(town, flags);
        Bukkit.getPluginManager().callEvent(buildTownFlagsEvent);

        v = v.replace("%flags%", String.join("<br/>", buildTownFlagsEvent.getFlags()));

        return v;
    }
    
    private boolean isVisible(String id, String worldname) {
        if((visible != null) && (visible.size() > 0)) {
            if((visible.contains(id) == false) && (visible.contains("world:" + worldname) == false)) {
                return false;
            }
        }
        if((hidden != null) && (hidden.size() > 0)) {
            if(hidden.contains(id) || hidden.contains("world:" + worldname))
                return false;
        }
        return true;
    }
        
    private TownStyle addStyle(Town town, AreaMarker m, TownBlockType btype) {
        AreaStyle as = cusstyle.get(town.getName());	/* Look up custom style for town, if any */
        AreaStyle ns = nationstyle.get(getNationNameOrNone(town));	/* Look up nation style, if any */

        double y = defstyle.getY(as, ns);
        m.setRangeY(y, y);
        m.setBoostFlag(defstyle.getBoost(as, ns));

        TownStyle style = new TownStyle(0, 0, 0, 0, 0);

        //Read dynamic colors from town/nation objects
        if(dynamicTownColorsEnabled || dynamicNationColorsEnabled) {
            try {
                String colorHexCode; 
                Integer townFillColorInteger = null;
                Integer townBorderColorInteger = null;

                //CALCULATE FILL COLOUR
                if (dynamicTownColorsEnabled) {
                    //Here we know the server is using town colors. This takes top priority for fill
                    colorHexCode = town.getMapColorHexCode();              
                    if(!colorHexCode.isEmpty()) {
                        //If town has a color, use it
                        townFillColorInteger = Integer.parseInt(colorHexCode, 16);
                        townBorderColorInteger = townFillColorInteger;
                    }
                } else {
                    //Here we know the server is using nation colors
                    colorHexCode = town.getNationMapColorHexCode();              
                    if(colorHexCode != null && !colorHexCode.isEmpty()) {
                        //If nation has a color, use it
                        townFillColorInteger = Integer.parseInt(colorHexCode, 16);                
                    }                            
                }

                //CALCULATE BORDER COLOR
                if (dynamicNationColorsEnabled) {
                    //Here we know the server is using nation colors. This takes top priority for border
                    colorHexCode = town.getNationMapColorHexCode();              
                    if(colorHexCode != null && !colorHexCode.isEmpty()) {
                        //If nation has a color, use it
                        townBorderColorInteger = Integer.parseInt(colorHexCode, 16);
                    }                                
                } else {
                    //Here we know the server is using town colors
                    colorHexCode = town.getMapColorHexCode();              
                    if(!colorHexCode.isEmpty()) {
                        //If town has a color, use it
                        townBorderColorInteger = Integer.parseInt(colorHexCode, 16);                
                    }                
                }

                //SET FILL COLOR
                if(townFillColorInteger != null) {
                    //Set fill style
                    double fillOpacity = m.getFillOpacity();
                    //Allow special fills for some townblock types
                    int townblockcolor = defstyle.getFillColor(btype);
                    style.setFill(fillOpacity, townblockcolor >= 0 ? townblockcolor : townFillColorInteger);
                } else {
                    style.setFill(defstyle.getFillOpacity(as, ns), defstyle.getFillColor(as, ns, btype));
                }

                //SET BORDER COLOR
                if(townBorderColorInteger != null) {
                    //Set stroke style
                    double strokeOpacity = m.getLineOpacity();
                    int strokeWeight = m.getLineWeight();
                    style.setBorder(strokeOpacity, strokeWeight, townBorderColorInteger);
                } else {
                    if(btype == null) {
                        style.setBorder(defstyle.getStrokeOpacity(as, ns), defstyle.getStrokeWeight(as, ns), defstyle.getStrokeColor(as, ns));
                    }
                    else {
                        style.setBorder(1, 0, 0);
                    }
                }

            } catch (Exception ex) {}
        }
        return style;
    }

	private MarkerIcon getMarkerIcon(Town town) {
        if (town.isRuined())
        	return defstyle.ruinicon;

        AreaStyle as = cusstyle.get(town.getName());
        AreaStyle ns = nationstyle.get(getNationNameOrNone(town));

        return town.isCapital() ? defstyle.getCapitalMarker(as, ns) : defstyle.getHomeMarker(as, ns);
    }

	private String getNationNameOrNone(Town town) {
		return town.hasNation() ? town.getNationOrNull().getName() : "_none_";
	}

    enum direction { XPLUS, ZPLUS, XMINUS, ZMINUS };
        
    /**
     * Find all contiguous blocks, set in target and clear in source
     */
    private int floodFillTarget(TileFlags src, TileFlags dest, int x, int y) {
        int cnt = 0;
        ArrayDeque<int[]> stack = new ArrayDeque<int[]>();
        stack.push(new int[] { x, y });
        
        while(stack.isEmpty() == false) {
            int[] nxt = stack.pop();
            x = nxt[0];
            y = nxt[1];
            if(src.getFlag(x, y)) { /* Set in src */
                src.setFlag(x, y, false);   /* Clear source */
                dest.setFlag(x, y, true);   /* Set in destination */
                cnt++;
                if(src.getFlag(x+1, y))
                    stack.push(new int[] { x+1, y });
                if(src.getFlag(x-1, y))
                    stack.push(new int[] { x-1, y });
                if(src.getFlag(x, y+1))
                    stack.push(new int[] { x, y+1 });
                if(src.getFlag(x, y-1))
                    stack.push(new int[] { x, y-1 });
            }
        }
        return cnt;
    }
    
    /* Handle specific town */
    private void handleTown(Town town, Map<String, AreaMarker> newmap, Map<String, Marker> newmark, TownBlockType btype) {
        String name = town.getName();
        double[] x = null;
        double[] z = null;
        int poly_index = 0; /* Index of polygon for given town */
                
        /* Handle areas */
    	Collection<TownBlock> blocks = town.getTownBlocks();
    	if(blocks.isEmpty())
    	    return;
        /* Build popup */
        BuildTownMarkerDescriptionEvent event = new BuildTownMarkerDescriptionEvent(town, formatInfoWindow(town, btype));
        Bukkit.getPluginManager().callEvent(event);
        String desc = event.getDescription();

    	HashMap<String, TileFlags> blkmaps = new HashMap<String, TileFlags>();
        LinkedList<TownBlock> nodevals = new LinkedList<TownBlock>();
        TownyWorld curworld = null;
        TileFlags curblks = null;
        boolean vis = false;
    	/* Loop through blocks: set flags on blockmaps for worlds */
    	for(TownBlock b : blocks) {
    	    /* If we're scanning for specific type, and this isn't it, skip */
    	    if((btype != null) && (b.getType() != btype)) {
    	        continue;
    	    }
    	    if(b.getWorld() != curworld) { /* Not same world */
    	        String wname = b.getWorld().getName();
    	        vis = isVisible(name, wname);  /* See if visible */
    	        if(vis) {  /* Only accumulate for visible areas */
    	            curblks = blkmaps.get(wname);  /* Find existing */
    	            if(curblks == null) {
    	                curblks = new TileFlags();
    	                blkmaps.put(wname, curblks);   /* Add fresh one */
    	            }
    	        }
    	        curworld = b.getWorld();
    	    }
    	    if(vis) {
    	        curblks.setFlag(b.getX(), b.getZ(), true); /* Set flag for block */
    	        nodevals.addLast(b);
    	    }
    	}
        /* Loop through until we don't find more areas */
        while(nodevals != null) {
            LinkedList<TownBlock> ournodes = null;
            LinkedList<TownBlock> newlist = null;
            TileFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(TownBlock node : nodevals) {
                int nodex = node.getX();
                int nodez = node.getZ();
                if(ourblks == null) {   /* If not started, switch to world for this block first */
                    if(node.getWorld() != curworld) {
                        curworld = node.getWorld();
                        curblks = blkmaps.get(curworld.getName());
                    }
                }
                /* If we need to start shape, and this block is not part of one yet */
                if((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new TileFlags();  /* Create map for shape */
                    ournodes = new LinkedList<TownBlock>();
                    floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex; minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((ourblks != null) && (node.getWorld() == curworld) &&
                    (ourblks.getFlag(nodex, nodez))) {
                    ournodes.add(node);
                    if(nodex < minx) {
                        minx = nodex; minz = nodez;
                    }
                    else if((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(newlist == null) newlist = new LinkedList<TownBlock>();
                    newlist.add(node);
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */
            if(ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int init_x = minx;
                int init_z = minz;
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<int[]>();
                linelist.add(new int[] { init_x, init_z } ); // Add start point
                while((cur_x != init_x) || (cur_z != init_z) || (dir != direction.ZMINUS)) {
                    switch(dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if(!ourblks.getFlag(cur_x+1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z-1)) {  /* Straight? */
                                cur_x++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++; cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if(!ourblks.getFlag(cur_x, cur_z+1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z+1)) {  /* Straight? */
                                cur_z++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++; cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if(!ourblks.getFlag(cur_x-1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z+1)) {  /* Straight? */
                                cur_x--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--; cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if(!ourblks.getFlag(cur_x, cur_z-1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z-1)) {  /* Straight? */
                                cur_z--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--; cur_z--;
                            }
                            break;
                    }
                }
                /* Build information for specific area */
                String polyid = town.getName() + "__" + poly_index;
                if(btype != null) {
                    polyid += "_" + btype;
                }
                int sz = linelist.size();
                x = new double[sz];
                z = new double[sz];
                for(int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double)line[0] * (double)townblocksize;
                    z[i] = (double)line[1] * (double)townblocksize;
                }
                /* Find existing one */
                AreaMarker m = resareas.get(polyid); /* Existing area? */
                if(m == null) {
                    m = set.createAreaMarker(polyid, name, false, curworld.getName(), x, z, false);
                    if(m == null) {
                        info("error adding area marker " + polyid);
                        return;
                    }
                }
                else {
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(name);   /* Update label */
                }
                /* Set popup */
                m.setDescription(desc);
                /* Set line and fill properties */
                TownStyle style = addStyle(town, m, btype);
                TownStyleEvent styleEvent = new TownStyleEvent(town, style);
                styleEvent.callEvent();
                style = styleEvent.getStyle();

                m.setLineStyle(style.getBorderWeight(), style.getBorderOpacity(), style.getBorderColor());
                m.setFillStyle(style.getFillOpacity(), style.getFillColor());

                /* Fire an event allowing other plugins to alter the AreaMarker */
                TownRenderEvent renderEvent = new TownRenderEvent(town, m); 
                Bukkit.getPluginManager().callEvent(renderEvent);

                /* Add to map */
                newmap.put(polyid, renderEvent.getAreaMarker());
                poly_index++;
            }
        }
        if(btype == null) {
            /* Now, add marker for home block */
            TownBlock blk = town.getHomeBlockOrNull();
            if((blk != null) && isVisible(name, blk.getWorld().getName())) {
                String markid = town.getName() + "__home";
                MarkerIcon ico = getMarkerIcon(town);

                /* Fire an event allowing other plugins to alter the MarkerIcon */
                TownSetMarkerIconEvent iconEvent = new TownSetMarkerIconEvent(town, ico);
                Bukkit.getPluginManager().callEvent(iconEvent);
                ico = iconEvent.getIcon();

                if(ico != null) {
                    Marker home = resmark.get(markid);
                    double xx = townblocksize*blk.getX() + (townblocksize/2);
                    double zz = townblocksize*blk.getZ() + (townblocksize/2);
                    if(home == null) {
                        home = set.createMarker(markid, name, blk.getWorld().getName(),
                                xx, 64, zz, ico, false);
                        if(home == null)
                            return;
                    }
                    else {
                        home.setLocation(blk.getWorld().getName(), xx, 64, zz);
                        home.setLabel(name);   /* Update label */
                        home.setMarkerIcon(ico);
                    }
                    /* Set popup */
                    home.setDescription(desc);

                    newmark.put(markid, home);
                }

				if (town.hasOutpostSpawn()) {
					MarkerIcon outpostIco = defstyle.outposticon;
					int i = 0;
					for (Location loc : town.getAllOutpostSpawns()) {
						i++;
						TownBlock tblk = TownyAPI.getInstance().getTownBlock(loc);
						if (tblk == null)
							continue;

						double xx = townblocksize * tblk.getX() + (townblocksize / 2);
						double zz = townblocksize * tblk.getZ() + (townblocksize / 2);
						String outpostName = town.getName() + "_Outpost_" + i;
						String outpostMarkerID = outpostName;
						Marker outpostMarker = resmark.get(outpostMarkerID);
						if (outpostMarker == null) {
							outpostMarker = set.createMarker(outpostMarkerID, outpostName, tblk.getWorld().getName(), xx, 64, zz, outpostIco, true);
							if (outpostMarker == null)
								continue;
						} else {
							outpostMarker.setLocation(tblk.getWorld().getName(), xx, 64, zz);
							outpostMarker.setLabel(outpostName);
							outpostMarker.setMarkerIcon(outpostIco);
						}
						outpostMarker.setDescription(tblk.getName() != null ? tblk.getName() : outpostName);
						newmark.put(outpostMarkerID, outpostMarker);
					}
				}
            }
        }
    }
    
    /* Update Towny information */
    private void updateTowns() {
        Map<String,AreaMarker> newmap = new HashMap<String,AreaMarker>(); /* Build new map */
        Map<String,Marker> newmark = new HashMap<String,Marker>(); /* Build new map */
        
        /* Loop through towns */
        for(Town t : TownyAPI.getInstance().getTowns()) {
    		handleTown(t, newmap, newmark, null);
    		if(show_shops) {
                handleTown(t, newmap, newmark, TownBlockType.COMMERCIAL);
    		}
            if(show_arenas) {
                handleTown(t, newmap, newmark, TownBlockType.ARENA);
            }
            if(show_embassies) {
                handleTown(t, newmap, newmark, TownBlockType.EMBASSY);
            }
            if(show_wilds) {
                handleTown(t, newmap, newmark, TownBlockType.WILDS);
            }
        }
        /* Now, review old map - anything not in the new map is gone */
        for (Map.Entry<String, AreaMarker> entries: resareas.entrySet()) {
            if (!newmap.containsKey(entries.getKey())) {
                entries.getValue().deleteMarker();;
            }
        }
        for (Map.Entry<String, Marker> entries: resmark.entrySet()) {
            if (!newmark.containsKey(entries.getKey())) {
                entries.getValue().deleteMarker();;
            }
        }
        /* And replace with new map */
        resareas = newmap;
        resmark = newmark;
                
    }
    
    private class OurServerListener implements Listener {

        @EventHandler(priority = EventPriority.MONITOR)
        public void onWebchatEvent(DynmapWebChatEvent event) {
            if(using_townychat && !chatformat.isEmpty()) {
                if(!event.isCancelled() && !event.isProcessed()) {
                    event.setProcessed();
                    String msg = chatformat.replace("&color;", "\u00A7").replace("%playername%", event.getName()).replace("%message%", event.getMessage());
                    getServer().broadcastMessage(msg);
                }
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerLogin(PlayerLoginEvent event) {
            if(chat_sendlogin && using_townychat && event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
                Player player = event.getPlayer();
                api.postPlayerJoinQuitToWeb(player, true);
            }
        }

        @EventHandler(priority = EventPriority.MONITOR)
        public void onPlayerQuit(PlayerQuitEvent event) {
            if(chat_sendquit && using_townychat) {
                Player player = event.getPlayer();
                api.postPlayerJoinQuitToWeb(player, false);
            }
        }

        @EventHandler(priority=EventPriority.NORMAL)
        private void onDynMapReload (PluginEnableEvent event) {
        	if (event.getPlugin().getName().equals("dynmap")) {
                PluginManager pluginManager = getServer().getPluginManager();
                if (pluginManager.isPluginEnabled("Dynmap-Towny")) {                	
	                pluginManager.disablePlugin(DynmapTownyPlugin.this);
	                pluginManager.enablePlugin(DynmapTownyPlugin.this);
                }
        	}
        	
        }
    }
    
    public void onEnable() {
        info("initializing");
        PluginManager pm = getServer().getPluginManager();
        /* Get dynmap */
        dynmap = pm.getPlugin("dynmap");
        if(dynmap == null) {
            severe("Cannot find dynmap!");
            return;
        }
        api = (DynmapAPI)dynmap; /* Get API */
        /* Get Towny */
        Plugin p = pm.getPlugin("Towny");
        if(p == null) {
            severe("Cannot find Towny!");
            return;
        }
        Towny towny = (Towny)p;
        
        p = pm.getPlugin("TownyChat");
        if(p != null) {
            townychat = (Chat)p;
        }
        
		if (Version.fromString(towny.getDescription().getVersion()).compareTo(requiredTownyVersion) < 0) {
			getLogger().severe("Towny version does not meet required minimum version: " + requiredTownyVersion.toString());
			this.getServer().getPluginManager().disablePlugin(this);
			return;
		} else {
			getLogger().info("Towny version " + towny.getDescription().getVersion() + " found.");
		}
        

        getServer().getPluginManager().registerEvents(new OurServerListener(), this);
        /* If both enabled, activate */
        if(dynmap.isEnabled() && towny.isEnabled()) {
            activate();
            prepForChat();
        }
    }
    
    private void prepForChat() {
        /* If townychat is active, and we've found dynmap API */
        if((townychat != null) && townychat.isEnabled() && dynmap.isEnabled()) {
            api.setDisableChatToWebProcessing(true);
            using_townychat = true;
            info("TownyChat detect: disabling normal chat-to-web processing in Dynmap");
        }
    }
    
    private void activate() {
        markerapi = api.getMarkerAPI();
        if(markerapi == null) {
            severe("Error loading dynmap marker API!");
            return;
        }

        townblocksize = Coord.getCellSize();

        /* Load configuration */
        if(reload) {
            reloadConfig();
            if(set != null) {
                set.deleteMarkerSet();
                set = null;
            }
        }
        else {
            reload = true;
        }
        FileConfiguration cfg = getConfig();
        cfg.options().copyDefaults(true);   /* Load defaults, if needed */
        this.saveConfig();  /* Save updates, if needed */

        /* Now, add marker set for mobs (make it transient) */
        set = markerapi.getMarkerSet("towny.markerset");
        if(set == null)
            set = markerapi.createMarkerSet("towny.markerset", cfg.getString("layer.name", "Towny"), null, false);
        else
            set.setMarkerSetLabel(cfg.getString("layer.name", "Towny"));
        if(set == null) {
            severe("Error creating marker set");
            return;
        }
        int minzoom = cfg.getInt("layer.minzoom", 0);
        if(minzoom > 0)
            set.setMinZoom(minzoom);
        set.setLayerPriority(cfg.getInt("layer.layerprio", 10));
        set.setHideByDefault(cfg.getBoolean("layer.hidebydefault", false));
        use3d = cfg.getBoolean("use3dregions", false);
        infowindow = cfg.getString("infowindow", DEF_INFOWINDOW);
        /* See if we need to show commercial areas */
        show_shops = cfg.getBoolean("layer.showShops", false);
        show_arenas = cfg.getBoolean("layer.showArenas", false);
        show_embassies = cfg.getBoolean("layer.showEmbassies", false);
        show_wilds = cfg.getBoolean("layer.showWilds", false);

        /* Get style information */
        defstyle = new AreaStyle(cfg, "regionstyle", markerapi);
        cusstyle = new HashMap<String, AreaStyle>();
        nationstyle = new HashMap<String, AreaStyle>();
        ConfigurationSection sect = cfg.getConfigurationSection("custstyle");
        if(sect != null) {
            Set<String> ids = sect.getKeys(false);
            
            for(String id : ids) {
                cusstyle.put(id, new AreaStyle(cfg, "custstyle." + id, markerapi));
            }
        }
        sect = cfg.getConfigurationSection("nationstyle");
        if(sect != null) {
            Set<String> ids = sect.getKeys(false);
            
            for(String id : ids) {
                nationstyle.put(id, new AreaStyle(cfg, "nationstyle." + id, markerapi));
            }
        }
        List<String> vis = cfg.getStringList("visibleregions");
        if(vis != null) {
            visible = new HashSet<String>(vis);
        }
        List<String> hid = cfg.getStringList("hiddenregions");
        if(hid != null) {
            hidden = new HashSet<String>(hid);
        }

        chat_sendlogin = cfg.getBoolean("chat.sendlogin", true);
        chat_sendquit = cfg.getBoolean("chat.sendquit", true);
        chatformat = cfg.getString("chat.format", "&color;2[WEB] %playername%: &color;f%message%");

        /* Check if player sets enabled */
        playersbytown = cfg.getBoolean("visibility-by-town", false);
        if(playersbytown) {
            try {
                if(!api.testIfPlayerInfoProtected()) {
                    playersbytown = false;
                    info("Dynmap does not have player-info-protected enabled - visibility-by-town will have no effect");
                }
            } catch (NoSuchMethodError x) {
                playersbytown = false;
                info("Dynmap does not support function needed for 'visibility-by-town' - need to upgrade to 0.60 or later");
            }
        }
        playersbynation = cfg.getBoolean("visibility-by-nation", false);
        if(playersbynation) {
            try {
                if(!api.testIfPlayerInfoProtected()) {
                    playersbynation = false;
                    info("Dynmap does not have player-info-protected enabled - visibility-by-nation will have no effect");
                }
            } catch (NoSuchMethodError x) {
                playersbynation = false;
                info("Dynmap does not support function needed for 'visibility-by-nation' - need to upgrade to 0.60 or later");
            }
        }

        dynamicNationColorsEnabled = cfg.getBoolean("dynamic-nation-colors", true);
        dynamicTownColorsEnabled = cfg.getBoolean("dynamic-town-colors", true);

        /* Set up update job - based on periond */
        int per = Math.max(15, cfg.getInt("update.period", 300));
        updperiod = (per*20L);
        stop = false;

        scheduler.runAsyncRepeating(new TownyUpdate(), 40, per);
        
        info("version " + this.getDescription().getVersion() + " is activated");
    }

    public void onDisable() {
        if(set != null) {
            set.deleteMarkerSet();
            set = null;
        }
        resareas.clear();
        stop = true;
    }

    private static boolean isFoliaClassPresent() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
}
