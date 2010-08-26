package net.sf.freecol.util.test;

import java.util.HashMap;
import java.util.Iterator;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.common.model.Unit.UnitState;

public class FreeColTestUtils {
    
    private static ColonyBuilder colonyBuilder = null;
    
    public static ColonyBuilder getColonyBuilder(){
        Game game = FreeColTestCase.getGame();
        if(game == null){
            throw new NullPointerException("Game not set");
        }
        if(colonyBuilder == null){
            colonyBuilder = new ColonyBuilder(game);
        }
        else{
            colonyBuilder = colonyBuilder.reset().setGame(game);
        }
        
        return colonyBuilder;
    }
    
    public static class ColonyBuilder{
        
        // Required parameter
        static final UnitType colonistType = FreeColTestCase.spec().getUnitType("model.unit.freeColonist");
        private Game game;
        
        private HashMap<UnitType,Integer> colonists = new HashMap<UnitType,Integer>();
        private Player player;
        private String name;
        private int initialColonists;
        private final String defaultPlayer = "model.nation.dutch";
        private String defaultName = "New Amsterdam";
        private int initialDefaultColonists = 1;
        private Tile colonyTile;
        
        
        private ColonyBuilder(Game game){
            this.game = game;
            setStartingParams();
        }
        
        private void setStartingParams(){
            // Some params can only be set in build(), because the default values 
            //may not be valid for the game set
            // However, the tester himself may set them to valid values later, 
            //so they are set to null for now
            player = null;
            colonyTile = null;
            name = defaultName; 
            initialColonists = initialDefaultColonists;
            colonists.clear();
        }
        
        public ColonyBuilder player(Player player){
            this.player = player;
            
            if(player == null || !game.getPlayers().contains(player)){
                throw new IllegalArgumentException("Player not in game");
            }
            
            return this;
        }
        
        public ColonyBuilder initialColonists(int colonists){
            if(colonists <= 0){
                throw new IllegalArgumentException("Number of colonists must be positive");
            }
            this.initialColonists = colonists;
            return this;
        }
        
        public ColonyBuilder colonyTile(Tile tile){
            Tile tileOnMap = this.game.getMap().getTile(tile.getPosition());
            if(tile != tileOnMap){
                throw new IllegalArgumentException("Given tile not on map");
            }
            this.colonyTile = tile;
            return this;
        }
        
        public ColonyBuilder colonyName(String name){
            if(name == null){
                throw new IllegalArgumentException("Name cannot be null");
            }
            this.name = name;
            return this;
        }
        
        public ColonyBuilder addColonist(UnitType type){
            if(!colonists.containsKey(type)){
                colonists.put(type, 0);
            }
            Integer nCol = colonists.get(type);
            colonists.put(type, nCol + 1);
            return this;
        }
        
        public Colony build(){
                        
            // player not set, get default
            if(player == null){
                player = game.getPlayer(defaultPlayer);
                if(player == null){
                    throw new IllegalArgumentException("Default Player " + defaultPlayer + " not in game");
                }
            }
            
            // settlement tile no set, get default
            if(colonyTile == null){
                colonyTile = game.getMap().getTile(5, 8);
                if(colonyTile == null){
                    throw new IllegalArgumentException("Default tile not in game");
                }
            }
            
            /*
            if(this.name != null){
                for(Colony colony : player.getColonies()){
                    if(colony.getName().equals(this.name)){
                        throw new IllegalArgumentException("Another colony already has the given name");
                    }
                }
            }
            */
            
            Colony colony = new Colony(game, player, name, colonyTile);
            colony.placeSettlement();

            
            // Add colonists
            int nCol = 0;
            Iterator<UnitType> iter = colonists.keySet().iterator();
            while(iter.hasNext()){
                UnitType type = iter.next();
                Integer n = colonists.get(type);
                for(int i=0; i < n; i++){
                    Unit colonist = new Unit(game, colony, player, type, UnitState.IN_COLONY,
                            colonistType.getDefaultEquipment());
                    colonist.setLocation(colony);
                    nCol++;
                }
            }
            // add rest of colonists as simple free colonists
            for(int i=nCol; i < initialColonists; i++){
                Unit colonist = new Unit(game, colony, player, colonistType, UnitState.IN_COLONY,
                        colonistType.getDefaultEquipment());
                colonist.setLocation(colony);
            }
            
            return colony;
        }
        
        public ColonyBuilder setGame(Game game){
            this.game = game;
            return reset();
        }
        
        public ColonyBuilder reset() {
            setStartingParams();
            
            return this;
        }
    }
    
}
