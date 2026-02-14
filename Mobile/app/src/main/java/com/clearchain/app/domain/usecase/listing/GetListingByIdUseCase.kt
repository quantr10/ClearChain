package com.clearchain.app.domain.usecase.listing

import com.clearchain.app.domain.model.Listing
import com.clearchain.app.domain.repository.ListingRepository
import com.clearchain.app.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class GetListingByIdUseCase @Inject constructor(
    private val listingRepository: ListingRepository
) {
    operator fun invoke(id: String): Flow<Resource<Listing>> = flow {
        try {
            emit(Resource.Loading())

            val result = listingRepository.getListingById(id)

            if (result.isSuccess) {
                emit(Resource.Success(result.getOrNull()!!))
            } else {
                emit(Resource.Error(result.exceptionOrNull()?.message ?: "Failed to get listing"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "An unexpected error occurred"))
        }
    }
}