package com.jrc.tmacc.nerone.vo;

public class Entity {		
	
	protected String entityType;
	private String value;
	private String id;
	private String type;	
	private String count;
	private String pos;
	private String name;		
	
	public Entity() {
		this.entityType = "emm:entity";
	}
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	
	public String getCount() {
		return count;
	}
	public void setCount(String count) {
		this.count = count;
	}
	public String getPos() {
		return pos;
	}
	public void setPos(String pos) {
		this.pos = pos;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	
		
}
