package com.tankmissile.dungeon;

import java.util.Vector;

public class DungeonGenerator {
	
	final int MAP_WIDTH, MAP_HEIGHT;
	final int ROOM_MAXW = 6, ROOM_MAXH = 6;
	final int ROOM_MINW = 3, ROOM_MINH = 3;

	final int CORNER_WEIGHT = 5; //percent chance of generating a hallway intersection/corner
	final int ROOM_ATTEMPTS; //number of tries to create rooms - NOT the number of rooms in final maze!
	final int MAX_DOORS_PER_ROOM = 2;
	final int DOOR_WEIGHT = 25;//percent chance of generating a door at any given point

	final boolean allowRoomOverlap = false;

	public enum tileType { UNUSED, WALL, HALL, HWALL, ROOM, DOOR }; //HWALL is a hallway wall; not used for generating doors but otherwise functions the same

	tileType[][] map;
	Vector<Room> rooms;
	Vector<Room> deadEnds;
	Vector<Room> doors;

	public DungeonGenerator(int w, int h){
		MAP_WIDTH = w;
		MAP_HEIGHT = h;
		ROOM_ATTEMPTS = (int) Math.max(10, w*2);

		rooms = new Vector<Room>();
		deadEnds = new Vector<Room>();
		doors = new Vector<Room>();

		//initialize map
		map = new tileType[w][h];
		for(int j = 0; j < MAP_HEIGHT; j++){
			for(int i = 0; i < MAP_WIDTH; i++){
				map[i][j] = tileType.UNUSED;
			}
		}

		//place rooms
		placeRooms();
		System.out.println("\nPlaced Rooms...");
		debugPrintMap();

		//fill halls
		placeHalls();
		System.out.println("\nPlaced Halls...");
		debugPrintMap();

		//connect everything
		placeDoors();
		System.out.println("\nPlaced Doors...");
		debugPrintMap();

		//remove dead ends
		removeDeadEnds();
		System.out.println("\nRemoved Dead Ends...");
		debugPrintMap();
		
		//make the halls less jagged
		//straightenHalls();
		//System.out.println("\nStraightened Halls...");
		//debugPrintMap();

		//remove extra walls
		removeExtraWalls();
		System.out.println("\nRemoved Extra Walls...");
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
				if(map[i][j] == tileType.UNUSED){
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

			//try north edge
			if(c.y > 2){
				for(int i = c.x; i < c.x + c.w; i++){
					if(map[i][c.y-1] == tileType.ROOM) continue;
					
					if(map[i][c.y-2] == tileType.HALL || map[i][c.y-2] == tileType.ROOM){
						if(numDoors < MAX_DOORS_PER_ROOM) {
							map[i][c.y-1] = tileType.DOOR;
							doors.add(new Room(i,c.y-1,1,1));
							numDoors++;
							break;
						}
						else {
							break;
						}
					}
				}
			}

			//try south edge
			if(c.y + c.w < MAP_HEIGHT - 2){
				for(int i = c.x; i < c.x + c.w; i++){
					if(map[i][c.y+c.h] == tileType.ROOM) continue;
					
					if(map[i][c.y+c.h+1] == tileType.HALL || map[i][c.y+c.h+1] == tileType.ROOM){
						if(numDoors < MAX_DOORS_PER_ROOM) {
							map[i][c.y+c.h] = tileType.DOOR;
							doors.add(new Room(i,c.y+c.h,1,1));
							numDoors++;
							break;
						}
						else {
							break;
						}
					}
				}
			}

			//try west edge
			if(c.x > 2){
				for(int j = c.y; j < c.y + c.h; j++){
					if(map[c.x-1][j] == tileType.ROOM) continue;
					
					if(map[c.x-2][j] == tileType.HALL || map[c.x-2][j] == tileType.ROOM){
						if(numDoors < MAX_DOORS_PER_ROOM) {
							map[c.x-1][j] = tileType.DOOR;
							doors.add(new Room(c.x-1,j,1,1));
							numDoors++;
							break;
						}
						else {
							break;
						}
					}
				}
			}

			//try east edge
			if(c.x + c.y < MAP_WIDTH - 2){
				for(int j = c.y; j < c.y + c.h; j++){
					if(map[c.x+c.w][j] == tileType.ROOM) continue;
					
					if(map[c.x+c.w+1][j] == tileType.HALL || map[c.x+c.w+1][j] == tileType.ROOM){
						if(numDoors < MAX_DOORS_PER_ROOM) {
							map[c.x+c.w][j] = tileType.DOOR;
							doors.add(new Room(c.x+c.w,j,1,1));
							numDoors++;
							break;
						}
						else {
							break;
						}
					}
				}
			}
		}
	}

	void removeDeadEnds(){
		for(Room r : deadEnds){
			killDeadEnd(r.x, r.y);
		}
	}

	//iterate over each door, find a path to another door or intersection and attempt to straighten it
	void straightenHalls(){
		for(Room r : doors){
			crawlPathForIntersection(r.x, r.y);
		}
	}
	
	void removeExtraWalls(){
		for(int i = 0; i < MAP_WIDTH; i++){
			for(int j = 0; j < MAP_HEIGHT; j++){
				//check all sides to see if one is a hall/room/door, if so skip
				if(i > 0){
					if(map[i-1][j] == tileType.HALL || map[i-1][j] == tileType.ROOM || map[i-1][j] == tileType.DOOR) continue;
					if(j > 0 && (map[i-1][j-1] == tileType.HALL || map[i-1][j] == tileType.ROOM || map[i-1][j] == tileType.DOOR)) continue;
					if(j < MAP_HEIGHT-1 && (map[i-1][j] == tileType.HALL || map[i-1][j] == tileType.ROOM)) continue;
				}
				if(i < MAP_WIDTH -1){
					if(map[i+1][j] == tileType.HALL || map[i+1][j] == tileType.ROOM || map[i+1][j] == tileType.DOOR) continue;
					if(j > 0 && (map[i+1][j-1] == tileType.HALL || map[i+1][j] == tileType.ROOM || map[i+1][j] == tileType.DOOR)) continue;
					if(j < MAP_HEIGHT-1 && (map[i+1][j] == tileType.HALL || map[i+1][j] == tileType.ROOM || map[i+1][j] == tileType.DOOR)) continue;
				}
				if(j > 0 && (map[i][j-1] == tileType.HALL || map[i][j-1] == tileType.ROOM || map[i][j-1] == tileType.DOOR)) continue;
				if(j < MAP_HEIGHT-1 && (map[i][j+1] == tileType.HALL || map[i][j+1] == tileType.ROOM || map[i][j+1] == tileType.DOOR)) continue;
				
				map[i][j] = tileType.UNUSED;
			}
		}
	}

	void crawlPathForIntersection(int x, int y){
		int oldx = x, oldy = y;
		//find which direction to start
		if(y > 0 && map[x][y-1] == tileType.HALL) {
			y = y-1;
		}
		else if(y < MAP_HEIGHT -1 &&  map[x][y+1] == tileType.HALL) {
			y = y+1;
		}
		else if(x > 0 && map[x-1][y] == tileType.HALL) {
			x = x-1;
		}
		else if(x < MAP_WIDTH-1 && map[x+1][y] == tileType.HALL) {
			x = x+1;
		}
		
		//store this tile as the starting point for the straighten
		int startx = x, starty = y;
		
		//crawl along until an intersection or door is found
		while(!isIntersection(x, y)){
			if(x-1 >= 0 && x-1 != oldx && map[x-1][y] == tileType.HALL || map[x-1][y] == tileType.DOOR){
				oldx=x;
				x = x-1;
			}
			else if(x+1 < MAP_WIDTH && x+1 != x && map[x+1][y] == tileType.HALL || map[x+1][y] == tileType.DOOR){
				oldx=x;
				x = x+1;
			}
			else if(y-1 > 0 && y-1 != oldy && map[x][y-1] == tileType.HALL ||  map[x][y-1] == tileType.DOOR){
				oldy = y;
				y = y-1;
			}
			else if(y+1 < MAP_HEIGHT && y+1 != oldy && map[x][y+1] == tileType.HALL ||  map[x][y+1] == tileType.DOOR){
				oldy = y;
				y = y+1;
			}
		}
		
		//attempt to find a straighter line between them
		straighten(startx, starty, x, y);
		
		//clear out the dead ends created
	}
	
	void straighten(int x, int y, int targx, int targy){
		if(targx == x && targy == y) return;
		
		if(targx > x){
			if(targy > y){ //down and to the right
				//try horizontal first
				if(map[x+1][y] != tileType.WALL){
					setHall(x+1,y);
					straighten(x+1, y, targx, targy);
				}
				//if not do vertical first
				else if(map[x][y+1] != tileType.WALL){
					setHall(x,y+1);
					straighten(x,y+1,targx,targy);
				}
			}
			else if(targy <= y){ //up and to the right
				
			}
		}
		else if(targx <= x){
			if(targy > y){ //down and to the left
				
			}
			else if(targy <= y){ //up and to the left
				
			}
		}
	}
	
	void killDeadEnd(int x, int y){
		if(!isDeadEnd(x, y)) return;
		
		map[x][y] = tileType.HWALL;
		killDeadEnd(x-1, y);
		killDeadEnd(x+1,y);
		killDeadEnd(x,y-1);
		killDeadEnd(x,y+1);
		
	}
	
	boolean isDeadEnd(int x, int y){
		if(map[x][y] != tileType.HALL && map[x][y] != tileType.DOOR) return false;
		
		int numAdj = 0;
		if(map[x-1][y] == tileType.HALL || map[x-1][y] == tileType.ROOM || map[x-1][y] == tileType.DOOR) numAdj++;
		if(map[x+1][y] == tileType.HALL || map[x+1][y] == tileType.ROOM || map[x+1][y] == tileType.DOOR) numAdj++;
		if(map[x][y-1] == tileType.HALL || map[x][y-1] == tileType.ROOM || map[x][y-1] == tileType.DOOR) numAdj++;
		if(map[x][y+1] == tileType.HALL || map[x][y+1] == tileType.ROOM || map[x][y+1] == tileType.DOOR) numAdj++;
		if(numAdj > 1) return false;
		
		return true;
	}
	
	boolean isIntersection(int x, int y){
		if(map[x][y] != tileType.HALL && map[x][y] != tileType.DOOR) return false;
		if(map[x][y] == tileType.DOOR) return true; //all doors count as intersections
		
		int numAdj = 0;
		if(map[x-1][y] == tileType.HALL || map[x-1][y] == tileType.ROOM || map[x-1][y] == tileType.DOOR) numAdj++;
		if(map[x+1][y] == tileType.HALL || map[x+1][y] == tileType.ROOM || map[x+1][y] == tileType.DOOR) numAdj++;
		if(map[x][y-1] == tileType.HALL || map[x][y-1] == tileType.ROOM || map[x][y-1] == tileType.DOOR) numAdj++;
		if(map[x][y+1] == tileType.HALL || map[x][y+1] == tileType.ROOM || map[x][y+1] == tileType.DOOR) numAdj++;
		if(numAdj <= 2) return false;
		
		return true;
	}
	
	boolean generateHall(int x, int y){
		//if the hall is being generated on an occupied tile, end recursion
		if(map[x][y] != tileType.UNUSED && map[x][y] != tileType.HWALL){
			return false;
		}
		else {
			//set the tile as a hall; if it returned false then there was a problem
			if(!setHall(x, y)){
				return false;
			}

			int fromx = x, fromy = y;
			//find direction the hall came from
			if(map[x-1][y] == tileType.HALL) fromx = x-1; //came from the west
			else if(map[x+1][y] == tileType.HALL) fromx = x+1; //came from the east
			else if(map[x][y-1] == tileType.HALL) fromy = y-1; //came from the north
			else if(map[x][y+1] == tileType.HALL) fromy = y+1; //came from the south

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
		if(map[x][y] != tileType.UNUSED && map[x][y] != tileType.HWALL){
			return false;
		}
		
		//make sure there's only one adjacent hallway, if any
		int numAdj = 0;
		if(map[x-1][y] == tileType.HALL) numAdj++;
		if(map[x+1][y] == tileType.HALL) numAdj++;
		if(map[x][y-1] == tileType.HALL) numAdj++;
		if(map[x][y+1] == tileType.HALL) numAdj++;
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
		if(map[x-1][y] == tileType.HALL) left = true;
		if(map[x+1][y] == tileType.HALL) right = true;
		if(map[x][y-1] == tileType.HALL) up = true;
		if(map[x][y+1] == tileType.HALL) down = true;

		//if the hall is adjacent to more than one hall, reject
		if(up && left || up && right || down && left || down && right ) return false;

		if(map[x][y] == tileType.UNUSED || map[x][y] == tileType.HWALL){ //don't overwrite wall or room tiles; that would be bad
			map[x][y] = tileType.HALL;
			for(int i = x-1; i <= x+1; i++){
				for(int j = y-1; j <= y+1; j++){
					if(map[i][j] == tileType.UNUSED)
						map[i][j] = tileType.HWALL;
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

		//check that there's no overlap
		if(!allowRoomOverlap){
			for(int j = y; j < y+h; j++){
				for(int i = x; i < x+w; i++){
					if(map[i][j] != tileType.UNUSED) return false;
				}
			}
		}

		//System.out.println("placing " + w + "x" + h + " room at " + x + "," + y);

		for(int j = y-1; j <= y+h; j++){
			for(int i = x-1; i <= x+w; i++){
				if(map[i][j] == tileType.UNUSED || map[i][j] == tileType.WALL){
					if(!(i==x-1 || i == x+w || j == y-1 || j == y+h)){
						map[i][j] = tileType.ROOM;
					}
					else {
						map[i][j] = tileType.WALL;
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
				switch(map[i][j]){
				case UNUSED:
					System.out.print("███");
					break;
				case WALL:
					System.out.print("███");
					break;
				case HWALL:
					System.out.print("███");
					break;
				case HALL:
					System.out.print("   ");
					break;
				case ROOM:
					System.out.print("   ");
					break;
				case DOOR:
					System.out.print("   ");
					break;
				}
			}

			System.out.print("\n"); //end the line
		}
	}

	public static void main(String[] args) {
		DungeonGenerator dg = new DungeonGenerator(15,20);

		dg.debugPrintMap();
	}

}

class Room {
	int x;
	int y;
	int w;
	int h;

	public Room(int a, int b, int c, int d){
		x = a;
		y = b;
		w = c;
		h = d;
	}
}