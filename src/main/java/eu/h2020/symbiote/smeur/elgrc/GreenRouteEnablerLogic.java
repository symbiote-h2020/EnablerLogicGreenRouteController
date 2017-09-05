package eu.h2020.symbiote.smeur.elgrc;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.elgrc.model.AirQualityUpdateMessage;
import eu.h2020.symbiote.smeur.elgrc.model.RouteRequest;
import eu.h2020.symbiote.smeur.elgrc.model.RouteResponse;
import eu.h2020.symbiote.smeur.elgrc.routing.RoutingService;

@Component
public class GreenRouteEnablerLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(GreenRouteEnablerLogic.class);

    private EnablerLogic enablerLogic;
    
    private ArrayList<RoutingService> registeredRoutingServices;

    @Override
    public void initialization(EnablerLogic enablerLogic) {
        this.enablerLogic = enablerLogic;
        this.registeredRoutingServices = new ArrayList<RoutingService>();

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
    		log.info("Setting up Air Quality Updates Consumer");
	    	enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(
	    		    AirQualityUpdateMessage.class, 
	    		    (m) -> this.airQualityUpdatesConsumer(m));

	    	// Consume route Requests
	    	log.info("Setting up Route Request Consumer");
	    	enablerLogic.registerSyncMessageFromEnablerLogicConsumer(
	    		    RouteRequest.class, 
	    		    (m) -> this.routeRequestConsumer(m));
    }
    
    
    private void requestAirQualityData() {
    		//This should work, but couldn't find in the interpolator code this exposed
    		QueryInterpolatedStreetSegmentListResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
	    		    "EnablerLogicInterpolator",
	    		    new QueryInterpolatedStreetSegmentList(),
	    		    QueryInterpolatedStreetSegmentListResponse.class);
    		
    		StreetSegmentList streetSegments = response.theList;
    		
    		if (!registerServices()) {
    			//TODO see expected behaviour if no services are registered
    			;
    		}
    	
    		//TODO for each registered service handle depending if it is external or not
    		for (RoutingService rs : this.registeredRoutingServices) {
    			//TODO only send the streetsegments the service wants, doesn't seem like this distinction exists yet
    			if (rs.isExternal()) {
    	    			//TODO send street segments to service (REST)
    	    			//TODO send qir quality data to service (REST)	
    			}
    			else {
    				//TODO send street segments to PP (Rabbit)
    				//TODO send qir quality data to PP (Rabbit)
    			}
    		}
    }
    
    private boolean registerServices() {
    		//TODO check how services are stored
    		return true;
    }
    
    private void airQualityUpdatesConsumer(AirQualityUpdateMessage m) {
    		//TODO incoming to be sent to services that want them
    		for (RoutingService rs : this.registeredRoutingServices) {
    			//TODO check if service wants this data
    			if (rs.isExternal()) {
    				//TODO send through rest
    			}
    			else {
    				//TODO send through rabbit
    			}
    		}
    }
    
    private RouteResponse routeRequestConsumer(RouteRequest r) {
	    	for (RoutingService rs : this.registeredRoutingServices) {
	    		//TODO check if this is the service that the message should be sent to
	    		if (rs.isExternal()) {
    				//TODO send through rest
    			}
    			else {
    				//TODO send through rabbit
    			}
	    	}
    		return null;
    }
    
}
