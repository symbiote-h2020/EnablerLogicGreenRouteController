package eu.h2020.symbiote.smeur.elgrc.routing;

import java.util.HashSet;
import java.util.Set;

import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.elgrc.mapparser.MapParser;

public class Region {
	private String name;
	private String file;
	private String fileFormat;
	private StreetSegmentList streetSegmentList;
	private Set<Property> properties = new HashSet<Property>();
	
	public Region() {
		
	}
	
	public Region(String name, String file, String fileFormat, Set<Property> propSet) {
		this.setName(name);
		this.setFile(file);
		this.setFileFormat(fileFormat);
		this.setProperties(propSet);
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

	public Set<Property> getProperties() {
		return properties;
	}

	public void setProperties(Set<Property> properties) {
		this.properties = properties;
	}

}
