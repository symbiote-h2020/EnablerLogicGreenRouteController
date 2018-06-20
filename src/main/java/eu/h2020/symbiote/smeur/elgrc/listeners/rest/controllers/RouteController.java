package eu.h2020.symbiote.smeur.elgrc.listeners.rest.controllers;

import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.smeur.elgrc.dto.FilterRequest;
import eu.h2020.symbiote.smeur.elgrc.listeners.rest.interfaces.IRoute;
import eu.h2020.symbiote.smeur.elgrc.repositories.RouteRepository;
import eu.h2020.symbiote.smeur.elgrc.repositories.entities.RoutePoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class RouteController implements IRoute {

	private static final Logger log = LoggerFactory.getLogger(RouteController.class);


	@Autowired
	private RouteRepository routeRepo;

	@Override
	public Map<Long, List<RoutePoint>> getAllRoutes() {
		return routeRepo.findAll().stream().collect(Collectors.groupingBy(RoutePoint::getRouteId));
	}

	@Override
	public Map<Long, List<RoutePoint>> getRouteByTimestamp(@RequestBody FilterRequest filter)
			throws InvalidArgumentsException {

		if (filter.begin == null || filter.end == null){
			throw new InvalidArgumentsException();
		}

		return routeRepo.findByTimestampBetween(filter.begin, filter.end).stream().collect(Collectors.groupingBy(RoutePoint::getRouteId));
	}
}
