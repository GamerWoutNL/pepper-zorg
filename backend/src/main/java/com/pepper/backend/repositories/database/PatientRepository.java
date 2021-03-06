package com.pepper.backend.repositories.database;

import com.pepper.backend.model.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PatientRepository extends MongoRepository<Patient, String> {

}
