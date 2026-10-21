package com.vetpocket.app.ui.map

import android.location.Location
import com.yandex.mapkit.geometry.BoundingBox
import com.yandex.mapkit.geometry.Geometry
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.search.BusinessObjectMetadata
import com.yandex.mapkit.search.Response
import com.yandex.mapkit.search.SearchFactory
import com.yandex.mapkit.search.SearchManager
import com.yandex.mapkit.search.SearchManagerType
import com.yandex.mapkit.search.SearchOptions
import com.yandex.mapkit.search.SearchType
import com.yandex.mapkit.search.Session
import com.yandex.mapkit.GeoObject
import com.yandex.runtime.Error as MapKitError
import com.yandex.runtime.network.NetworkError

data class Clinic(
    val id: String,
    val name: String,
    val address: String,
    val phone: String?,
    val point: Point,
    /** null — в данных нет сведений о графике работы. */
    val isOpenNow: Boolean?,
    val workingText: String?,
    val distanceMeters: Double,
)

fun distanceMeters(a: Point, b: Point): Double {
    val out = FloatArray(1)
    Location.distanceBetween(a.latitude, a.longitude, b.latitude, b.longitude, out)
    return out[0].toDouble()
}

/** Поиск организаций по категории «Ветеринарные клиники» вокруг заданной точки. */
class ClinicSearcher {
    private val manager: SearchManager by lazy {
        SearchFactory.getInstance().createSearchManager(SearchManagerType.COMBINED)
    }
    private var session: Session? = null

    fun search(center: Point, onResult: (List<Clinic>) -> Unit, onFailure: (String) -> Unit) {
        session?.cancel()
        val dLat = 0.08
        val dLon = 0.14
        val box = BoundingBox(
            Point(center.latitude - dLat, center.longitude - dLon),
            Point(center.latitude + dLat, center.longitude + dLon),
        )
        val options = SearchOptions().apply {
            searchTypes = SearchType.BIZ.value
            resultPageSize = 40
        }
        session = manager.submit(
            "Ветеринарные клиники",
            Geometry.fromBoundingBox(box),
            options,
            object : Session.SearchListener {
                override fun onSearchResponse(response: Response) {
                    val clinics = response.collection.children
                        .mapNotNull { it.obj }
                        .mapNotNull { toClinic(it, center) }
                        .sortedBy { it.distanceMeters }
                    onResult(clinics)
                }

                override fun onSearchError(error: MapKitError) {
                    onFailure(
                        if (error is NetworkError) "Нет соединения с интернетом. Проверьте сеть и повторите."
                        else "Не удалось выполнить поиск клиник. Проверьте ключ MapKit и повторите."
                    )
                }
            },
        )
    }

    fun cancel() {
        session?.cancel()
        session = null
    }

    private fun toClinic(obj: GeoObject, center: Point): Clinic? {
        val meta = obj.metadataContainer.getItem(BusinessObjectMetadata::class.java) ?: return null
        val point = obj.geometry.firstOrNull()?.point ?: return null
        val hours = meta.workingHours
        return Clinic(
            id = meta.oid,
            name = meta.name,
            address = meta.address.formattedAddress,
            phone = meta.phones.firstOrNull()?.formattedNumber,
            point = point,
            isOpenNow = hours?.state?.isOpenNow,
            workingText = hours?.state?.text ?: hours?.text,
            distanceMeters = distanceMeters(center, point),
        )
    }
}
