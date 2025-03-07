package com.pacepdro.logkriptografi

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pacepdro.logkriptografi.ui.theme.LogKriptografiTheme
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LogKriptografiTheme {
                LogScreen()
            }
        }
    }
}

data class LogEntry(
    val timestamp: String,
    val text: String,
    val amount: String,
    val hash: String,
    val previousHash: String
)

@Composable
fun LogScreen() {
    val context = LocalContext.current
    val dataStore = remember { LogDataStore(context) }
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf(TextFieldValue("")) }
    var inputAmount by remember { mutableStateOf(TextFieldValue("")) }
    var searchQuery by remember { mutableStateOf(TextFieldValue("")) }
    var logEntries by remember { mutableStateOf(emptyList<LogEntry>()) }
    var showSearchPopup by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val quickAmounts = listOf("10000", "20000", "50000")
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(dataStore) {
        dataStore.logFlow.collect { logs ->
            logEntries = logs.mapNotNull {
                val parts = it.split("|")
                if (parts.size == 5) LogEntry(parts[0], parts[1], parts[2], parts[3], parts[4]) else null
            }
        }
    }

    val filteredLogs = logEntries.filter {
        it.text.contains(searchQuery.text, ignoreCase = true) ||
                it.amount.contains(searchQuery.text, ignoreCase = true)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Masukkan Teks Log") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = inputAmount,
            onValueChange = { if (it.text.all { char -> char.isDigit() }) inputAmount = it },
            label = { Text("Masukkan Nominal Uang") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            quickAmounts.forEach { amount ->
                Button(onClick = { inputAmount = TextFieldValue(amount) }) {
                    Text("Rp $amount")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (inputText.text.isNotBlank() && inputAmount.text.isNotBlank()) {
                val timestamp = dateFormat.format(Date())
                val previousHash = logEntries.lastOrNull()?.hash ?: "0"
                val hash = sha256("$timestamp-${inputText.text}-${inputAmount.text}-$previousHash")

                val newEntry = LogEntry(timestamp, inputText.text, inputAmount.text, hash, previousHash)
                logEntries = logEntries + newEntry
                inputText = TextFieldValue("")
                inputAmount = TextFieldValue("")

                scope.launch {
                    dataStore.saveLog("${newEntry.timestamp}|${newEntry.text}|${newEntry.amount}|${newEntry.hash}|${newEntry.previousHash}")
                    snackbarMessage = "Log berhasil ditambahkan"
                }
            } else {
                snackbarMessage = "Harap isi semua bidang"
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Tambahkan Log")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { showSearchPopup = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Cari Log")
        }

        if (showSearchPopup) {
            AlertDialog(
                onDismissRequest = { showSearchPopup = false },
                title = { Text("Pencarian Log") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Masukkan kata kunci") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn(modifier = Modifier.fillMaxHeight(0.5f)) {
                            items(filteredLogs, key = { it.hash }) { entry ->
                                LogItem(entry)
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showSearchPopup = false }) {
                        Text("Tutup")
                    }
                }
            )
        }
    }
}

@Composable
fun LogItem(entry: LogEntry) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = "🕒 ${entry.timestamp}", style = MaterialTheme.typography.bodySmall)
            Text(text = "📝 ${entry.text}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "💰 Nominal: Rp ${entry.amount}", style = MaterialTheme.typography.bodyMedium)
            Text(text = "🔗 Hash: ${entry.hash.take(15)}...", style = MaterialTheme.typography.bodySmall)
            Text(text = "🔗 Prev Hash: ${entry.previousHash.take(15)}...", style = MaterialTheme.typography.bodySmall)
        }
    }
}

fun sha256(input: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogScreen() {
    LogKriptografiTheme {
        LogScreen()
    }
}
