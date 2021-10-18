package org.wordpress.android.fluxc.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.OrderAddress.AddressType
import org.wordpress.android.fluxc.model.order.OrderIdentifier

@Entity
data class WCOrderModel(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val localSiteId: Int = 0,
    val remoteOrderId: Long = 0L, // The unique identifier for this order on the server
    val number: String = "", // The, order number to display to the user
    val status: String = "",
    val currency: String = "",
    val orderKey: String = "",
    val dateCreated: String = "", // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    val dateModified: String = "",// ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    val total: String = "", // Complete total, including taxes
    val totalTax: String = "", // The total amount of tax (from products, shipping, discounts, etc.)
    val shippingTotal: String = "", // The total shipping cost (excluding tax)
    val paymentMethod: String = "", // Payment method code e.g. 'cod' 'stripe'
    val paymentMethodTitle: String = "", // Displayable payment method e.g. 'Cash on delivery' 'Credit Card (Stripe)'
    val datePaid: String = "",
    val pricesIncludeTax: Boolean = false,
    val customerNote: String = "", // Note left by the customer during order submission
    val discountTotal: String = "",
    val discountCodes: String = "",
    val refundTotal: Double = 0.0, // The total refund value for this order (usually a negative number)
    val billingFirstName: String = "",
    val billingLastName: String = "",
    val billingCompany: String = "",
    val billingAddress1: String = "",
    val billingAddress2: String = "",
    val billingCity: String = "",
    val billingState: String = "",
    val billingPostcode: String = "",
    val billingCountry: String = "",
    val billingEmail: String = "",
    val billingPhone: String = "",
    val shippingFirstName: String = "",
    val shippingLastName: String = "",
    val shippingCompany: String = "",
    val shippingAddress1: String = "",
    val shippingAddress2: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPostcode: String = "",
    val shippingCountry: String = "",
    val lineItems: String = "",
    val shippingLines: String = "",
    val feeLines: String = "",
    val metaData: String = "",
) {

    companion object {
        private val gson by lazy { Gson() }
    }

    class ShippingLine {
        val id: Long? = null
        val total: String? = null

        @SerializedName("total_tax")
        val totalTax: String? = null

        @SerializedName("method_id")
        val methodId: String? = null

        @SerializedName("method_title")
        val methodTitle: String? = null
    }

    /**
     * Represents a fee line
     * We are reading only the name and the total, as the tax is already included in the order totalTax
     */
    class FeeLine {
        @SerializedName("name")
        val name: String? = null

        @SerializedName("total")
        val total: String? = null
    }

    class LineItem {
        val id: Long? = null
        val name: String? = null

        @SerializedName("parent_name")
        val parentName: String? = null

        @SerializedName("product_id")
        val productId: Long? = null

        @SerializedName("variation_id")
        val variationId: Long? = null
        val quantity: Float? = null
        val subtotal: String? = null
        val total: String? = null // Price x quantity

        @SerializedName("total_tax")
        val totalTax: String? = null
        val sku: String? = null
        val price: String? = null // The per-item price

        @SerializedName("meta_data")
        val metaData: List<WCMetaData>? = null

        class Attribute(val key: String?, val value: String?)

        fun getAttributeList(): List<Attribute> {
            return metaData?.filter {
                it.displayKey is String && it.displayValue is String
            }?.map {
                Attribute(it.displayKey, it.displayValue as String)
            } ?: emptyList()
        }
    }

    /**
     * Returns an [OrderIdentifier], representing a unique identifier for this [WCOrderModel].
     */
    fun getIdentifier() = OrderIdentifier(this)

    /**
     * Returns true if there are shipping details defined for this order,
     * which are different from the billing details.
     *
     * If no separate shipping details are defined, the billing details should be used instead,
     * as the shippingX properties will be empty.
     */
    fun hasSeparateShippingDetails() = shippingCountry.isNotEmpty()

    /**
     * Returns the billing details wrapped in a [OrderAddress].
     */
    fun getBillingAddress() = OrderAddress(this, AddressType.BILLING)

    /**
     * Returns the shipping details wrapped in a [OrderAddress].
     */
    fun getShippingAddress() = OrderAddress(this, AddressType.SHIPPING)

    /**
     * Deserializes the JSON contained in [lineItems] into a list of [LineItem] objects.
     */
    fun getLineItemList(): List<LineItem> {
        val responseType = object : TypeToken<List<LineItem>>() {}.type
        return gson.fromJson(lineItems, responseType) as? List<LineItem> ?: emptyList()
    }

    /**
     * Returns the order subtotal (the sum of the subtotals of each line item in the order).
     */
    fun getOrderSubtotal(): Double {
        return getLineItemList().sumByDouble { it.subtotal?.toDoubleOrNull() ?: 0.0 }
    }

    /**
     * Deserializes the JSON contained in [shippingLines] into a list of [ShippingLine] objects.
     */
    fun getShippingLineList(): List<ShippingLine> {
        val responseType = object : TypeToken<List<ShippingLine>>() {}.type
        return gson.fromJson(shippingLines, responseType) as? List<ShippingLine> ?: emptyList()
    }

    /**
     * Deserializes the JSON contained in [feeLines] into a list of [FeeLine] objects.
     */
    fun getFeeLineList(): List<FeeLine> {
        val responseType = object : TypeToken<List<FeeLine>>() {}.type
        return gson.fromJson(feeLines, responseType) as? List<FeeLine> ?: emptyList()
    }

    /**
     * Deserializes the JSON contained in [metaData] into a list of [WCMetaData] objects.
     */
    fun getMetaDataList(): List<WCMetaData> {
        val responseType = object : TypeToken<List<WCMetaData>>() {}.type
        return gson.fromJson(metaData, responseType) as? List<WCMetaData> ?: emptyList()
    }

    fun isMultiShippingLinesAvailable() = getShippingLineList().size > 1
}
