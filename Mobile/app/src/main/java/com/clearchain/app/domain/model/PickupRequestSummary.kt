package com.clearchain.app.domain.model

/**
 * A pickup request's [PickupRequest.items] are the source of truth for what it moves; the
 * `listing*` fields on the request itself are only a summary the server derives from them
 * (see PickupRequestSummary on the API side).
 *
 * These helpers are the one place that knows how to read a request's contents, so screens
 * can't each invent their own rule.
 */

/**
 * The request's contents as displayable lines.
 *
 * Falls back to a single line rebuilt from the summary because the Room cache stores
 * requests without their items, so an offline list would otherwise show nothing.
 */
val PickupRequest.itemLines: List<PickupRequestItem>
    get() = items.ifEmpty {
        listOf(
            PickupRequestItem(
                id = id,
                requestedQuantity = requestedQuantity,
                listingTitle = listingTitle,
                listingCategory = listingCategory,
                listingExpiryDate = listingExpiryDate,
                listingUnit = listingUnit
            )
        )
    }

/**
 * Everything a person might type to find this request, lowercased. Searching only the
 * summary title would miss the individual products inside a multi-item request.
 */
val PickupRequest.searchText: String
    get() = buildString {
        append(listingTitle).append(' ')
        append(listingCategory).append(' ')
        append(groceryName).append(' ')
        append(ngoName).append(' ')
        notes?.let { append(it).append(' ') }
        items.forEach { item ->
            append(item.listingTitle).append(' ')
            append(item.listingCategory).append(' ')
        }
    }.lowercase()

/** Product names in the request, for exports and summaries. */
val PickupRequest.itemTitles: List<String>
    get() = itemLines.map { it.listingTitle }.filter { it.isNotBlank() }
