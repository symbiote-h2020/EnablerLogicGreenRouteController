package eu.h2020.symbiote.smeur.elgrc.repositories;

import eu.h2020.symbiote.smeur.elgrc.repositories.entities.RoutePoint;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface RouteRepository extends MongoRepository<RoutePoint, String> {

	List<RoutePoint> findByTimestampBetween(long begin, long end);

}
