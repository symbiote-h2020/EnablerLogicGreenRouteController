package eu.h2020.symbiote.smeur.elgrc.routing;

import java.util.ArrayList;

public class RoutingService {

	private boolean isExternal;
	private String name;
	private ArrayList<String> locations;
	
	public RoutingService() {
		this.setLocations(new ArrayList<String>());
	}
	
	public RoutingService(boolean isExternal, String name, ArrayList<String> locations) {
		this.setExternal(isExternal);
		this.setName(name);
		this.setLocations(locations);
	}

	public boolean isExternal() {
		return isExternal;
	}

	public void setExternal(boolean isExternal) {
		this.isExternal = isExternal;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ArrayList<String> getLocations() {
		return locations;
	}

	public void setLocations(ArrayList<String> locations) {
		this.locations = locations;
	}
	
}
