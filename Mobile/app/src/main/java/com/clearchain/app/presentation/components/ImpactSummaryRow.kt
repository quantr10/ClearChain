package com.clearchain.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EnergySavingsLeaf
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.clearchain.app.R
import com.clearchain.app.ui.theme.BrandGreen
import com.clearchain.app.ui.theme.BrandTeal

/**
 * The three-column impact row: food saved, the meals it stands for, and the CO2e it kept
 * out of landfill. The NGO home, the grocery home and the analytics screen all show the
 * same three numbers, so they show them through this one composable - otherwise the
 * columns drift apart in label, order or colour and the same figure starts looking like
 * three different measurements.
 *
 * Meals and CO2e are conversions of [kgSaved], not separate measurements. The API does the
 * converting (QuantityUnits) and hands all three figures over ready to display - never
 * recompute them here, or the app starts quoting a different impact than the server does.
 */
@Composable
fun ImpactSummaryRow(
    kgSaved: Int,
    mealsEstimate: Int,
    co2EstimateKg: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ImpactStatCell(
            icon = Icons.Default.Scale,
            label = stringResource(R.string.impact_food_saved),
            value = "${kgSaved}kg",
            color = BrandGreen,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(modifier = Modifier.height(64.dp))
        ImpactStatCell(
            icon = Icons.Default.Restaurant,
            label = stringResource(R.string.impact_meals),
            value = "$mealsEstimate",
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
        VerticalDivider(modifier = Modifier.height(64.dp))
        ImpactStatCell(
            icon = Icons.Default.EnergySavingsLeaf,
            label = stringResource(R.string.impact_co2),
            value = "${co2EstimateKg}kg",
            color = BrandTeal,
            modifier = Modifier.weight(1f)
        )
    }
}
