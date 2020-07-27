package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsMapper
import org.wordpress.android.fluxc.model.leaderboards.WCProductLeaderboardsModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.orderstats.OrderStatsRestClient.OrderStatsApiUnit
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WCLeaderboardsStore @Inject constructor(
    private val restClient: LeaderboardsRestClient,
    private val productStore: WCProductStore,
    private val mapper: WCProductLeaderboardsMapper,
    private val coroutineEngine: CoroutineEngine
) {
    suspend fun fetchProductLeaderboards(
        site: SiteModel,
        unit: OrderStatsApiUnit? = null,
        queryTimeRange: LongRange? = null,
        quantity: Int? = null
    ): WooResult<WCProductLeaderboardsModel> =
            coroutineEngine.withDefaultContext(AppLog.T.API, this, "fetchLeaderboards") {
                fetchAllLeaderboards(site, unit, queryTimeRange, quantity)
                        .model
                        ?.firstOrNull { it.type == PRODUCTS }
                        ?.run { WooResult(mapper.map(this, site, productStore)) }
                        ?: WooResult(WooError(GENERIC_ERROR, UNKNOWN))
            }

    private suspend fun fetchAllLeaderboards(
        site: SiteModel,
        unit: OrderStatsApiUnit? = null,
        queryTimeRange: LongRange? = null,
        quantity: Int? = null
    ): WooResult<List<LeaderboardsApiResponse>> =
            with(restClient.fetchLeaderboards(site, unit, queryTimeRange, quantity)) {
                return when {
                    isError -> WooResult(error)
                    result != null -> WooResult(result.toList())
                    else -> WooResult(WooError(GENERIC_ERROR, UNKNOWN))
                }
            }
}
