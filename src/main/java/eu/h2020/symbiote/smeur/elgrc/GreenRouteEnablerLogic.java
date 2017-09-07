package eu.h2020.symbiote.smeur.elgrc;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.elgrc.model.AirQualityUpdateMessage;
import eu.h2020.symbiote.smeur.elgrc.model.RouteRequest;
import eu.h2020.symbiote.smeur.elgrc.model.RouteResponse;
import eu.h2020.symbiote.smeur.elgrc.routing.Region;
import eu.h2020.symbiote.smeur.elgrc.routing.RoutingService;

@Component
public class GreenRouteEnablerLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(GreenRouteEnablerLogic.class);

	private EnablerLogic enablerLogic;

	private ArrayList<Region> registeredRegions;
	private ArrayList<RoutingService> registeredRoutingServices;

	@Value("${routing.regions}")
	String regions;
	@Value("${routing.regions.files}")
	String regionsFiles;
	@Value("${routing.services}")
	String services;
	@Value("${routing.services.preferences}")
	String servicesPreferences;
	@Value("${routing.services.isexternal}")
	String servicesIsExternal;

	@Override
	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;
		this.registeredRegions = new ArrayList<Region>();
		this.registeredRoutingServices = new ArrayList<RoutingService>();

		// do stuff
		buildServicesStructures();
		registerWithInterpolator();
		// the previous step might take a while?
		requestAirQualityData();
		registerConsumers();
	}

	@Override
	public void measurementReceived(EnablerLogicDataAppearedMessage dataAppeared) {
		System.out.println("received new Observations:\n" + dataAppeared);
	}

	/**
	 * Method to create a structure of registered regions and services from
	 * bootstrp.properties
	 */
	private void buildServicesStructures() {
		String[] regionsArray = this.regions.split(";");
		String[] regionsFiles = this.regionsFiles.split(";");
		for (int i = 0; i < regionsArray.length; i++)
			registeredRegions.add(new Region(regionsArray[i], regionsFiles[i]));

		/*
		 * This part could probably be made more efficient with the use of hashmaps, but
		 * it shouldn't be an issue, because there probably won't be that many services.
		 */
		String[] servicesArray = this.services.split(";");
		String[] servicesPreferences = this.servicesPreferences.split(";");
		String[] servicesIsExternal = this.servicesIsExternal.split(";");
		for (int i = 0; i < servicesArray.length; i++) {
			RoutingService newService = new RoutingService();
			newService.setName(servicesArray[i]);
			newService.setExternal(Boolean.parseBoolean(servicesIsExternal[i]));

			String[] servicePreferences = servicesPreferences[i].split(",");
			for (int j = 0; j < servicePreferences.length; j++) {
				for (Region region : this.registeredRegions) {
					if (region.getName().equals(servicePreferences[j])) {
						newService.appenLocation(region);
						break;
					}
				}
			}

			this.registeredRoutingServices.add(newService);
		}
	}

	/**
	 * Method that sends the streetsegments to the interpolator
	 */
	private void registerWithInterpolator() {
		for (Region region : this.registeredRegions) {
			RegisterRegion registrationMessage = buildRegistrationMessage(region);

			RegisterRegionResponse response = enablerLogic.sendSyncMessageToEnablerLogic("EnablerLogicInterpolator",
					registrationMessage, RegisterRegionResponse.class);
			if (response.status != RegisterRegionResponse.StatusCode.SUCCESS) {
				// TODO check not success
				;
			}
		}

	}

	/**
	 * Method that builds the message to send to the interpolator
	 * 
	 * @param region
	 *            The region that contains the streetsegments
	 * @return the message to be sent
	 */
	private RegisterRegion buildRegistrationMessage(Region region) {
		log.info("Loading Street Data Information");
		// TODO get streetsegments from file in region.file
		RegisterRegion registrationMessage = new RegisterRegion();
		registrationMessage.regionID = region.getName();
		return registrationMessage;
	}

	/**
	 * Method that registers the consumers of data of GRC
	 */
	private void registerConsumers() {
		// Consume Air Quality Updates
		log.info("Setting up Air Quality Updates Consumer");
		enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(AirQualityUpdateMessage.class,
				(m) -> this.airQualityUpdatesConsumer(m));

		// Consume route Requests
		log.info("Setting up Route Request Consumer");
		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(RouteRequest.class,
				(m) -> this.routeRequestConsumer(m));
	}

	/**
	 * Method to obtain air quality data from interpolator
	 */
	private void requestAirQualityData() {
		for (Region region : this.registeredRegions) {
			log.info("Requesting data from " + region.getName());
			QueryInterpolatedStreetSegmentList interpolatedRequest = new QueryInterpolatedStreetSegmentList();
			interpolatedRequest.sslID = region.getName();
			QueryInterpolatedStreetSegmentListResponse response = enablerLogic.sendSyncMessageToEnablerLogic(
					"EnablerLogicInterpolator", interpolatedRequest, QueryInterpolatedStreetSegmentListResponse.class);

			log.info("Received data from " + region.getName());
			StreetSegmentList streetSegments = response.theList;

			for (RoutingService rs : this.registeredRoutingServices) {
				for (Region serviceRegion : rs.getLocations()) {
					if (serviceRegion.getName().equals(region.getName())) {
						if (rs.isExternal()) {
							log.info("Sending Air Quality Updates from " + serviceRegion.getName() + " to "
									+ rs.getName() + " through REST");
							// TODO send street segments and qir quality to service (REST)
						} else {
							log.info("Sending Air Quality Updates from " + serviceRegion.getName() + " to "
									+ rs.getName() + " through Rabbit");
							// TODO send street segments and qir quality to PP (Rabbit)
						}
						break;
					}
				}
			}
		}
	}

	/**
	 * Consumes air quality data updates and sends them to whoever wants it
	 * 
	 * @param m
	 */
	private void airQualityUpdatesConsumer(AirQualityUpdateMessage m) {
		log.info("Received data from " + m.getRegionID());
		// Should be similar to requestAirQualityData
		for (RoutingService rs : this.registeredRoutingServices) {
			for (Region serviceRegion : rs.getLocations()) {
				if (serviceRegion.getName().equals(m.getRegionID())) {
					if (rs.isExternal()) {
						log.info("Sending Air Quality Updates from " + serviceRegion.getName() + " to " + rs.getName()
								+ " through REST");
						// TODO send through rest
					} else {
						log.info("Sending Air Quality Updates from " + serviceRegion.getName() + " to " + rs.getName()
								+ " through Rabbit");
						// TODO send through rabbit
					}
					break;
				}
			}
		}
	}

	/**
	 * Method that consumes the route requests and redirects them to the correct
	 * service
	 * 
	 * @param r
	 * @return
	 */
	private RouteResponse routeRequestConsumer(RouteRequest r) {
		log.info("Received route request");
		for (RoutingService rs : this.registeredRoutingServices) {
			// TODO check if this is the service that the message should be sent to
			if (rs.isExternal()) {
				// TODO send through rest
			} else {
				// TODO send through rabbit
			}
		}
		return null;
	}

}
