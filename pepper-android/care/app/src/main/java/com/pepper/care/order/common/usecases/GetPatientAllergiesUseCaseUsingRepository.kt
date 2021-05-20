package com.pepper.care.order.common.usecases

import com.pepper.care.common.AppResult
import com.pepper.care.common.repo.PatientRepository

interface GetPatientAllergiesUseCase {
    suspend operator fun invoke() : AppResult<String>
}

class GetPatientAllergiesUseCaseUsingRepository(
    private val patientRepository: PatientRepository
) : GetPatientAllergiesUseCase {

    override suspend fun invoke() : AppResult<String> {
        return patientRepository.fetchAllergies()
    }
}