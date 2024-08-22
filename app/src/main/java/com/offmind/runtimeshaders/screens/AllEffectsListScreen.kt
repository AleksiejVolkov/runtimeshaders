package com.offmind.runtimeshaders.screens

import android.media.MicrophoneInfo.Coordinate3F
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.offmind.runtimeshaders.R
import com.offmind.runtimeshaders.navigation.Route

@Composable
fun AllEffectsListScreen(
    paddingValues: PaddingValues,
    effects: List<EffectScreenData>,
    navController: NavController
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.BottomEnd
    ) {
        Image(
            painter = painterResource(id = R.drawable.main_background),
            contentDescription = "Sample Image",
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Image(
            modifier = Modifier.padding(16.dp),
            painter = painterResource(id = R.drawable.logo_label),
            contentDescription = "Sample Image"
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            effects.forEach {
                item {
                    EffectListItem(
                        effect = it,
                        onEffectSelected = {
                            navController.navigate(it)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun EffectListItem(
    effect: EffectScreenData,
    onEffectSelected: (Route) -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(Color(0x44000000))
        .clickable {
            onEffectSelected(effect.screenRoute)
        }
        .padding(16.dp)) {
        Text(
            text = effect.title,
            color = Color.White,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleLarge
        )
    }
}