package io.github.withlet11.digitalquadrant

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun AutoHoldSwitch(
    modifier: Modifier = Modifier, // Allow passing modifiers from the caller
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(start = 16.dp, end = 0.dp, top = 8.dp, bottom = 8.dp) // Add some padding
    ) {
        Column {
            Text(
                text = "AUTO",
                style = MaterialTheme.typography.bodySmall // Or your desired text style
            )
            Text(
                text = "HOLD",
                style = MaterialTheme.typography.bodySmall // Or your desired text style
            )

        }
        Switch(
            modifier = Modifier
                .scale(0.8f)
                .padding(2.dp),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xff000040),
                checkedTrackColor = Color(0xffd0d0e0),
                uncheckedThumbColor = Color(0xff8080c0),
                uncheckedTrackColor = Color(0xff000080),
            ),
            checked = isChecked,
            onCheckedChange = onCheckedChange,
        )
    }
}