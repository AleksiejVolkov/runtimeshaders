package com.offmind.runtimeshaders.screens.effects

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun NavigationTestScreen(onLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFFF5F5F5)
    ) {
        LoginScreen(onLogin = onLogin)
    }
}

@Composable
fun NavigationTestFeedScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(brush = Brush.verticalGradient(
              colors = listOf(Color(0xFFE8EDFF), Color(0xFFBFC1C9))
        )),
    ) {
        FeedScreen()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginScreen(onLogin: () -> Unit) {
    var email by remember { mutableStateOf("demo@example.com") }
    var password by remember { mutableStateOf("password") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE7ECF5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFF315B96),
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Welcome back",
                color = Color(0xFF172033),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Sign in to continue",
                color = Color(0xFF687386),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 6.dp, bottom = 28.dp)
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null)
                },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                leadingIcon = {
                    Icon(Icons.Filled.Lock, contentDescription = null)
                },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = onLogin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(52.dp)
            ) {
                Text("OK")
            }
        }
    }
}

@Composable
private fun FeedScreen() {
    val items = remember {
        listOf(
            FeedItem("Morning brief", "Design review moved to 11:30. Three open notes need a decision."),
            FeedItem("Product update", "The new navigation prototype is ready for internal testing."),
            FeedItem("Team activity", "Mira commented on the shader transition thread."),
            FeedItem("Release note", "Build 42 adds a cleaner login flow and feed screen sample."),
            FeedItem("Reminder", "Check edge-to-edge behavior across the mock app screens.")
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(top = 44.dp, bottom = 8.dp)) {
                Text(
                    text = "News Feed",
                    color = Color(0xFF172033),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Today",
                    color = Color(0xFF687386),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
        items(items) { item ->
            FeedCard(item)
        }
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEAF1FF)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.title.take(1),
                    color = Color(0xFF315B96),
                    fontWeight = FontWeight.Bold
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    color = Color(0xFF172033),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = item.body,
                    color = Color(0xFF687386),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private data class FeedItem(
    val title: String,
    val body: String
)
