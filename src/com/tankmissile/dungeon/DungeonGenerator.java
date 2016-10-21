package com.tankmissile.dungeon;

import java.util.Random;
import java.util.Vector;

public class DungeonGenerator {

	final int MAP_WIDTH, MAP_HEIGHT;
	final int ROOM_MAXW = 6, ROOM_MAXH = 6;
	final int ROOM_MINW = 3, ROOM_MINH = 3;

	final int CORNER_WEIGHT = 5; //percent chance of generating a hallway intersection/corner
	final int ROOM_ATTEMPTS; //number of tries to create rooms - NOT the number of rooms in final maze!
	final int MAX_DOORS_PER_ROOM = 4;
	final int DOOR_WEIGHT = 25;//percent chance of generating a door at any given point

	final boolean allowRoomOverlap = false;

	Tile[][] map;
	Vector<Room> rooms;
	Vector<Room> deadEnds;
	Vector<Door> doors;

	public DungeonGenerator(int w, int h){
		MAP_WIDTH = w;
		MAP_HEIGHT = h;
		ROOM_ATTEMPTS = (int) Math.max(10, w*2);

		rooms = new Vector<Room>();
		deadEnds = new Vector<Room>();
		doors = new Vector<Door>();

		//initialize map
		map = new Tile[w][h];
		for(int j = 0; j < MAP_HEIGHT; j++){
			for(int i = 0; i < MAP_WIDTH; i++){
				map[i][j] = new Tile(i,j);
			}
		}

		//place rooms
		placeRooms();
		//System.out.println("\nPlaced Rooms...");
		//debugPrintMap();

		//fill halls
		placeHalls();
		//System.out.println("\nPlaced Halls...");
		//debugPrintMap();

		//connect everything
		placeDoors();
		//System.out.println("\nPlaced Doors...");
		//debugPrintMap();

		//remove dead ends
		removeDeadEnds();
		//System.out.println("\nRemoved Dead Ends...");
		//debugPrintMap();

		//make the halls less jagged
		//straightenHalls();
		//System.out.println("\nStraightened Halls...");
		//debugPrintMap();

		//remove extra walls
		removeExtraWalls();
		//System.out.println("\nRemoved Extra Walls...");
		//debugPrintMap();
	}

	//attempts to place ROOM_ATTEMPTS rooms within the map; will discard any overlapping rooms if allowOverlap is set to false
	void placeRooms(){
		int x, y, w, h;

		for(int i = 0; i < ROOM_ATTEMPTS; i++){
			x = (int)(Math.random() * (MAP_WIDTH-2)) + 1;
			y = (int)(Math.random() * (MAP_HEIGHT-2)) + 1;
			w = (int)(Math.random() * (ROOM_MAXW - ROOM_MINW)) + ROOM_MINW;
			h = (int)(Math.random() * (ROOM_MAXH - ROOM_MINH)) + ROOM_MINH;

			//System.out.println("Creating " + w + "x" + h + " room at " + x + "," + y);
			if(setRoom( x, y, w, h )){
				rooms.add(new Room(x, y, w, h));
			}
		}
	}

	//fills unoccupied space with hallways across the entire map
	void placeHalls(){
		for(int i = 1; i < MAP_WIDTH-1; i++){
			for(int j = 1; j < MAP_HEIGHT-1; j++){
				if(map[i][j].type == Tile.UNUSED){
					generateHallJagged(i,j);
					deadEnds.add(new Room(i,j,1,1));
				}
			}
		}
	}

	//puts at least one door connected to a hallway in every room
	void placeDoors(){
		for(Room c : rooms){

			int numDoors = 0;

			//try east edge
			if(numDoors < MAX_DOORS_PER_ROOM) numDoors += tryPlaceDoors(c, Tile.EAST, 1);

			//try north edge
			if(numDoors < MAX_DOORS_PER_ROOM) numDoors += tryPlaceDoors(c, Tile.NORTH, 1);

			//try south edge
			if(numDoors < MAX_DOORS_PER_ROOM) numDoors += tryPlaceDoors(c, Tile.SOUTH, 1);

			//try west edge
			if(numDoors < MAX_DOORS_PER_ROOM) numDoors += tryPlaceDoors(c, Tile.WEST, 1);
		}
	}

	//tries to place a door along Room c's wall as specified by side.  Returns true if a door was placed
	int tryPlaceDoors(Room c, int side, int numDoorsToPlace){
		if(numDoorsToPlace == 0) return 0;
		
		int x = c.x, y = c.y, hdir = 0, vdir = 0;

		switch(side){
		case Tile.NORTH:
			y = c.y-1;
			vdir = -1;
			break;
		case Tile.SOUTH:
			y = c.y+c.h;
			vdir = 1;
			break;
		case Tile.WEST:
			x = c.x-1;
			hdir = -1;
			break;
		case Tile.EAST:
			x = c.x+c.w;
			hdir = 1;
			break;
		default:
			return 0;
		}

		Vector<Door> possibleDoors = new Vector<Door>();

		//vertical
		if(y+vdir > 1 && y+vdir < MAP_HEIGHT-1){ //make sure it doesn't go out of bounds
			for( ; x < c.x + c.w; x++){
				if(!canPlaceDoor(x, y, side, vdir)) continue;
				possibleDoors.add(new Door(x,y, (vdir == -1) ? Tile.NORTH : Tile.SOUTH));
			}
		}
		//horizontal
		else if(x+hdir > 1 && x+hdir < MAP_WIDTH-1){ //make sure it doesn't go out of bounds
			for( ; y < c.y + c.h; y++){
				if(!canPlaceDoor(x, y, side, hdir)) continue;
				possibleDoors.add(new Door(x,y, hdir == -1 ? Tile.WEST : Tile.EAST));
			}
		}

		if(possibleDoors.size() == 0){
			return 0;
		}

		//actually add doors
		int where;
		int numPlaced = 0;
		while(numPlaced < numDoorsToPlace && possibleDoors.size() > 0){
			where = (int)Math.floor(Math.random() * possibleDoors.size());
			Door newDoor = possibleDoors.elementAt(where);
			
			//make sure we haven't made a door invalid
			if(!canPlaceDoor(newDoor.x, newDoor.y, side, (side == Tile.EAST || side == Tile.WEST) ? hdir : vdir)){
				possibleDoors.remove(newDoor);
				continue;
			}
			
			map[newDoor.x][newDoor.y] = newDoor;
			numPlaced++;
			possibleDoors.remove(newDoor);
		}

		return numPlaced;
	}

	boolean canPlaceDoor(int x, int y, int side, int dir){
		//make sure the door isn't being placed in the middle of a room or sideways next to a door/hallway
		if(side == Tile.EAST || side == Tile.WEST){
			if(map[x][y].type == Tile.ROOM || map[x][y].type == Tile.DOOR) return false;
			if( y-1 > 0 && (map[x][y-1].type == Tile.ROOM || map[x][y-1].type == Tile.DOOR)) return false;
			if( y+1 < MAP_HEIGHT && (map[x][y+1].type == Tile.ROOM || map[x][y+1].type == Tile.DOOR)) return false;
			
			if(map[x+dir][y].type == Tile.HALL || map[x+dir][y].type == Tile.ROOM) return true;
		}
		else {
			if(map[x][y].type == Tile.ROOM || map[x][y].type == Tile.DOOR) return false; //don't place a door on a room or door tile, silly!
			if( x-1 > 0 && (map[x-1][y].type == Tile.ROOM || map[x-1][y].type == Tile.DOOR || map[x-1][y].type == Tile.HALL)) return false; //don't place a door next to a room, hall or door tile, silly!
			if( x+1 < MAP_WIDTH && (map[x+1][y].type == Tile.ROOM || map[x+1][y].type == Tile.DOOR || map[x+1][y].type == Tile.HALL)) return false;
			
			if(map[x][y+dir].type == Tile.HALL || map[x][y+dir].type == Tile.ROOM) return true;
		}

		return false;
	}

	void removeDeadEnds(){
		for(Room r : deadEnds){
			killDeadEnd(r.x, r.y);
		}
	}

	//iterate over each door, find a path to another door or intersection and attempt to straighten it
	void straightenHalls(){
		for(Door d : doors){
			
		}
	}

	void removeExtraWalls(){
		for(int i = 0; i < MAP_WIDTH; i++){
			for(int j = 0; j < MAP_HEIGHT; j++){
				//check all sides to see if one is a hall/room/door, if so skip
				if(i > 0){
					if(map[i-1][j].type == Tile.HALL || map[i-1][j].type == Tile.ROOM || map[i-1][j].type == Tile.DOOR) continue;
					if(j > 0 && (map[i-1][j-1].type == Tile.HALL || map[i-1][j].type == Tile.ROOM || map[i-1][j].type == Tile.DOOR)) continue;
					if(j < MAP_HEIGHT-1 && (map[i-1][j].type == Tile.HALL || map[i-1][j].type == Tile.ROOM)) continue;
				}
				if(i < MAP_WIDTH -1){
					if(map[i+1][j].type == Tile.HALL || map[i+1][j].type == Tile.ROOM || map[i+1][j].type == Tile.DOOR) continue;
					if(j > 0 && (map[i+1][j-1].type == Tile.HALL || map[i+1][j].type == Tile.ROOM || map[i+1][j].type == Tile.DOOR)) continue;
					if(j < MAP_HEIGHT-1 && (map[i+1][j].type == Tile.HALL || map[i+1][j].type == Tile.ROOM || map[i+1][j].type == Tile.DOOR)) continue;
				}
				if(j > 0 && (map[i][j-1].type == Tile.HALL || map[i][j-1].type == Tile.ROOM || map[i][j-1].type == Tile.DOOR)) continue;
				if(j < MAP_HEIGHT-1 && (map[i][j+1].type == Tile.HALL || map[i][j+1].type == Tile.ROOM || map[i][j+1].type == Tile.DOOR)) continue;

				map[i][j].type = Tile.UNUSED;
			}
		}
	}

	void killDeadEnd(int x, int y){
		if(!isDeadEnd(x, y)) return;

		map[x][y].type = Tile.HWALL;
		killDeadEnd(x-1, y);
		killDeadEnd(x+1,y);
		killDeadEnd(x,y-1);
		killDeadEnd(x,y+1);

	}

	boolean isDeadEnd(int x, int y){
		if(map[x][y].type != Tile.HALL && map[x][y].type != Tile.DOOR) return false;

		int numAdj = 0;
		if(map[x-1][y].type == Tile.HALL || map[x-1][y].type == Tile.ROOM || map[x-1][y].type == Tile.DOOR) numAdj++;
		if(map[x+1][y].type == Tile.HALL || map[x+1][y].type == Tile.ROOM || map[x+1][y].type == Tile.DOOR) numAdj++;
		if(map[x][y-1].type == Tile.HALL || map[x][y-1].type == Tile.ROOM || map[x][y-1].type == Tile.DOOR) numAdj++;
		if(map[x][y+1].type == Tile.HALL || map[x][y+1].type == Tile.ROOM || map[x][y+1].type == Tile.DOOR) numAdj++;
		if(numAdj > 1) return false;

		return true;
	}

	boolean isIntersection(int x, int y){
		if(map[x][y].type != Tile.HALL && map[x][y].type != Tile.DOOR) return false;
		if(map[x][y].type == Tile.DOOR) return true; //all doors count as intersections

		int numAdj = 0;
		if(map[x-1][y].type == Tile.HALL || map[x-1][y].type == Tile.ROOM || map[x-1][y].type == Tile.DOOR) numAdj++;
		if(map[x+1][y].type == Tile.HALL || map[x+1][y].type == Tile.ROOM || map[x+1][y].type == Tile.DOOR) numAdj++;
		if(map[x][y-1].type == Tile.HALL || map[x][y-1].type == Tile.ROOM || map[x][y-1].type == Tile.DOOR) numAdj++;
		if(map[x][y+1].type == Tile.HALL || map[x][y+1].type == Tile.ROOM || map[x][y+1].type == Tile.DOOR) numAdj++;
		if(numAdj <= 2) return false;

		return true;
	}

	boolean generateHall(int x, int y){
		//if the hall is being generated on an occupied tile, end recursion
		if(map[x][y].type != Tile.UNUSED && map[x][y].type != Tile.HWALL){
			return false;
		}
		else {
			//set the tile as a hall; if it returned false then there was a problem
			if(!setHall(x, y)){
				return false;
			}

			int fromx = x, fromy = y;
			//find direction the hall came from
			if(map[x-1][y].type == Tile.HALL) fromx = x-1; //came from the west
			else if(map[x+1][y].type == Tile.HALL) fromx = x+1; //came from the east
			else if(map[x][y-1].type == Tile.HALL) fromy = y-1; //came from the north
			else if(map[x][y+1].type == Tile.HALL) fromy = y+1; //came from the south

			//generate a hall traveling in the same direction
			if(fromx == x-1) {
				generateHall(x+1,y); //generate to the east
			}
			else if(fromx == x+1) {
				generateHall(x-1,y); //generate to the west
			}
			else if(fromy == y-1) {
				generateHall(x,y-1); //generate to the north
			}
			else if(fromy == y+1) {
				generateHall(x,y+1); //generate to the south
			}
			else { //hallway is a root node, try generating in every direction
				generateHall(x-1,y);
				generateHall(x+1,y);
				generateHall(x,y-1);
				generateHall(x,y+1);
			}

			//try generating corners
			int numAdj = 1;
			if(Math.random()*100 < CORNER_WEIGHT){
				if(fromy != y){ //try east-west if hallway came from north/south
					if(Math.random() * 100 < 50){ //50% chance to generate a west hall
						generateHall(x-1,y);
						numAdj++;
					}
					else { //50% chance to generate an east hall
						generateHall(x+1,y);
						numAdj++;
					}
				}
				if(fromx != x){ //try north-south if hallway came from east/west
					if(Math.random() * 100 < 50){ //50% chance to generate a north hall
						generateHall(x,y-1);
						numAdj++;
					}
					else { //50% chance to generate a south hall
						generateHall(x,y+1);
						numAdj++;
					}
				}
			}
			if(numAdj > 1) deadEnds.add(new Room(x,y,1,1));
		}

		return true;
	}

	//simpler version that doesn't prioritize travel in the same direction (will probably be ugly)
	boolean generateHallJagged(int x, int y){
		if(x < 1 || x >= MAP_WIDTH-1 || y < 1 || y >= MAP_HEIGHT -1){
			return false;
		}

		//only generate on new tiles
		if(map[x][y].type != Tile.UNUSED && map[x][y].type != Tile.HWALL){
			return false;
		}

		//make sure there's only one adjacent hallway, if any
		int numAdj = 0;
		if(map[x-1][y].type == Tile.HALL) numAdj++;
		if(map[x+1][y].type == Tile.HALL) numAdj++;
		if(map[x][y-1].type == Tile.HALL) numAdj++;
		if(map[x][y+1].type == Tile.HALL) numAdj++;
		if(numAdj > 1) return false;

		setHall(x, y);

		//generate on every side
		numAdj = 1;
		double d = Math.random();
		if(d < .25){
			if(generateHallJagged(x-1,y)) numAdj++;
			if(generateHallJagged(x+1,y)) numAdj++;
			if(generateHallJagged(x,y-1)) numAdj++;
			if(generateHallJagged(x,y+1)) numAdj++;
		}
		else if (d < .5){
			if(generateHallJagged(x+1,y)) numAdj++;
			if(generateHallJagged(x,y-1)) numAdj++;
			if(generateHallJagged(x,y+1)) numAdj++;
			if(generateHallJagged(x-1,y)) numAdj++;
		}
		else if(d < .75){
			if(generateHallJagged(x,y-1)) numAdj++;
			if(generateHallJagged(x,y+1)) numAdj++;
			if(generateHallJagged(x-1,y)) numAdj++;
			if(generateHallJagged(x+1,y)) numAdj++;
		}
		else {
			if(generateHallJagged(x,y+1)) numAdj++;
			if(generateHallJagged(x-1,y)) numAdj++;
			if(generateHallJagged(x+1,y)) numAdj++;
			if(generateHallJagged(x,y-1)) numAdj++;
		}
		if(numAdj == 1) deadEnds.add(new Room(x,y,1,1)); //this will make finding dead ends easier later

		return true;
	}

	//sets a point as a hallway, and adds walls to all adjacent and diagonal tiles
	//returns true if successful, false otherwise
	boolean setHall(int x, int y){
		if(x < 1 || y < 1 || x >= MAP_WIDTH-1 || y >= MAP_HEIGHT-1 ) return false; //make sure it's within bounds, leaving room for walls

		//check which adjacent tiles are hallways
		boolean up = false, left = false, down = false, right = false;
		if(map[x-1][y].type == Tile.HALL) left = true;
		if(map[x+1][y].type == Tile.HALL) right = true;
		if(map[x][y-1].type == Tile.HALL) up = true;
		if(map[x][y+1].type == Tile.HALL) down = true;

		//if the hall is adjacent to more than one hall, reject
		if(up && left || up && right || down && left || down && right ) return false;

		if(map[x][y].type == Tile.UNUSED || map[x][y].type == Tile.HWALL){ //don't overwrite wall or room tiles; that would be bad
			map[x][y].type = Tile.HALL;
			for(int i = x-1; i <= x+1; i++){
				for(int j = y-1; j <= y+1; j++){
					if(map[i][j].type == Tile.UNUSED)
						map[i][j].type = Tile.HWALL;
				}
			}
		}

		return true;
	}

	//creates a room with width w and height h, starting at the top left corner (x,y)
	//returns true if set, false otherwise
	boolean setRoom(int x, int y, int w, int h){
		//check that the room is within bounds
		if(x < 1 || y < 1 || x+w+1 > MAP_WIDTH-1 || y+h+1 > MAP_HEIGHT-1){ //we want an outside edge for the walls
			return false;
		}

		//check that there's no overlap, if disabled
		if(!allowRoomOverlap){
			for(int j = y; j < y+h; j++){
				for(int i = x; i < x+w; i++){
					if(map[i][j].type != Tile.UNUSED) return false;
				}
			}
		}

		//System.out.println("placing " + w + "x" + h + " room at " + x + "," + y);

		for(int j = y-1; j <= y+h; j++){
			for(int i = x-1; i <= x+w; i++){
				if(map[i][j].type == Tile.UNUSED || map[i][j].type == Tile.WALL){
					if(!(i==x-1 || i == x+w || j == y-1 || j == y+h)){
						map[i][j].type = Tile.ROOM;
					}
					else {
						map[i][j].type = Tile.WALL;
					}
				}
			}
		}

		return true;
	}


	//prints an ASCII map to System.out
	public void debugPrintMap(){
		for(int j = 0; j < MAP_HEIGHT; j++){
			for(int i = 0; i < MAP_WIDTH; i++){
				System.out.print(map[i][j].toString());
			}
			System.out.print("\n"); //end the line
		}
	}

	public static void main(String[] args) {
		DungeonGenerator dg = new DungeonGenerator(15,20);

		dg.debugPrintMap();
	}

}

class Door extends Tile {
	int dir; //direction of hall (use Tile.NORTH /etc)
	
	public Door(int x, int y, int direction){
		super(x, y);
		dir = direction;
		type = Tile.DOOR;
	}
	
	public int getDir(){
		return dir;
	}
	public void setDir(int d){
		dir = d;
	}
}

//The tile it extends is the origin, this also stores a width and height (tiles within range are not stored) //TODO maybe I should store them?
class Room extends Tile{
	protected final int w, h;

	public Room(int a, int b, int c, int d){
		super(a,b);
		w = c;
		h = d;
	}
	
	public int getWidth(){
		return w;
	}
	public int getHeight(){
		return h;
	}
}
