package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.ai.LanguageMode
import com.example.ui.AiChatMessage
import com.example.ui.MainViewModel
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.NavyDark
import com.example.ui.theme.NavyLightSurface
import com.example.ui.theme.OceanBlue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AiAssistantScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val activeLang by viewModel.aiLanguageMode.collectAsStateWithLifecycle()

    var inputPrompt by remember { mutableStateOf("") }

    val presetQueries = listOf(
        "🎙️ Voice Sale: '10 biscuit sale hui'" to "Aaj 10 packet biscuit ki sale hui",
        "🔮 AI Demand Forecast" to "Konsa stock jaldi khatam hoga? Demand forecast dikhao",
        "📊 Net Profit & Operating Expenses" to "Aaj ka total sale, COGS, expenses aur net profit kitna hua?",
        "📒 Customer Udhaar Receivables" to "Customer ka total Udhaar aur pending balance kitna hai?",
        "⚠️ Low Stock Alert" to "Konsa stock khatam ya alert level par hai?",
        "🏢 Supplier Payables" to "Supplier ko kitna payment dena baaki hai?"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("ai_assistant_screen")
    ) {
        // AI Header & Language Bar
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = NavyDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(OceanBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "StockSaathi AI Advisor",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Senior Business Intelligence • Accurate Audits",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Icon(Icons.Default.Language, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Language Mode Selector Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(LanguageMode.values()) { mode ->
                        val isSelected = activeLang == mode
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setAiLanguageMode(mode) },
                            label = {
                                Text(
                                    when (mode) {
                                        LanguageMode.AUTO -> "Auto Detect"
                                        LanguageMode.ENGLISH -> "English"
                                        LanguageMode.HINDI -> "हिंदी"
                                        LanguageMode.HINGLISH -> "Hinglish"
                                    },
                                    fontSize = 11.sp
                                )
                            },
                            shape = RoundedCornerShape(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Preset Business Query Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetQueries) { (label, prompt) ->
                AssistChip(
                    onClick = { viewModel.sendAiUserQuery(prompt) },
                    label = { Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Messages List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                ChatMessageItem(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Query Input Field
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputPrompt,
                onValueChange = { inputPrompt = it },
                placeholder = { Text("Ask in Hindi, English or Hinglish...") },
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_query_input"),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )

            IconButton(
                onClick = {
                    if (inputPrompt.isNotBlank()) {
                        val q = inputPrompt
                        inputPrompt = ""
                        viewModel.sendAiUserQuery(q)
                    }
                },
                enabled = inputPrompt.isNotBlank(),
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(if (inputPrompt.isNotBlank()) NavyDark else Color.LightGray)
                    .testTag("send_ai_query_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(message: AiChatMessage) {
    val isUser = message.isUser
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            if (!isUser) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(NavyDark),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(8.dp))
            }

            Card(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (isUser) 14.dp else 2.dp,
                    bottomEnd = if (isUser) 2.dp else 14.dp
                ),
                colors = CardDefaults.cardColors(
                    containerColor = if (isUser) OceanBlue else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isUser) 1.dp else 2.dp),
                modifier = Modifier.testTag(if (isUser) "user_message_bubble" else "ai_message_bubble")
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.message,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 22.sp,
                            fontFamily = if (!isUser && message.message.contains("|")) FontFamily.Monospace else FontFamily.Default
                        ),
                        color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeFormat.format(Date(message.timestamp)),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = if (isUser) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isUser) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(OceanBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = OceanBlue, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
