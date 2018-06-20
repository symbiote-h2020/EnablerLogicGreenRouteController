package eu.h2020.symbiote.smeur.elgrc.repositories.entities;

import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.messages.RouteCommunication;
import org.springframework.data.annotation.Id;

import javax.validation.constraints.NotNull;

public class RoutePoint {

	@Id
	private String id;

	private WGS84Location location;

	private long timestamp;

	@NotNull
	private long routeId;

	public RoutePoint(){}

	public RoutePoint(RouteCommunication routeCommunication){
		this.routeId = routeCommunication.getRouteId();
		this.timestamp = routeCommunication.getTimestamp();
		this.location = routeCommunication.getLocation();
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public WGS84Location getLocation() {
		return location;
	}

	public void setLocation(WGS84Location location) {
		this.location = location;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public long getRouteId() {
		return routeId;
	}

	public void setRouteId(long routeId) {
		this.routeId = routeId;
	}
}
