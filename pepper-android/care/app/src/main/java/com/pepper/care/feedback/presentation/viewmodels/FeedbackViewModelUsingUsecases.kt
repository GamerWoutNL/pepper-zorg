package com.pepper.care.feedback.presentation.viewmodels

import android.app.Activity
import android.graphics.Typeface
import android.view.View
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.findNavController
import com.example.awesomedialog.*
import com.pepper.care.R
import com.pepper.care.dialog.DialogRoutes
import com.pepper.care.feedback.FeedbackCallback
import com.pepper.care.feedback.FeedbackConstants.FEEDBACK_MOCK_EXPLANATION
import com.pepper.care.feedback.common.usecases.AddPatientGivenHealthFeedbackUseCaseUsingRepository
import com.pepper.care.feedback.common.usecases.AddPatientHealthFeedbackUseCaseUsingRepository
import com.pepper.care.feedback.entities.FeedbackEntity
import kotlinx.coroutines.launch

class FeedbackViewModelUsingUsecases(
    private val feedbackType: AddPatientHealthFeedbackUseCaseUsingRepository,
    private val feedbackExplain: AddPatientGivenHealthFeedbackUseCaseUsingRepository
) : ViewModel(), FeedbackViewModel {

    override val headerText: String = "Hoe voelt u zich momenteel?"
    override val badFeedbackEntity: FeedbackEntity =
        FeedbackEntity(FeedbackEntity.FeedbackMessage.BAD)
    override val mediumFeedbackEntity: FeedbackEntity =
        FeedbackEntity(FeedbackEntity.FeedbackMessage.OKAY)
    override val goodFeedbackEntity: FeedbackEntity =
        FeedbackEntity(FeedbackEntity.FeedbackMessage.GOOD)

    override val cardClickedListener: FeedbackCallback =
        object : FeedbackCallback {
            override fun onClicked(view: View, type: FeedbackEntity.FeedbackMessage) {
                createDialog(view, type)
            }
        }

    private fun createDialog(view: View, type: FeedbackEntity.FeedbackMessage) {
        AwesomeDialog.build(view.context as Activity)
            .title("Klopt het onderstaande?", Typeface.DEFAULT_BOLD, R.color.black)
            .body("U gaf het volgend antwoord: ${type.text}, $FEEDBACK_MOCK_EXPLANATION.", null, R.color.black)
            .onPositive("Ja", R.color.green) {
                viewModelScope.launch {
                    feedbackType.invoke(type)
                    feedbackExplain.invoke(FEEDBACK_MOCK_EXPLANATION)
                }

                view.findNavController().navigate(
                    R.id.dialogFragment, bundleOf(
                        Pair<String, DialogRoutes>("ROUTE_TYPE", DialogRoutes.GOODBYE)
                    )
                )
            }
            .onNegative("Nee", R.color.red)
    }
}