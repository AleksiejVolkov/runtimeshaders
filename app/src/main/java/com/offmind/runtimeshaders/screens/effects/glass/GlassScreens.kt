package com.offmind.runtimeshaders.screens.effects.glass

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Ink = Color(0xF2FFFFFF)
private val InkSoft = Color(0xB3FFFFFF)

// three basic pieces of glass: a pill, a circle and a card
@Composable
fun GlassBasicsScreen() {
    GlassBackdrop(TealMood) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 36.dp),
            verticalArrangement = Arrangement.spacedBy(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(width = 200.dp, height = 68.dp).glass(cornerRadius = 34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Sign In", fontSize = 22.sp, fontWeight = FontWeight.Medium, color = Ink)
                }
                Box(
                    modifier = Modifier.size(68.dp).glass(cornerRadius = 34.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, null, Modifier.size(34.dp), tint = Ink)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .glass(cornerRadius = 28.dp)
                    .padding(24.dp)
            ) {
                Text("Glass Interface", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(
                    "Mostly transparent. Light tells you where it is.",
                    modifier = Modifier.width(240.dp).padding(top = 8.dp),
                    fontSize = 14.sp,
                    color = InkSoft
                )
            }
        }
    }
}

// the whole set: every element is the same Modifier.glass with different parameters
@Composable
fun GlassSetScreen() {
    var tab by remember { mutableIntStateOf(2) }
    var box by remember { mutableIntStateOf(0) }
    GlassBackdrop(TealMood) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 72.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            Column {
                Text("Glass", fontSize = 30.sp, fontWeight = FontWeight.SemiBold, color = GlassInk)
                Text("one shader · one light", fontSize = 14.sp, color = GlassInkSoft)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassTabBar(
                    tabs = listOf(
                        "Home" to Icons.Default.Home,
                        "Health" to Icons.Default.Favorite,
                        "Insights" to Icons.Default.Info,
                    ),
                    selected = tab,
                    onSelect = { tab = it },
                    modifier = Modifier.weight(1f)
                )
                GlassRoundButton(Icons.Default.Star, onClick = {}, size = 76.dp)
            }
            GlassSearchField("Ask anything", Icons.Default.Search, Modifier.fillMaxWidth())
            GlassSegmented(
                options = listOf("Inbox", "Draft"),
                selected = box,
                onSelect = { box = it },
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                GlassCard(Modifier.weight(1f).height(152.dp)) {
                    Text("Morning brief", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = GlassInk)
                    Text(
                        "3 meetings, a quiet afternoon and rain after six.",
                        fontSize = 13.sp,
                        color = GlassInkSoft,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GlassRoundButton(Icons.Default.Lock, onClick = {}, size = 69.dp)
                    GlassRoundButton(Icons.Default.Notifications, onClick = {}, size = 69.dp)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassPressable(
                    onClick = {},
                    modifier = Modifier.weight(1f).height(64.dp),
                    cornerRadius = 32.dp
                ) {
                    GlassLabel("Sign In")
                }
                GlassRoundButton(Icons.Default.KeyboardArrowUp, onClick = {})
            }
        }
    }
}
