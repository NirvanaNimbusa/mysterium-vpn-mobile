package network.mysterium.navigation

import android.content.Intent
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.findNavController
import network.mysterium.vpn.R

enum class Screen {
    MAIN,
    FEEDBACK,
    PROPOSALS,
    PROPOSALS_COUNTRY_FILTER_LIST,
    PROPOSALS_PRICE_FILTER,
    PROPOSALS_QUALITY_FILTER,
    PROPOSALS_NODE_TYPE_FILTER,
    ACCOUNT,
    FORCE_UPDATE
}

fun navigateTo(navController: NavController, destination: Screen) {
    val to = when(destination) {
        Screen.MAIN -> R.id.action_go_to_vpn_screen
        Screen.FEEDBACK -> R.id.action_go_to_feedback_screen
        Screen.PROPOSALS -> R.id.action_go_to_proposals_screen
        Screen.PROPOSALS_COUNTRY_FILTER_LIST -> R.id.action_go_to_proposals_country_filter_list_screen
        Screen.PROPOSALS_PRICE_FILTER -> R.id.action_go_to_proposals_price_filter_screen
        Screen.PROPOSALS_QUALITY_FILTER -> R.id.action_go_to_proposals_quality_filter_screen
        Screen.PROPOSALS_NODE_TYPE_FILTER -> R.id.action_go_to_proposals_node_type_filter_screen
        Screen.ACCOUNT -> R.id.action_go_to_account_screen
        Screen.FORCE_UPDATE -> R.id.action_go_to_feedback_screen
    }
    navController.navigate(to)
}

fun navigateTo(view: View, destination: Screen) {
    val navController = view.findNavController()
    navigateTo(navController, destination)
}

fun Fragment.onBackPress(cb: () -> Unit) {
    val callback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            cb()
        }
    }
    this.requireActivity().onBackPressedDispatcher.addCallback(this.viewLifecycleOwner, callback)
}

// Default back behaviour fully closes app, but we only want to minimize it.
// To do so we emulate home button press.
fun Fragment.emulateHomePress() {
    val startMain = Intent(Intent.ACTION_MAIN)
    startMain.addCategory(Intent.CATEGORY_HOME)
    startMain.flags = Intent.FLAG_ACTIVITY_NEW_TASK
    this.startActivity(startMain)
}
