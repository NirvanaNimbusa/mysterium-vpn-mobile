/*
 * Copyright (C) 2020 The "mysteriumnetwork/mysterium-vpn-mobile" Authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package network.mysterium.wallet

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import io.intercom.android.sdk.Intercom
import io.intercom.android.sdk.UserAttributes
import kotlinx.coroutines.launch
import mysterium.GetBalanceRequest
import mysterium.RegisterIdentityRequest
import network.mysterium.logging.BugReporter
import network.mysterium.service.core.NodeRepository

enum class IdentityRegistrationStatus(val status: String) {
    UNKNOWN("Unknown"),
    REGISTERED("Registered"),
    UNREGISTERED("Unregistered"),
    IN_PROGRESS("InProgress"),
    REGISTRATION_ERROR("RegistrationError");

    companion object {
        fun parse(status: String): IdentityRegistrationStatus {
            return values().find { it.status == status } ?: UNKNOWN
        }
    }
}

@Entity(tableName = "identity")
class IdentityModel(
        @PrimaryKey
        val address: String,
        val channelAddress: String,
        var status: IdentityRegistrationStatus
) {
    // Identity is considered as registered if it's status RegisteredConsumer or InProgress since we support fast
    // identity registration flow which means there is no need to wait for actual registration.
    val registered: Boolean
        get() {
            return status == IdentityRegistrationStatus.REGISTERED || status == IdentityRegistrationStatus.IN_PROGRESS
        }

    val registrationFailed: Boolean
        get() {
            return status == IdentityRegistrationStatus.REGISTRATION_ERROR
        }
}

class BalanceModel(val balance: TokenModel)

class TokenModel(token: Double = 0.0) {
    var displayValue = ""
    var value = 0.00

    init {
        value = token
        displayValue = "%.3f MYSTT".format(value)
    }
}

class WalletViewModel(private val nodeRepository: NodeRepository, private val bugReporter: BugReporter) : ViewModel() {
    val balance = MutableLiveData<BalanceModel>()
    val identity = MutableLiveData<IdentityModel>()

    suspend fun load() {
        initListeners()
        loadIdentity()
        loadBalance()
    }

    fun needToTopUp(): Boolean {
        if (balance.value == null) {
            return false
        }
        return balance.value!!.balance.value < 0.01
    }

    fun isIdentityRegistered(): Boolean {
        val currentIdentity = identity.value ?: return false
        return currentIdentity.registered
    }

    suspend fun loadIdentity() {
        try {
            // Load node identity and it's registration status.
            val nodeIdentity = nodeRepository.getIdentity()
            val identityResult = IdentityModel(
                    address = nodeIdentity.address,
                    channelAddress = nodeIdentity.channelAddress,
                    status = IdentityRegistrationStatus.parse(nodeIdentity.registrationStatus)
            )
            identity.value = identityResult
            bugReporter.setUserIdentifier(nodeIdentity.address)
            Intercom.client().apply {
                registerUnidentifiedUser()
                val attrs = UserAttributes.Builder()
                        .withCustomAttribute("node_identity", nodeIdentity.address)
                        .build()
                updateUser(attrs)
            }
            Log.i(TAG, "Loaded identity ${nodeIdentity.address}, channel addr: ${nodeIdentity.channelAddress}, status: ${nodeIdentity.registrationStatus}")

            // Register identity if not registered or failed.
            if (!identityResult.registered) {
                if (identity.value != null) {
                    val req = RegisterIdentityRequest()
                    req.identityAddress = identity.value!!.address
                    nodeRepository.registerIdentity(req)
                }
            }
        } catch (e: Exception) {
            identity.value = IdentityModel(address = "", channelAddress = "", status = IdentityRegistrationStatus.REGISTRATION_ERROR)
            Log.e(TAG, "Failed to load account identity", e)
        }
    }

    fun generateChannelQRCode(channelAddress: String): Bitmap {
        val barcodeEncoder = BarcodeEncoder()
        return barcodeEncoder.encodeBitmap(channelAddress, BarcodeFormat.QR_CODE, 500, 500)
    }

    private suspend fun loadBalance() {
        if (identity.value == null) {
            return
        }
        val req = GetBalanceRequest()
        req.identityAddress = identity.value!!.address
        val balance = nodeRepository.balance(req)
        handleBalanceChange(balance)
    }

    private suspend fun initListeners() {
        nodeRepository.registerBalanceChangeCallback {
            handleBalanceChange(it)
        }

        nodeRepository.registerIdentityRegistrationChangeCallback {
            handleIdentityRegistrationChange(it)
        }
    }

    private fun handleIdentityRegistrationChange(status: String) {
        val currentIdentity = identity.value ?: return
        viewModelScope.launch {
            currentIdentity.status = IdentityRegistrationStatus.parse(status)
            identity.value = currentIdentity
        }
    }

    private fun handleBalanceChange(changedBalance: Double) {
        viewModelScope.launch {
            balance.value = BalanceModel(TokenModel(changedBalance))
        }
    }

    companion object {
        const val TAG = "WalletViewModel"
    }
}
