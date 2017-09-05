package eu.h2020.symbiote.smeur.elgrc.model;

public class RouteRequest {
	private String request;

    public RouteRequest() {
    }
    
    public RouteRequest(String request) {
        this.setRequest(request);
    }

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}
}
