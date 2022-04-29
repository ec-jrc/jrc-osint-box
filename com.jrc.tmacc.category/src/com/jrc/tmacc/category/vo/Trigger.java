package com.jrc.tmacc.category.vo;

/*
 * emm:trigger="Donald Trump[3]; President Donald Trump[1]; Trump[12]; "
 */
public class Trigger {
	
	private String text;
	private String count;
	
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getCount() {
		return count;
	}
	public void setCount(String count) {
		this.count = count;
	}
}
