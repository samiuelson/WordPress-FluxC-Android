package org.wordpress.android.fluxc.network.rest.wpcom.wc.pay

import com.google.gson.annotations.SerializedName

data class CapturePaymentApiResponse(
    @SerializedName("status") val status: String,
    @SerializedName("id") val paymentId: String
)
