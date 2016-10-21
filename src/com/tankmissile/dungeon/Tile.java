package com.tankmissile.dungeon;

public class Tile {
	public static final int NORTH = 0, SOUTH = 1, WEST = 3, EAST = 4;
	public static final int UNUSED = 0, WALL = 1, HALL = 2, HWALL = 3, ROOM = 4, DOOR = 5;
	public static final int FIRE = 0, NATURE = 1, WATER = 2, MACHINE = 3, DARKNESS = 4;
	
	protected int type = UNUSED;
	
	protected final int x, y;
	
	public Tile(int x, int y){
		this.x=x;
		this.y=y;
	}
	
	public int getType(){
		return type;
	}
	
	public void setType(int t){
		type = t;
	}
	
	public String toString(){
		switch(type){
		case UNUSED:
			return "███";
		case WALL:
			return "███";
		case HWALL:
			return "███";
		case HALL:
			return "   ";
		case ROOM:
			return "   ";
		case DOOR:
			return "   ";
		default:
			return "   ";
		}
	}
}