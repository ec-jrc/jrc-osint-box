package com.jrc.tmacc.nerone.vo;

/*
 * <emm:guess type="o" subtype="ORG" count="1" pos="186" name="Iranian military" rules="mo1_6">Iranian military</emm:guess>
 */
public class Guess extends Entity {
	
	private String subType;
	private String rules;
	
	public Guess() {
		this.entityType = "emm:guess";
	}
	
	public String getSubType() {
		return subType;
	}
	public void setSubType(String subType) {
		this.subType = subType;
	}
	public String getRules() {
		return rules;
	}
	public void setRules(String rules) {
		this.rules = rules;
	}
	
}
