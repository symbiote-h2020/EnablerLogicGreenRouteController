package eu.h2020.symbiote.smeur.elgrc.listeners.rest.interfaces;

import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.smeur.elgrc.commons.Constants;
import eu.h2020.symbiote.smeur.elgrc.dto.FilterRequest;
import eu.h2020.symbiote.smeur.elgrc.repositories.entities.RoutePoint;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import java.util.List;
import java.util.Map;

public interface IRoute {

	@GetMapping(value = Constants.GET_ALL_ROUTES)
	Map<Long, List<RoutePoint>> getAllRoutes();

	@PostMapping(value = Constants.GET_ROUTES_FILTERED, consumes = "application/json")
	Map<Long, List<RoutePoint>> getRouteByTimestamp(@RequestBody FilterRequest filters)
			throws InvalidArgumentsException;
}
