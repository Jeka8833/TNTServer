package com.jeka8833.tntserver.repositories;

import com.jeka8833.tntserver.models.TNTClientPlayerModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TNTClientPlayerRepository extends CrudRepository<TNTClientPlayerModel, UUID> {

    @Query(value = "INSERT INTO \"TC_Players\" (\"user\", \"version\", \"timeLogin\", \"blockModules\") VALUES " +
            "(:#{#playerModel.user}, :#{#playerModel.version}, CURRENT_TIMESTAMP, :#{#playerModel.blockModules}) " +
            "ON CONFLICT (\"user\") DO UPDATE SET \"version\" = EXCLUDED.\"version\", " +
            "\"timeLogin\" = CURRENT_TIMESTAMP, \"blockModules\" = EXCLUDED.\"blockModules\"", nativeQuery = true)
    void upsert(TNTClientPlayerModel playerModel);

}
