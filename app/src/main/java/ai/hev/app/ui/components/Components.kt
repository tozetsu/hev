package ai.hev.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ai.hev.app.ui.theme.BarTrack
import ai.hev.app.ui.theme.Teal

@Composable
fun ProbabilityBar(
    label: String,
    probability: Double,
    highlighted: Boolean,
    modifier: Modifier = Modifier,
) {
    val pct = (probability * 100).coerceIn(0.0, 100.0)
    val anim by animateFloatAsState(targetValue = pct.toFloat() / 100f, label = "bar")
    val barColor = if (highlighted) Teal else MaterialTheme.colorScheme.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlighted) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "${formatFixed(pct, decimals = 1)}%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = if (highlighted) Teal else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(BarTrack.copy(alpha = 0.55f)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(anim.coerceIn(0f, 1f))
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
fun MetaChip(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
