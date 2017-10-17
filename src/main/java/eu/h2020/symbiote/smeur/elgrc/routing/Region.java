package eu.h2020.symbiote.smeur.elgrc.routing;

import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.elgrc.mapparser.MapParser;

public class Region {
	private String name;
	private String file;
	private String fileFormat;
	private StreetSegmentList streetSegmentList;
	
	public Region() {
		
	}
	
	public Region(String name, String file, String fileFormat) {
		this.setName(name);
		this.setFile(file);
		this.setFileFormat(fileFormat);
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
		this.streetSegmentList = MapParser.parser(this.file, this.fileFormat);
	}

	public String getFileFormat() {
		return fileFormat;
	}

	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
	}

}
