package com.clearchain.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.data.remote.dto.CartItemData
import com.clearchain.app.domain.model.Listing
import com.clearchain.app.ui.theme.ButtonShape

// Screen-wide touch signal used to collapse an expanded cart stepper: every touch-down
// anywhere on screen bumps `tick`, and carries the position + root coordinate space so a
// listener can tell whether that touch landed on its own stepper (and should be ignored).
data class GlobalTouch(
    val tick: Int,
    val position: Offset?,
    val rootCoordinates: LayoutCoordinates?
)

/** Records touches for [GlobalTouch]; attach it to the screen's root with [trackGlobalTouch]. */
@Stable
class GlobalTouchTracker {
    internal var tick by mutableIntStateOf(0)
    internal var position by mutableStateOf<Offset?>(null)
    internal var rootCoordinates by mutableStateOf<LayoutCoordinates?>(null)

    val current: GlobalTouch get() = GlobalTouch(tick, position, rootCoordinates)
}

@Composable
fun rememberGlobalTouchTracker(): GlobalTouchTracker = remember { GlobalTouchTracker() }

fun Modifier.trackGlobalTouch(tracker: GlobalTouchTracker): Modifier = this
    .onGloballyPositioned { tracker.rootCoordinates = it }
    .pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val down = event.changes.firstOrNull { it.changedToDown() }
                if (down != null) {
                    tracker.position = down.position
                    tracker.tick++
                }
            }
        }
    }

/**
 * Add-to-cart button that turns into a quantity stepper once the listing is in the cart,
 * and collapses back to a quantity chip on the next touch outside the stepper.
 */
@Composable
fun ListingCartAction(
    listing: Listing,
    cartItem: CartItemData?,
    enabled: Boolean,
    globalTouch: GlobalTouch,
    onAddToCart: (String) -> Unit,
    onIncrementCartItem: (String) -> Unit,
    onDecrementCartItem: (String) -> Unit,
    onRemoveFromCart: (String) -> Unit,
    availableQuantity: Int = listing.quantity,
    /** This listing's cart call is in flight; [enabled] covers the other listings waiting on it. */
    loading: Boolean = false
) {
    var isExpanded by remember(listing.id) { mutableStateOf(false) }
    var expandTick by remember(listing.id) { mutableIntStateOf(0) }
    var stepperBounds by remember(listing.id) { mutableStateOf<Rect?>(null) }

    LaunchedEffect(globalTouch.tick) {
        if (isExpanded && globalTouch.tick != expandTick) {
            expandTick = globalTouch.tick
            val position = globalTouch.position
            val bounds = stepperBounds
            val touchedStepper = position != null && bounds != null && bounds.contains(position)
            if (!touchedStepper) {
                isExpanded = false
            }
        }
    }

    when {
        cartItem == null || cartItem.requestedQuantity <= 0 -> {
            ClearChainActionIconButton(
                icon = Icons.Default.Add,
                contentDescription = stringResource(R.string.cart_add_to_cart),
                onClick = {
                    onAddToCart(listing.id)
                    expandTick = globalTouch.tick
                    isExpanded = true
                },
                tint = MaterialTheme.colorScheme.primary,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                enabled = enabled && availableQuantity > 0,
                loading = loading
            )
        }
        isExpanded -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ClearChainQuantityStepper(
                    quantity = cartItem.requestedQuantity,
                    unit = listing.unit,
                    canIncrement = cartItem.requestedQuantity < availableQuantity,
                    enabled = enabled,
                    onDecrement = { onDecrementCartItem(listing.id) },
                    onIncrement = { onIncrementCartItem(listing.id) },
                    expanded = false,
                    buttonSize = 24.dp,
                    loading = loading,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        stepperBounds = globalTouch.rootCoordinates?.localBoundingBoxOf(coordinates)
                    }
                )
                ClearChainActionIconButton(
                    icon = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.cart_remove),
                    onClick = { onRemoveFromCart(listing.id) },
                    tint = MaterialTheme.colorScheme.error,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    enabled = enabled
                )
            }
        }
        else -> {
            Surface(
                onClick = {
                    expandTick = globalTouch.tick
                    isExpanded = true
                },
                modifier = Modifier.height(ClearChainButtonDefaults.Height).widthIn(min = 72.dp),
                enabled = enabled,
                shape = ButtonShape,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 3.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        "${cartItem.requestedQuantity} ${listing.unit}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
