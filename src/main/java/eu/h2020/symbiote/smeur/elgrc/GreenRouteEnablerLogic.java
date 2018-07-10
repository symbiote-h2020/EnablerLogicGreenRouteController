package eu.h2020.symbiote.smeur.elgrc;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import eu.h2020.symbiote.smeur.elgrc.commons.Utils;
import eu.h2020.symbiote.smeur.elgrc.repositories.RouteRepository;
import eu.h2020.symbiote.smeur.elgrc.repositories.entities.RoutePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import at.ac.ait.ariadne.routeformat.ModeOfTransport;
import at.ac.ait.ariadne.routeformat.RequestModeOfTransport;
import at.ac.ait.ariadne.routeformat.RouteSegment;
import at.ac.ait.ariadne.routeformat.RoutingResponse;
import at.ac.ait.ariadne.routeformat.RoutingRequest;
import at.ac.ait.ariadne.routeformat.Constants.GeneralizedModeOfTransportType;
import at.ac.ait.ariadne.routeformat.geojson.GeoJSONCoordinate;
import at.ac.ait.ariadne.routeformat.location.Location;
import eu.h2020.symbiote.core.internal.CoreQueryRequest;
import eu.h2020.symbiote.enabler.messaging.model.EnablerLogicDataAppearedMessage;
import eu.h2020.symbiote.enabler.messaging.model.ResourcesUpdated;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskInfo;
import eu.h2020.symbiote.enabler.messaging.model.ServiceExecutionTaskResponse;
import eu.h2020.symbiote.enabler.messaging.model.ServiceParameter;
import eu.h2020.symbiote.enabler.messaging.model.NotEnoughResourcesAvailable;
import eu.h2020.symbiote.enabler.messaging.model.PlatformProxyResourceInfo;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerAcquisitionStartResponse;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoRequest;
import eu.h2020.symbiote.enabler.messaging.model.ResourceManagerTaskInfoResponse;
import eu.h2020.symbiote.enablerlogic.EnablerLogic;
import eu.h2020.symbiote.enablerlogic.ProcessingLogic;
import eu.h2020.symbiote.enablerlogic.messaging.properties.EnablerLogicProperties;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.smeur.messages.GrcRequest;
import eu.h2020.symbiote.smeur.messages.GrcResponse;
import eu.h2020.symbiote.smeur.messages.PushInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentList;
import eu.h2020.symbiote.smeur.messages.QueryInterpolatedStreetSegmentListResponse;
import eu.h2020.symbiote.smeur.messages.RegisterRegion;
import eu.h2020.symbiote.smeur.messages.RegisterRegionResponse;
import eu.h2020.symbiote.smeur.messages.RouteCommunication;
import eu.h2020.symbiote.smeur.messages.Waypoint;
import eu.h2020.symbiote.smeur.StreetSegmentList;
import eu.h2020.symbiote.smeur.elgrc.routing.Region;
import eu.h2020.symbiote.smeur.elgrc.routing.RoutingService;

@Component
public class GreenRouteEnablerLogic implements ProcessingLogic {
	private static final Logger log = LoggerFactory.getLogger(GreenRouteEnablerLogic.class);

	private EnablerLogic enablerLogic;
	
	@Autowired
    private EnablerLogicProperties props;

	private ArrayList<Region> registeredRegions;
	private ArrayList<RoutingService> registeredRoutingServices;

	@Autowired
	private RouteRepository routeRepo;
	
	private PlatformProxyResourceInfo routingServiceInfo;

	@Value("${routing.regions}")
	String regions;
	@Value("${routing.regions.files}")
	String regionsFiles;
	@Value("${routing.regions.fileFormats}")
	String regionsFileFormats;
	@Value("${routing.regions.properties}")
	String regionsProperties;
	@Value("${routing.regions.properties.iri}")
	String regionsPropertiesIRI;
	@Value("${routing.services}")
	String services;
	@Value("${routing.services.preferences}")
	String servicesPreferences;
	@Value("${routing.services.isexternal}")
	String servicesIsExternal;
	@Value("${routing.services.api.routerequest}")
	String servicesRouteAPIs;
	@Value("${routing.service.id}")
	String routingServiceId;
	

	@Override
	public void initialization(EnablerLogic enablerLogic) {
		this.enablerLogic = enablerLogic;
		this.registeredRegions = new ArrayList<Region>();
		this.registeredRoutingServices = new ArrayList<RoutingService>();
		this.routingServiceInfo = new PlatformProxyResourceInfo();

		// do stuff
		searchAndStoreRoutingService();
		buildServicesStructures();
		registerConsumers();
		registerWithInterpolator();
		// the previous step might take a while?
		//TODO keep this comented? requestAirQualityData();
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
	 * Method to serach and store the metainformation of the routing service 
	 * from MoBaaS
	 */
	private void searchAndStoreRoutingService() {
		log.info("Searching for routing service...");
		CoreQueryRequest coreQueryRequest = new CoreQueryRequest();
		coreQueryRequest.setId(routingServiceId);
		
		ResourceManagerTaskInfoRequest request = new ResourceManagerTaskInfoRequest(
	    		"routing service search", //task id
	    		1, 
	    		1, 
	    		coreQueryRequest, 
	    		null, //"P0000-00-00T00:01:00",
	    		false, 
	    		null, 
	    		false,
	    		props.getEnablerName(), 
	    		null
	    	);

	    ResourceManagerAcquisitionStartResponse response = enablerLogic.queryResourceManager(request);

	    try {
	        log.info("Response JSON: {}", new ObjectMapper().writeValueAsString(response));
	    } catch (JsonProcessingException e) {
	        log.info("Response: {}", response);
	    }
	    
	    ResourceManagerTaskInfoResponse resourceManagerTaskInfoResponse = response.getTasks().get(0);
		String resourceId = resourceManagerTaskInfoResponse.getResourceDescriptions().get(0).getId();
		String accessURL = resourceManagerTaskInfoResponse.getResourceUrls().get(resourceId);
		
		routingServiceInfo.setAccessURL(accessURL);
		routingServiceInfo.setResourceId(resourceId);
		log.info("MoBaaS Routing Service obtained!");
	}

	/**
	 * Method to create a structure of registered regions and services from
	 * bootstrp.properties
	 */
	private void buildServicesStructures() {
		String[] regionsArray = this.regions.split(";");
		String[] regionsFiles = this.regionsFiles.split(";");
		String[] regionsFileFormats = this.regionsFileFormats.split(";");
		String[] regionsProperties = this.regionsProperties.split(";");
		String[] regionsPropertiesIRI = this.regionsPropertiesIRI.split(";");
		
		for (int i = 0; i < regionsArray.length; i++) {
			Set<Property> propSet = new HashSet<Property>();
			
			String[] regionProperties = regionsProperties[i].split(",");
			String[] regionPropertiesIRI = regionsPropertiesIRI[i].split(",");
			
			for (int j = 0; j < regionProperties.length; j++) 
				propSet.add(new Property(
						regionProperties[j], regionPropertiesIRI[j], new ArrayList<String>()));
			
			
			Region r = new Region(regionsArray[i], regionsFiles[i], regionsFileFormats[i], propSet);
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
		log.info("Registering regions with the Interpolator...");
		for (Region region : this.registeredRegions) {
			log.info("Registering " + region.getName() + " region...");
			RegisterRegion registrationMessage = buildRegistrationMessage(region);

			RegisterRegionResponse response = enablerLogic.sendSyncMessageToEnablerLogic("EnablerLogicInterpolator",
					registrationMessage, RegisterRegionResponse.class, 120_000);
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
		log.info("Loading Street Data Information for " + region.getName());
		RegisterRegion registrationMessage = new RegisterRegion();
		registrationMessage.regionID = region.getName();
		registrationMessage.streetSegments = region.getStreetSegmentList();
		registrationMessage.yPushInterpolatedValues = true;
		registrationMessage.properties = region.getProperties();
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
		
		// Consume route communication Requests
		log.info("Setting up Route Communication Consumer");
		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(RouteCommunication.class,
				(m) -> this.routeCommunicationUpdatesConsumer(m));

		// Consume route Requests
		log.info("Setting up Route Request Consumer");
		enablerLogic.registerSyncMessageFromEnablerLogicConsumer(GrcRequest.class, (m) -> this.routeRequestConsumer(m));
	}
	
	/**
	 * Method to store route communications 
	 */
	private GrcResponse routeCommunicationUpdatesConsumer(RouteCommunication rc) {
		log.info("Received point:\nId: " + rc.getRouteId() + "\n" + rc.getLocation() + "\nTimestamp: " + rc.getTimestamp());

		routeRepo.save(new RoutePoint(rc));

		return null;
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
		
		log.info("Storing to file data from " + m.regionID);
		StreetSegmentList streetSegments = m.theList;
		ObjectMapper mapper = new ObjectMapper();

		File newFile = new File("streetSegments" + m.regionID + ".json");

		try {
			mapper.writeValue(newFile, streetSegments);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("DONE!");
		
		// Should be similar to requestAirQualityData
		for (RoutingService rs : this.registeredRoutingServices) {
			for (Region serviceRegion : rs.getLocations()) {
				if (serviceRegion.getName().equals(m.regionID)) {
					if (rs.isExternal()) {
						log.info("Sending Air Quality Updates from " + serviceRegion.getName() + " to " + rs.getName()
								+ " through REST");
						// log.info(m.theList.toString());
						try {
							log.info("The size of the received data is " + m.theList.size());
						} catch (NullPointerException e) {
							log.error("Received a null update!");
						}
						
						// TODO send through rest
					} else {
						File zipFile = null;
						log.info("Sending Air Quality Updates from " + serviceRegion.getName() + " to " + rs.getName()
								+ " through Rabbit");
						// TODO send through rabbit

						try {
							// TODO ~ not tested yet
							zipFile = Utils.zipCompress(newFile);

							// TODO ~ send file

						} catch (IOException e) {
							log.error("[ERROR] compressing file > " + e.getMessage());«
						} finally {
							newFile.delete();
							zipFile.delete();
						}

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
			if (isInVienna(r.getFrom())) {
				log.info("The route is for Vienna ---> Send it to AIT!");
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
				
				ArrayList<RequestModeOfTransport<?>> rmotList = null;
				// Define mode of transportation,
				if (r.getTransportationMode().equalsIgnoreCase("foot")) {
					ModeOfTransport mot = ModeOfTransport.createMinimal(GeneralizedModeOfTransportType.FOOT);
					RequestModeOfTransport<?> rmot = RequestModeOfTransport.createMinimal(mot);
					rmotList = new ArrayList<RequestModeOfTransport<?>>();
					rmotList.add(rmot);
				} else if (r.getTransportationMode().equalsIgnoreCase("bike") || r.getTransportationMode().equalsIgnoreCase("bicycle")) {
					ModeOfTransport mot = ModeOfTransport.createMinimal(GeneralizedModeOfTransportType.BICYCLE);
					RequestModeOfTransport<?> rmot = RequestModeOfTransport.createMinimal(mot);
					rmotList = new ArrayList<RequestModeOfTransport<?>>();
					rmotList.add(rmot);
				}

				// Put everything into model
				rr.setFrom(locFrom);
				rr.setTo(locTo);
				rr.setOptimizedFor("TRAVELTIME");  // TODO check if better option
				rr.setModesOfTransport(rmotList);

				// Send Post request with parameters
				log.info("Sendind POST request to AIT!");
				RestTemplate template = new RestTemplate();
				HttpEntity<RoutingRequest> request = new HttpEntity<>(rr);
				ResponseEntity<String> response = null;
				
				try {
					response = template.exchange(rs.getRouteAPI(), HttpMethod.POST, request,
							String.class);
				} catch (HttpClientErrorException e) {
					log.error("Problem communicating with AIT Routing Engine!");
					e.printStackTrace();
					return new GrcResponse();
				}

				// Obtain url to obtain route
				HttpHeaders headers = response.getHeaders();
				String getUrl = headers.getLocation().toString();

				// Get request to obtain route
				log.info("Sendind GET request to AIT!");
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
				
				String logGrcMessage = "GRC message: \nDistance: " + resp.getDistance() + "\nTravelTime: " + 
						resp.getTravelTime() + "\nRouteSize: " + resp.getRoute().size();
				
				log.info(logGrcMessage);

				return resp;
			} else {
				log.info("The route is not for Vienna ---> Send it to MoBaaS!");
				// TODO send through rabbit
				
				String fromRequest = "" + r.getFrom().getLatitude() + "," + r.getFrom().getLongitude();
				String toRequest = "" + r.getTo().getLatitude() + "," + r.getTo().getLongitude();
				String modeRequest = "";
				
				if (r.getTransportationMode().equalsIgnoreCase("foot"))
					modeRequest = "WALK";
				else if (r.getTransportationMode().equalsIgnoreCase("bike") || r.getTransportationMode().equalsIgnoreCase("bicycle"))
					modeRequest = "BICYCLE";
				
				
				ServiceExecutionTaskResponse response = enablerLogic.invokeService(
					new ServiceExecutionTaskInfo(
						"routingServiceTask", routingServiceInfo, props.getEnablerName(), 
						Arrays.asList(
							new ServiceParameter("from", fromRequest),
							new ServiceParameter("to", toRequest), 
							new ServiceParameter("mode", modeRequest)
						)
					)
				);

//                             log.info(response.toString());



				try {
					ObjectMapper om = new ObjectMapper();
					om.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
					  .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

					GrcResponse resp = om.readValue(response.getOutput()
							.replaceAll("=",":")
							.replaceAll("longitude",    "@c:\".WGS84Location\", longitude")
							, new TypeReference<GrcResponse>(){});
					return resp;

				} catch (IOException e) {
					log.error("Problem communicating with MoBaaS Routing Service!" + e.getMessage() );
					return new GrcResponse();
				}
			}
		}
		
		GrcResponse dummyResponse = new GrcResponse();
		dummyResponse.setAirQualityRating(1.0);
		dummyResponse.setDistance(1.0);
		dummyResponse.setTravelTime(1.0);
		dummyResponse.setRoute(new ArrayList<Waypoint>());
		return dummyResponse;
	}
	
	
	private boolean isInVienna(WGS84Location loc) {
		double lat = loc.getLatitude();
		double lon = loc.getLongitude();
		if(lat >= 48.082850 && lat <= 48.330444 && lon >= 16.238929 && lon <= 16.560841) 
			return true;
		else 
			return false;
	}

}
