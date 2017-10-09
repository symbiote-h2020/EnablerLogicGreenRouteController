package eu.h2020.symbiote.smeur.elgrc.routing;

import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.elgrc.mapparser.MapParser;

public class Region {
	private String name;
	private String file;
	private StreetSegmentList streetSegmentList;
	
	public Region() {
		
	}
	
	public Region(String name, String file) {
		this.setName(name);
		this.setFile(file);
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public StreetSegmentList getStreetSegmentList() {
		return streetSegmentList;
	}

	public void setStreetSegmentList(StreetSegmentList streetSegmentList) {
		this.streetSegmentList = streetSegmentList;
	}
	
	/**
	 * Method to parse this regions file 
	 */
	public void parseStreetSegments() {
		this.streetSegmentList = MapParser.parser(this.file);
	}
}
