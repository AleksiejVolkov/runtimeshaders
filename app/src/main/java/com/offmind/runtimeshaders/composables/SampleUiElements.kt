package com.offmind.runtimeshaders.composables

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.offmind.runtimeshaders.R
import kotlinx.coroutines.delay

@Composable
fun LoginForm(
    onLogin: (success: Boolean) -> Unit = {}
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var error: String? by remember { mutableStateOf(null) }
    var success: Boolean by remember { mutableStateOf(false) }
    var loading: Boolean by remember { mutableStateOf(false) }
    var steps by remember { mutableStateOf(0) }

    LaunchedEffect(loading) {
        if (loading) {
            delay(2300)
            success = true
            onLogin(success)
            loading = false
            if (!success) {
                error = "Invalid username or password"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {

        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Login", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(text = "Username") },
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(text = "Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                isError = error != null
            )

            Button(
                onClick = {
                    steps++
                    loading = true
                    error = null

                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp)
                    )
                } else
                    Text(text = "Login")
            }
        }
    }
}

@Composable
fun MemoryCard(
    onClose: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)) {
                Text(text = "August 25, 2024", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    modifier = Modifier.size(24.dp),
                    onClick = {
                        onClose()
                    }
                ) {
                    Icon(painter = painterResource(R.drawable.flame), contentDescription = "Close")
                }
            }
            Box(
                modifier = Modifier.size(300.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color = Color.White)
                    .padding(12.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.sample_memory),
                    contentDescription = "Memory Card",
                    modifier = Modifier.size(300.dp)
                )
            }
            Text(
                text = "Today was incredible. The street performers were amazing, and I couldn't resist capturing their energy and talent. The sunset over the river was breathtaking—I’ve never seen such vibrant colors in the sky.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}