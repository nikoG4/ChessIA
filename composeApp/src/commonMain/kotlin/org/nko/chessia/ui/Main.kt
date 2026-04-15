package org.nko.chessia.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.nko.chessia.models.AIProvider
import org.nko.chessia.services.GameService

@Composable
fun GameModeSelector(
    onModeSelected: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    val gameModes = listOf(
        "🧑 Player vs 🧑 Player",
        "🧑 Player vs 🤖 IA",
        "🤖 IA vs 🤖 IA",
        "🤖 IA vs 🧑🤖 Asistido",
        "🧑🤖 Asistido vs 🧑🤖 Asistido",
        "🌐 Multiplayer Online"
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E))
    ) {
        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(20.dp)
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(50))
        ) {
            Text("⚙️", fontSize = 32.sp, color = Color.Cyan)
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("ChessIA", fontSize = 48.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Elige modo de juego", fontSize = 28.sp, color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))

            gameModes.forEach { mode ->
                Button(
                    onClick = { onModeSelected(mode) },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.width(300.dp).height(50.dp).padding(vertical = 4.dp)
                ) {
                    Text(mode, color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun GameSettingsScreen(
    selectedMode: String,
    onBack: () -> Unit,
    onStartGame: (Boolean, Int, String?, AIProvider?, String?) -> Unit
) {
    var withTimer by remember { mutableStateOf(false) }
    var timerMinutes by remember { mutableStateOf("5") }
    var difficulty by remember { mutableStateOf("Media") }
    var roomIdInput by remember { mutableStateOf("") }

    val isMultiplayer = selectedMode.contains("Multiplayer")
    val needsDifficulty = !selectedMode.contains("🧑 Player vs 🧑 Player") && !selectedMode.contains("Configuración") && !isMultiplayer
    val needsAIConfig = selectedMode.contains("🤖 IA") || selectedMode.contains("Configuración")
    val needsTimer = selectedMode.contains("🧑 Player vs 🧑 Player")

    val aiOptions = remember { 
        GameService().getAIProviders() + AIProvider(
            name = "Personalizado",
            endpoint = "https://tu-api.com/v1/chat",
            method = "POST",
            headers = mapOf("Content-Type" to "application/json", "Authorization" to "Bearer {{apiKey}}"),
            bodyTemplate = "{\"model\": \"gpt-4\", \"messages\": [{\"role\": \"user\", \"content\": \"FEN: {{fen}}\"}]}",
            extract = org.nko.chessia.models.Extractor(type = "jsonpath", path = "$.choices[0].message.content"),
            apiKey = ""
        )
    }
    var selectedAI by remember { mutableStateOf(aiOptions.firstOrNull()) }
    var showAdvanced by remember { mutableStateOf(selectedMode.contains("Configuración")) }

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF1C1C1E)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(16.dp).fillMaxHeight().verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Configuración de Partida", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(selectedMode, color = Color.LightGray, fontSize = 16.sp)

            Divider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

            if (isMultiplayer) {
                OutlinedTextField(
                    value = roomIdInput,
                    onValueChange = { roomIdInput = it },
                    label = { Text("ID de la Sala (Opcional)", color = Color.LightGray) },
                    modifier = Modifier.fillMaxWidth(0.9f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray),
                    singleLine = true
                )
            }

            if (needsTimer) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = withTimer, onCheckedChange = { withTimer = it }, colors = CheckboxDefaults.colors(checkmarkColor = Color.White))
                    Text("⏱ ¿Con tiempo?", color = Color.White)
                }
            }

            if (withTimer && needsTimer) {
                OutlinedTextField(
                    value = timerMinutes,
                    onValueChange = { if (it.all { c -> c.isDigit() }) timerMinutes = it },
                    label = { Text("Minutos por jugador", color = Color.LightGray) },
                    modifier = Modifier.width(200.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray),
                    singleLine = true
                )
            }

            if (needsDifficulty) {
                Text("🎯 Dificultad:", color = Color.White, fontWeight = FontWeight.SemiBold)
                DropdownMenuBox(
                    options = listOf("Fácil", "Media", "Difícil"),
                    selected = difficulty,
                    onSelected = { difficulty = it },
                    labelProvider = { it }
                )
            }

            if (needsAIConfig) {
                Divider(color = Color.Gray.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))
                Text("🎮 Configuración de IA:", color = Color.White, fontWeight = FontWeight.SemiBold)
                DropdownMenuBox(
                    options = aiOptions,
                    selected = selectedAI,
                    onSelected = { selectedAI = it },
                    labelProvider = { it.name }
                )

                selectedAI?.let { ai ->
                    OutlinedTextField(
                        value = ai.apiKey,
                        onValueChange = { newKey -> selectedAI = selectedAI?.copy(apiKey = newKey) },
                        label = { Text("API Key para ${ai.name}", color = Color.LightGray) },
                        colors = TextFieldDefaults.outlinedTextFieldColors(textColor = Color.White, focusedBorderColor = Color.White, unfocusedBorderColor = Color.Gray),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    )

                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(if (showAdvanced) "🔼 Ocultar Avanzado" else "🔽 Ver Configuración del Request", color = Color.Cyan)
                    }

                    if (showAdvanced) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth(0.9f)) {
                            ConfigField("Endpoint", ai.endpoint) { selectedAI = selectedAI?.copy(endpoint = it) }
                            ConfigField("Método", ai.method) { selectedAI = selectedAI?.copy(method = it) }
                            
                            val headersJson = remember(ai.headers) { 
                                ai.headers.entries.joinToString(",\n") { "\"${it.key}\": \"${it.value}\"" }
                                    .let { "{\n$it\n}" }
                            }
                            ConfigField("Headers (JSON)", headersJson, singleLine = false) { newValue ->
                                try {
                                    val newMap = newValue.trim().removeSurrounding("{", "}")
                                        .split(",")
                                        .filter { it.contains(":") }
                                        .associate {
                                            val parts = it.split(":")
                                            parts[0].trim().removeSurrounding("\"") to parts[1].trim().removeSurrounding("\"")
                                        }
                                    selectedAI = selectedAI?.copy(headers = newMap)
                                } catch (e: Exception) { }
                            }
                            ConfigField("Body Template", ai.bodyTemplate, singleLine = false) { selectedAI = selectedAI?.copy(bodyTemplate = it) }
                            
                            Text("Extracción de Respuesta:", color = Color.White, fontSize = 14.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { selectedAI = selectedAI?.copy(extract = ai.extract.copy(type = "jsonpath")) },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = if (ai.extract.type == "jsonpath") Color.Cyan else Color.DarkGray)
                                ) { Text("JSONPath", fontSize = 10.sp) }
                                Button(
                                    onClick = { selectedAI = selectedAI?.copy(extract = ai.extract.copy(type = "regex")) },
                                    colors = ButtonDefaults.buttonColors(backgroundColor = if (ai.extract.type == "regex") Color.Cyan else Color.DarkGray)
                                ) { Text("Regex", fontSize = 10.sp) }
                            }
                            
                            if (ai.extract.type == "jsonpath") {
                                ConfigField("JSON Path (e.g. $.move)", ai.extract.path ?: "") { 
                                    selectedAI = selectedAI?.copy(extract = ai.extract.copy(path = it)) 
                                }
                            } else {
                                ConfigField("Regex Pattern", ai.extract.pattern ?: "") { 
                                    selectedAI = selectedAI?.copy(extract = ai.extract.copy(pattern = it)) 
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    onStartGame(
                        withTimer,
                        timerMinutes.toIntOrNull() ?: 5,
                        if (needsDifficulty) difficulty else null,
                        selectedAI,
                        if (isMultiplayer) roomIdInput.trim().ifEmpty { null } else null
                    )
                },
                modifier = Modifier.fillMaxWidth(0.7f).height(50.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF007AFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("INICIAR PARTIDA", color = Color.White, fontWeight = FontWeight.Bold)
            }

            TextButton(onClick = onBack) {
                Text("⬅ Volver al menú", color = Color.LightGray)
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
fun ConfigField(label: String, value: String, singleLine: Boolean = true, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.LightGray, fontSize = 12.sp) },
        colors = TextFieldDefaults.outlinedTextFieldColors(
            textColor = Color.White,
            focusedBorderColor = Color.Cyan,
            unfocusedBorderColor = Color.Gray,
            cursorColor = Color.Cyan
        ),
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth(),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp)
    )
}

@Composable
fun <T> DropdownMenuBox(
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    labelProvider: (T) -> String,
    placeholder: String = "Seleccionar",
    modifier: Modifier = Modifier.width(200.dp)
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Button(
            onClick = { expanded = true },
            modifier = modifier,
            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF2C2C2E))
        ) {
            Text(labelProvider(selected ?: return@Button), color = Color.White)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(onClick = { onSelected(option); expanded = false }) {
                    Text(labelProvider(option))
                }
            }
        }
    }
}
