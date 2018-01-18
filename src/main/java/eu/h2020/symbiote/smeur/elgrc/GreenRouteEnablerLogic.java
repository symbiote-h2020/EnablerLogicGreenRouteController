package eu.h2020.symbiote.smeur.elgrc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

import at.ac.ait.ariadne.routeformat.ModeOfTransport;
import at.ac.ait.ariadne.routeformat.RequestModeOfTransport;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import at.ac.ait.ariadne.routeformat.RoutingResponse;
import at.ac.ait.ariadne.routeformat.RoutingRequest;
import at.ac.ait.ariadne.routeformat.Constants.GeneralizedModeOfTransportType;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONCoordinate;
import at.ac.ait.ariadne.routeformat.location.Location;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.ResourcesUpdated;
import eu.h2020.symbiote.enabler.messaging.model.NotEnoughResourcesAvailable;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.messages.GrcRequest;
import eu.h2020.symbiote.smeur.messages.GrcResponse;
import eu.h2020.symbiote.smeur.messages.PushInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;
import eu.h2020.symbiote.smeur.messages.Waypoint;
import eu.h2020.symbiote.smeur.StreetSegmentList;
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
	@Value("${routing.regions.fileFormats}")
	String regionsFileFormats;
	@Value("${routing.services}")
	String services;
	@Value("${routing.services.preferences}")
	String servicesPreferences;
	@Value("${routing.services.isexternal}")
	String servicesIsExternal;
	@Value("${routing.services.api.routerequest}")
	String servicesRouteAPIs;

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

	@Override
	public void resourcesUpdated(ResourcesUpdated ru) {
		System.out.println("Resources Updated:\n" + ru);
	}

	@Override
	public void notEnoughResources(NotEnoughResourcesAvailable nera) {
		System.out.println("Not Enough Resources Available:\n" + nera);
	}

	/**
	 * Method to create a structure of registered regions and services from
	 * bootstrp.properties
	 */
	private void buildServicesStructures() {
		String[] regionsArray = this.regions.split(";");
		String[] regionsFiles = this.regionsFiles.split(";");
		String[] regionsFileFormats = this.regionsFileFormats.split(";");
		for (int i = 0; i < regionsArray.length; i++) {
			Region r = new Region(regionsArray[i], regionsFiles[i], regionsFileFormats[i]);
			log.info("Going to parse file for " + regionsArray[i] + "region");
			r.parseStreetSegments();
			registeredRegions.add(r);
		}

		/*
		 * This part could probably be made more efficient with the use of hashmaps, but
		 * it shouldn't be an issue, because there probably won't be that many services.
		 */
		String[] servicesArray = this.services.split(";");
		String[] servicesPreferences = this.servicesPreferences.split(";");
		String[] servicesIsExternal = this.servicesIsExternal.split(";");
		String[] servicesRouteAPIs = this.servicesRouteAPIs.split(";");

		for (int i = 0; i < servicesArray.length; i++) {
			RoutingService newService = new RoutingService();
			newService.setName(servicesArray[i]);
			newService.setExternal(Boolean.parseBoolean(servicesIsExternal[i]));
			newService.setRouteAPI(servicesRouteAPIs[i]);

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
			log.info("Response from registering with Interpolator: " + response);
			try {
				if (response.status != RegisterRegionResponse.StatusCode.SUCCESS) {
					// TODO check not success
					;
				}
			} catch (NullPointerException npe) {
				log.error("Response from Interpolator when GRC registers is null!");
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
		RegisterRegion registrationMessage = new RegisterRegion();
		registrationMessage.regionID = region.getName();
		registrationMessage.streetSegments = region.getStreetSegmentList();
		registrationMessage.yPushInterpolatedValues = true;
		Set<Property> propSet = new HashSet<Property>();
		new Property("temperature", new ArrayList<String>());
		propSet.add(new Property("temperature", new ArrayList<String>()));
		registrationMessage.properties = propSet;
		return registrationMessage;
	}

	/**
	 * Method that registers the consumers of data of GRC
	 */
	private void registerConsumers() {
		// Consume Air Quality Updates
		log.info("Setting up Air Quality Updates Consumer");
		enablerLogic.registerAsyncMessageFromEnablerLogicConsumer(PushInterpolatedStreetSegmentList.class,
				(m) -> this.airQualityUpdatesConsumer(m));

		// Consume route Requests
		log.info("Setting up Route Request Consumer");
		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(GrcRequest.class, (m) -> this.routeRequestConsumer(m));
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
			try {
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
			} catch (NullPointerException e) {
				// TODO should this expection catcher be kept?
				log.error("Got no data from Interpolator!");
			}
		}
	}

	/**
	 * Consumes air quality data updates and sends them to whoever wants it
	 * 
	 * @param m
	 */
	private void airQualityUpdatesConsumer(PushInterpolatedStreetSegmentList m) {
		log.info("Received data from " + m.regionID);
		// Should be similar to requestAirQualityData
		for (RoutingService rs : this.registeredRoutingServices) {
			for (Region serviceRegion : rs.getLocations()) {
				if (serviceRegion.getName().equals(m.regionID)) {
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
	private GrcResponse routeRequestConsumer(GrcRequest r) {
		log.info("Received route request");
		for (RoutingService rs : this.registeredRoutingServices) {
			// TODO check if this is the service that the message should be sent to
			if (rs.isExternal()) {
				// https://github.com/dts-ait/ariadne-json-route-format/blob/master/src/main/java/at/ac/ait/ariadne/routeformat/RoutingRequest.java
				log.info("Building POST request");
				RoutingRequest rr = new RoutingRequest();

				// Define locations
				GeoJSONCoordinate gjcFrom = GeoJSONCoordinate.create(((WGS84Location) r.getFrom()).getLongitude(),
						((WGS84Location) r.getFrom()).getLatitude(), ((WGS84Location) r.getFrom()).getAltitude());
				GeoJSONCoordinate gjcTo = GeoJSONCoordinate.create(((WGS84Location) r.getTo()).getLongitude(),
						((WGS84Location) r.getTo()).getLatitude(), ((WGS84Location) r.getTo()).getAltitude());
				Location<?> locFrom = Location.createMinimal(gjcFrom);
				Location<?> locTo = Location.createMinimal(gjcTo);

				// Define mode of transportation,
				ModeOfTransport mot = ModeOfTransport.createMinimal(GeneralizedModeOfTransportType.FOOT);
				RequestModeOfTransport<?> rmot = RequestModeOfTransport.createMinimal(mot);
				ArrayList<RequestModeOfTransport<?>> rmotList = new ArrayList<RequestModeOfTransport<?>>();
				rmotList.add(rmot);

				// Put everything into model
				rr.setFrom(locFrom);
				rr.setTo(locTo);
				rr.setOptimizedFor("TRAVELTIME");
				rr.setModesOfTransport(rmotList);

				// Send Post request with parameters
				RestTemplate template = new RestTemplate();
				HttpEntity<RoutingRequest> request = new HttpEntity<>(rr);

				HttpEntity<String> response = template.exchange(rs.getRouteAPI(), HttpMethod.POST, request,
						String.class);

				// Obtain url to obtain route
				HttpHeaders headers = response.getHeaders();
				String getUrl = headers.getLocation().toString();

				// Get request to obtain route
				RoutingResponse routeResponse = template.getForObject(getUrl, RoutingResponse.class);

				// Start extracting data from the response into our own model
				RouteSegment route = routeResponse.getRoutes().get(0).getSegments().get(0);

				List<GeoJSONCoordinate> coorList = route.getGeometryGeoJson().get().getGeometry().getCoordinates();
				List<Waypoint> wayList = new ArrayList<Waypoint>();
				for (GeoJSONCoordinate coor : coorList) {
					Waypoint w = new Waypoint();
					w.setLocation(new WGS84Location(coor.getX().doubleValue(), coor.getY().doubleValue(), 100, "",
							new ArrayList<String>()));
					wayList.add(w);
				}

				GrcResponse resp = new GrcResponse();
				resp.setDistance(route.getDistanceMeters());
				resp.setTravelTime(route.getDurationSeconds());
				resp.setAirQualityRating(0);
				resp.setRoute(wayList);

				return resp;
			} else {
				// TODO send through rabbit
			}
		}
		GrcResponse dummyResponse = new GrcResponse();
		dummyResponse.setAirQualityRating(1.0);
		dummyResponse.setDistance(1.0);
		dummyResponse.setTravelTime(1.0);
		dummyResponse.setRoute(new ArrayList<Waypoint>());
		return dummyResponse;
	}

}
