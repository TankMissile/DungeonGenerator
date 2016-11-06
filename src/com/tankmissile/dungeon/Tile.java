package com.tankmissile.dungeon;

public class Tile {
	public static final int NORTH = 0, SOUTH = 1, WEST = 2, EAST = 3;
	public static final int UNUSED = 0, WALL = 1, HALL = 2, HWALL = 3, ROOM = 4, DOOR = 5;
	public static final int NONE = 0, FIRE = 1, NATURE = 2, WATER = 3, MACHINE = 4, DARKNESS = 5;
	public static final int MAG = 1, ROCK = 2, ACID = 3, BUG = 4, MINE = 5, CHEST = 6, MONSTER = 7, SPAWN = 8, EXIT = 9;
	public static final int EP = 0, MEM = 1, BITS = 2, RET = 3; //bug types
	
	protected int type = UNUSED;
	protected int object = NONE;
	protected int trapstrength = 0; //level of disarm required to remove a trap
	
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
	
	public int getObject(){
		return object;
	}
	
	public void setObject(int o){
		object = o;
	}
	public void setObject(int o, int str){
		object = o;
		trapstrength = str;
	}
	
	public boolean isPassable(){
		return (type == HALL || type == ROOM || type == DOOR);
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
			switch(object){
			case MAG:
				return "|M" + trapstrength;
			case ROCK: 
				return "|R" + trapstrength;
			case ACID:
				return "~" + trapstrength + "~";
			case MINE:
				return "[" + trapstrength + "]";
			case BUG:
				return "(" + trapstrength + ")";
			case CHEST:
				return "[C]";
			case MONSTER:
				return "D:<";
			case SPAWN:
				return " S ";
			case EXIT:
				return " + ";
			}
			return "   ";
		case DOOR:
			return "   ";
		default:
			return "   ";
		}
	}
}