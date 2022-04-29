package com.jrc.tmacc.category.vo;

import java.util.ArrayList;


public class CategoryMatcherResult {
	
	private String language;
	private String pubDate;
	private String text;
	private int wordCount;
	private String date;	
	private ArrayList<Category> categories;	
	private String name;
	
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getPubDate() {
		return pubDate;
	}
	public void setPubDate(String pubDate) {
		this.pubDate = pubDate;
	}
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getWordCount() {
		return wordCount;
	}
	public void setWordCount(int wordCount) {
		this.wordCount = wordCount;
	}
	public String getDate() {
		return date;
	}
	public void setDate(String date) {
		this.date = date;
	}
	public ArrayList<Category> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<Category> cats) {
		this.categories = cats;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}	
}
