package eu.h2020.symbiote.smeur.elgrc.model;

import eu.h2020.symbiote.smeur.StreetSegmentList;

public class AirQualityUpdateMessage {
	private StreetSegmentList theList;
	private String regionID;

    public AirQualityUpdateMessage() {
    }

	public StreetSegmentList getTheList() {
		return theList;
	}

	public void setTheList(StreetSegmentList theList) {
		this.theList = theList;
	}

	public String getRegionID() {
		return regionID;
	}

	public void setRegionID(String regionID) {
		this.regionID = regionID;
	}
    
}
