package monitoring

import org.springframework.data.mongodb.repository.MongoRepository

/**
 * Created by dimitri on 25/08/2014.
 */
public interface MonitoringRepository extends MongoRepository<MonitoringData, String> {
}
