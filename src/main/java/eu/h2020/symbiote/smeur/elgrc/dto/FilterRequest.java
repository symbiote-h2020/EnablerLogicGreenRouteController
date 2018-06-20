package eu.h2020.symbiote.smeur.elgrc.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FilterRequest {

	public Long begin;
	public Long end;
	public Long routeId;

	public FilterRequest() {}


	public FilterRequest(Long begin, Long end) {
		this.begin = begin;
		this.end = end;
	}

	public FilterRequest(Long begin,Long end,Long routeId) {
		this.begin = begin;
		this.routeId = routeId;
	}

	public Long getBegin() {
		return begin;
	}

	public void setBegin(Long begin) {
		this.begin = begin;
	}

	public Long getEnd() {
		return end;
	}

	public void setEnd(Long end) {
		this.end = end;
	}

	public Long getRouteId() {
		return routeId;
	}

	public void setRouteId(Long routeId) {
		this.routeId = routeId;
	}
}
