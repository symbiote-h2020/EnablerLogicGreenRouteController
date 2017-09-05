package eu.h2020.symbiote.smeur.elgrc.model;

public class AirQualityUpdateMessage {
	private String request;

    public AirQualityUpdateMessage() {
    }
    
    public AirQualityUpdateMessage(String request) {
        this.setRequest(request);
    }

	public String getRequest() {
		return request;
	}

	public void setRequest(String request) {
		this.request = request;
	}
    
}
