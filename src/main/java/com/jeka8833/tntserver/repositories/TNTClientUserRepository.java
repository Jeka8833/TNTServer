package com.jeka8833.tntserver.repositories;

import com.jeka8833.tntserver.models.TNTClientUserModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TNTClientUserRepository extends CrudRepository<TNTClientUserModel, UUID> {
}
