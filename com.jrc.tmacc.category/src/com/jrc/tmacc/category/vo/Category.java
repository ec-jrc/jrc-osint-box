package com.jrc.tmacc.category.vo;

import java.util.ArrayList;


/*
 * <category emm:rank="0" emm:score="1160" 
 * emm:trigger="Donald Trump[3]; President Donald Trump[1]; Trump[12]; ">Negative-CustomFeed-Trump</category>
 */
public class Category {
	
	private String rank;
	private String score;
	private String value;
	private ArrayList<Trigger> triggers;
	
	public String getRank() {
		return rank;
	}
	public void setRank(String rank) {
		this.rank = rank;
	}
	public String getScore() {
		return score;
	}
	public void setScore(String score) {
		this.score = score;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public ArrayList<Trigger> getTriggers() {
		return triggers;
	}
	public void setTriggers(ArrayList<Trigger> triggers) {
		this.triggers = triggers;
	}	
}
