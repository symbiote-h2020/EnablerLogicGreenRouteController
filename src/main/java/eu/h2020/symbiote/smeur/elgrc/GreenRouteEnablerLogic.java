package eu.h2020.symbiote.smeur.elgrc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.enablerlogic.messaging.RegistrationHandlerClientService;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.smeur.elgrc.model.AirQualityUpdateMessage;
import eu.h2020.symbiote.smeur.elgrc.model.MessageRequest;
import eu.h2020.symbiote.smeur.elgrc.model.MessageResponse;
import eu.h2020.symbiote.smeur.elgrc.model.RouteRequest;
import eu.h2020.symbiote.smeur.elgrc.model.RouteResponse;

@Component
public class GreenRouteEnablerLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(GreenRouteEnablerLogic.class);

    private EnablerLogic enablerLogic;
    
    //@Autowired
    //private EnablerLogicProperties props;
    
    //@Autowired
    //private RegistrationHandlerClientService rhClientService;

    @Override
    public void initialization(EnablerLogic enablerLogic) {
        this.enablerLogic = enablerLogic;

        // do stuff
        requestAirQualityData();
        registerConsumers();
    }
	
    
    @Override
    public void measurementReceived(EnablerLogicDataAppearedMessage dataAppeared) {
        System.out.println("received new Observations:\n"+dataAppeared);
    }
    
    
    private void registerConsumers() {
    		// Consume Air Quality Updates 
	    	enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(
	    		    AirQualityUpdateMessage.class, 
	    		    (m) -> this.airQualityUpdatesConsumer(m));
	    	
	    	// Consume route Requests
	    	enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
	    		    RouteRequest.class, 
	    		    (m) -> this.routeRequestConsumer(m));
    }
    
    
    private void requestAirQualityData() {
    		//TODO request to interpolator and handle response (Rabbit)
    		//TODO store data
    		//TODO for each registered service handle depending if it is external or not
    	
    		//TODO if external register service and handle preferences (REST)
    		//TODO send street segments to service (REST)
    		//TODO send qir quality data to service (REST)
    	
    		//TODO if internal register service with PP and handle preferences (Rabbit)
		//TODO send street segments to PP (Rabbit)
		//TODO send qir quality data to PP (Rabbit)
    }
    
    private void airQualityUpdatesConsumer(AirQualityUpdateMessage m) {
    		//TODO incoming to be sent to services that want them
    		//TODO if external through rest, if internal through rabbit
    }
    
    private RouteResponse routeRequestConsumer(RouteRequest r) {
    		//TODO handle incoming route requests
    		//TODO send them to internal or external service 
    		//TODO return response
    		return null;
    }
    
}
