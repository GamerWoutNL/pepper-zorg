package com.pepper.care.order.presentation.viewmodels

import android.util.Log
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.*
import androidx.navigation.findNavController
import com.aldebaran.qi.sdk.`object`.conversation.Phrase
import com.pepper.care.R
import com.pepper.care.common.AppResult
import com.pepper.care.common.ClickCallback
import com.pepper.care.common.DialogCallback
import com.pepper.care.common.DialogUtil
import com.pepper.care.common.entities.InformUserRecyclerItem
import com.pepper.care.common.entities.PlatformMealsResponse
import com.pepper.care.common.entities.RecyclerAdapterItem
import com.pepper.care.common.usecases.GetPatientNameUseCaseUsingRepository
import com.pepper.care.core.services.robot.DynamicConcepts
import com.pepper.care.core.services.robot.RobotManager
import com.pepper.care.dialog.DialogConstants
import com.pepper.care.dialog.DialogRoutes
import com.pepper.care.dialog.common.usecases.GetAvailableScreensUseCaseUsingRepository
import com.pepper.care.order.common.usecases.GetPatientAllergiesUseCaseUsingRepository
import com.pepper.care.order.common.usecases.GetPlatformMealsUseCaseUsingRepository
import com.pepper.care.order.common.view.MealSliderItem
import com.pepper.care.order.common.view.SliderAdapterItem
import kotlinx.coroutines.launch
import org.joda.time.LocalDateTime
import java.util.*
import kotlin.collections.ArrayList

class OrderViewModelUsingUsecases(
    private val getName: GetPatientNameUseCaseUsingRepository,
    private val getPlatformMealsUseCaseUsingRepository: GetPlatformMealsUseCaseUsingRepository,
    private val getPatientAllergiesUseCaseUsingRepository: GetPatientAllergiesUseCaseUsingRepository,
    private val getAvailableScreens: GetAvailableScreensUseCaseUsingRepository
) : ViewModel(), OrderViewModel {

    override val recyclerList = MutableLiveData<ArrayList<SliderAdapterItem>>(arrayListOf())
    override val recyclerVisibility: MutableLiveData<Boolean> = MutableLiveData(false)
    override val progressVisibility: MutableLiveData<Boolean> = MutableLiveData(true)
    override val recyclerSpanCount: MutableLiveData<Int> = MutableLiveData(3)

    override val meal: MutableLiveData<MealSliderItem> = MutableLiveData()
    override val orderText: String =
        "Het maaltijd menu, ${LocalDateTime().toString("EEEE d MMMM", Locale("nl"))}"

    private val fetchedName: MutableLiveData<String> =
        MutableLiveData(DialogConstants.DIALOG_MOCK_NAME)
    private val fetchedAvailableScreens: MutableLiveData<IntArray> =
        MutableLiveData(intArrayOf(0, 0, 0))

    override fun onStart() {
        fetchPatientDetails()
        showProgressView(true)
        viewModelScope.launch {
            when (val result = getPlatformMealsUseCaseUsingRepository.invoke()) {
                is AppResult.Success -> {
                    if (result.successData.isNotEmpty()) {
                        val responseList = result.successData

                        responseList.forEach { item ->
                            recyclerList.value!!.add(
                                MealSliderItem(
                                    item.id,
                                    item.name,
                                    item.description,
                                    item.allergies,
                                    item.calories,
                                    item.source
                                )
                            )
                        }

                        addDynamicContents(recyclerList.value!!)
                    }
                    showProgressView(false)
                }
                is AppResult.Error -> {
                    result.exception.message
                    showProgressView(false)
                }
            }
            when (val result = getAvailableScreens.invoke()) {
                is AppResult.Success -> {
                    fetchedAvailableScreens.postValue(result.successData)
                }
                is AppResult.Error -> result.exception.message
            }
        }
    }

    private fun addDynamicContents(list: List<SliderAdapterItem>) {
        val mealPhrases: ArrayList<Phrase> = ArrayList()
        list.forEach {
            if (it.getViewType() == SliderAdapterItem.ViewTypes.MEAL) {
                it as MealSliderItem
                mealPhrases.add(Phrase(it.name))
            }
        }
        RobotManager.addDynamicContents(DynamicConcepts.MEALS, mealPhrases)
    }

    override val adapterClickedListener: ClickCallback<SliderAdapterItem> =
        object : ClickCallback<SliderAdapterItem> {

            override fun onClicked(view: View, item: SliderAdapterItem) {
                when (item.getViewType()) {
                    SliderAdapterItem.ViewTypes.MEAL -> {
                        Log.d(OrderViewModelUsingUsecases::class.simpleName, "Clicked on Item")

                        this@OrderViewModelUsingUsecases.meal.apply {
                            value = item as MealSliderItem
                        }
                    }
                }
            }
        }

    private fun showProgressView(boolean: Boolean) {
        this@OrderViewModelUsingUsecases.progressVisibility.apply { value = boolean }
        this@OrderViewModelUsingUsecases.recyclerVisibility.apply { value = !boolean }
    }

    override fun onBackPress(view: View) {
        view.findNavController().popBackStack()
    }

    private fun fetchPatientDetails() {
        viewModelScope.launch {
            when (val result = getName.invoke()) {
                is AppResult.Success -> {
                    fetchedName.apply { value = result.successData }
                    RobotManager.addDynamicContents(
                        DynamicConcepts.NAME,
                        listOf(Phrase(result.successData))
                    )
                }
                is AppResult.Error -> result.exception.message
            }
        }
    }
}