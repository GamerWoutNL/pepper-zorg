package com.pepper.care.order.repo

import com.pepper.care.common.repo.AppPreferencesRepository
import com.pepper.care.core.services.platform.entities.Allergy
import com.pepper.care.core.services.platform.entities.PlatformMeal
import com.pepper.care.core.services.platform.entities.PlatformMessageBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import org.joda.time.Instant
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.roundToInt

interface OrderRepository {
    suspend fun fetchMeals(): StateFlow<List<PlatformMeal>>
    suspend fun addOrder(name: String)
}

class OrderRepositoryImpl(
    private val appPreferences: AppPreferencesRepository
) : OrderRepository {

    override suspend fun fetchMeals(): StateFlow<List<PlatformMeal>> {
        val id = appPreferences.patientIdState.value

        appPreferences.updatePublishMessage(
            PlatformMessageBuilder.Builder()
                .person(PlatformMessageBuilder.Person.PATIENT)
                .personId(id)
                .task(PlatformMessageBuilder.Task.MEAL_ID)
                .taskId("1")
                .build()
        )

        delay(1500)

        return appPreferences.mealsState
    }


    private fun getMockMeals(): ArrayList<PlatformMeal> {
        return ArrayList<PlatformMeal>(
            listOf(
                PlatformMeal(
                    "0",
                    "Appelpannenkoeken",
                    "Vier heerlijke pannenkoeken, allemaal lekker gevuld met een mooie appel-compote. De pannenkoeken zijn vol van smaak en aangenaam zoet door de vulling van appel en rozijn. Een lekkere zoete maaltijd, ook leuk ter afwisseling van andere maaltijden.",
                    HashSet(listOf(Allergy.GLUTEN, Allergy.LACTOSE, Allergy.EGGS)),
                    960.toString(),
                    "https://www.kantenklaarmaaltijden.nl/312-large_default/appelpannenkoeken.jpg"
                ),
                PlatformMeal(
                    "1",
                    "Macaroni Bolognese en Italiaanse groenten",
                    "De macaroni Bolognese met Italiaanse groenten is een heerlijke Italiaanse pasta. De macaroni Bolognese is bereid met een saus van tomaten en is rijkelijk gevuld met rundergehakt. De Italiaanse groentemix bevat onder andere doperwtjes, worteltjes en stukjes sperzieboon.",
                    HashSet(listOf(Allergy.GLUTEN, Allergy.CELERY, Allergy.EGGS)),
                    420.toString(),
                    "https://www.kantenklaarmaaltijden.nl/39-large_default/macaroni-bolognese-en-italiaanse-groenten.jpg"
                ),
                PlatformMeal(
                    "2",
                    "Rookworst met jus voor een 'broodje warme worst",
                    "Met onze heerlijke rookworst heeft u z?????? een smakelijk 'broodje warme worst' klaar. Onze rookworst is bereid naar eigen recept, is kant en klaar en voorzien van een ruime hoeveelheid jus. Even verwarmen en op een broodje doen -wellicht wat mosterd erbij- en uw smakelijke lunchgerecht, gezonde snack of lekkere tussendoortje is klaar! Dit lunchgerecht bevat 80 gram vlees en 100 gram jus.",
                    HashSet(listOf(Allergy.GLUTEN, Allergy.LACTOSE)),
                    350.toString(),
                    "https://www.kantenklaarmaaltijden.nl/254-large_default/rookworst-met-jus-voor-een-broodje-warme-worst-.jpg"
                )
            )
        )
    }

    override suspend fun addOrder(name: String) {
        var taskId = "-2"
        appPreferences.updateMealOrderIdState(taskId)

        val meals: List<PlatformMeal> = appPreferences.mealsState.value
        val patientId = appPreferences.patientIdState.value

        var id: String? = null
        for (meal in meals) {
            if (meal.name.equals(name, ignoreCase = true)) {
                id = meal.id
            }
        }

        if (id != null) {
            appPreferences.updatePublishMessage(
                PlatformMessageBuilder.Builder()
                    .person(PlatformMessageBuilder.Person.PATIENT)
                    .personId(patientId)
                    .task(PlatformMessageBuilder.Task.MEAL_ORDER_MEAL_ID)
                    .taskId("-1")
                    .data(id)
                    .build()
            )

            for (i in 0..100) {
                taskId = appPreferences.mealOrderIdState.value

                if (taskId != "-2") {
                    break
                }

                delay(20)
            }

            appPreferences.updatePublishMessage(
                PlatformMessageBuilder.Builder()
                    .person(PlatformMessageBuilder.Person.PATIENT)
                    .personId(patientId)
                    .task(PlatformMessageBuilder.Task.MEAL_ORDER_TIMESTAMP)
                    .taskId(taskId)
                    .data("${(Instant.now().millis / 1000.0).toInt()}")
                    .build()
            )
        }
    }

}