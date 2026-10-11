package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.viewmodel.AuraViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AuraViewModel,
    onNavigateBack: () -> Unit
) {
    val companionName by viewModel.companionName.collectAsState()
    val persona by viewModel.persona.collectAsState()

    var nameInput by remember { mutableStateOf(companionName) }
    var selectedPersona by remember { mutableStateOf(persona) }

    val personas = listOf("Sweet & Caring", "Playful & Teasing", "Romantic Poet", "Smart & Supportive")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Companion Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("settings_back_button")) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Companion Name", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("companion_name_input"),
                        singleLine = true
                    )
                    Button(
                        onClick = {
                            if (nameInput.isNotBlank()) {
                                viewModel.setCompanionName(nameInput)
                            }
                        },
                        modifier = Modifier.testTag("save_name_button")
                    ) {
                        Text("Save Name")
                    }
                }
            }

            Card(
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Personality & Persona", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    personas.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedPersona == p,
                                onClick = {
                                    selectedPersona = p
                                    viewModel.setPersona(p)
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(p, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Gemini API & Secrets Note", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "To enable live AI voice conversations and phone control, configure your GEMINI_API_KEY in the Secrets panel in AI Studio. Aura uses gemini-3.5-flash with function calling to control your device.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
