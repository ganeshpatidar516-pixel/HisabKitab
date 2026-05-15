package com.ganesh.hisabkitabpro.ui.ai_pilot

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SuperAIButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    // Static shadow only: infinite transitions have caused rare OEM/GPU instability on home.

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(85.dp)
            .shadow(
                10.dp,
                RoundedCornerShape(24.dp),
                ambientColor = colorScheme.primary.copy(alpha = 0.45f),
                spotColor = colorScheme.primary.copy(alpha = 0.45f)
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = {}
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colorScheme.primaryContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.AutoAwesome, 
                    contentDescription = null, 
                    tint = colorScheme.onPrimaryContainer, 
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(
                        "SUPER AI ASSISTANT",
                        fontWeight = FontWeight.ExtraBold, 
                        fontSize = 20.sp, 
                        color = colorScheme.onPrimaryContainer
                    )
                    Text(
                        "Tap to open AI Assistant",
                        fontSize = 12.sp,
                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos, 
                contentDescription = null, 
                tint = colorScheme.onPrimaryContainer, 
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
