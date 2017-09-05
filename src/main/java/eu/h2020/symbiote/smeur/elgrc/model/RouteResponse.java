package eu.h2020.symbiote.smeur.elgrc.model;

public class RouteResponse {
	private String response;

    public RouteResponse() {
    }
    
    public RouteResponse(String response) {
        this.setResponse(response);
    }

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}
}
