package com.jrc.tmacc.nerone.vo;

/**
 * <emm:georss name="Brussels:(Bruxelles-Capitale):Region de Bruxelles-Capitale / Brussels Hoofdstedelijk Gewes:Belgium" 
			id="16843797" lat="50.8371" lon="4.36761" count="1" pos="331" class="1" iso="BE" charpos="331" 
					wordlen="9" score="0.0">Bruxelles</emm:georss>
 * @author charlgr
 *
 */
public class GeoRSS extends Entity {
	
	private String lat;
	private String lon;
	private String emmClass;
	private String iso;
	private String charpos;
	private String wordlen;
	private String score;
	
	public GeoRSS() {
		this.entityType = "emm:georss";
	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}

	public String getEmmClass() {
		return emmClass;
	}

	public void setEmmClass(String emmClass) {
		this.emmClass = emmClass;
	}

	public String getIso() {
		return iso;
	}

	public void setIso(String iso) {
		this.iso = iso;
	}

	public String getCharpos() {
		return charpos;
	}

	public void setCharpos(String charpos) {
		this.charpos = charpos;
	}

	public String getWordlen() {
		return wordlen;
	}

	public void setWordlen(String wordlen) {
		this.wordlen = wordlen;
	}

	public String getScore() {
		return score;
	}

	public void setScore(String score) {
		this.score = score;
	}

}
