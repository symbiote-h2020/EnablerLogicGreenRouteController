package eu.h2020.symbiote.smeur.elgrc.routing;

import java.util.ArrayList;

public class RoutingService {

	private boolean isExternal;
	private String name;
	private ArrayList<Region> locations;
	private String routeAPI;
	
	public RoutingService() {
		this.setLocations(new ArrayList<Region>());
	}
	
	public RoutingService(boolean isExternal, String name, ArrayList<Region> locations, String routeAPI) {
		this.setExternal(isExternal);
		this.setName(name);
		this.setLocations(locations);
		this.setRouteAPI(routeAPI);
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

	public ArrayList<Region> getLocations() {
		return locations;
	}

	public void setLocations(ArrayList<Region> locations) {
		this.locations = locations;
	}
	
	public void appenLocation(Region location) {
		this.locations.add(location);
	}

	public String getRouteAPI() {
		return routeAPI;
	}

	public void setRouteAPI(String routeAPI) {
		this.routeAPI = routeAPI;
	}
	
}
