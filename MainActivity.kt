package dk.pengeoverblik.app

import android.os.Bundle
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private val Green = Color(0xFF286B5B)
private val Ink = Color(0xFF202B28)
private val Canvas = Color(0xFFF7F7F2)
private val Muted = Color(0xFF7A8580)
private val Mint = Color(0xFFE5F0E9)
private val Peach = Color(0xFFF8EAE2)
private val Line = Color(0xFFE7EAE4)
private val Danish = Locale("da", "DK")

private data class Entry(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val isIncome: Boolean,
    val category: String,
    val recurring: Boolean = false
)

private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Danish).format(value)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { PengeOverblikApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PengeOverblikApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("pengeoverblik", Context.MODE_PRIVATE) }
    var entries by remember { mutableStateOf(loadEntries(prefs)) }
    var tab by remember { mutableIntStateOf(0) }
    var showAdd by remember { mutableStateOf(false) }
    var incomeMode by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    fun persist(next: List<Entry>) {
        entries = next
        prefs.edit().putString("entries", JSONArray().apply {
            next.forEach { e -> put(JSONObject().apply {
                put("id", e.id); put("title", e.title); put("amount", e.amount)
                put("income", e.isIncome); put("category", e.category); put("recurring", e.recurring)
            }) }
        }.toString()).apply()
    }

    val totalIncome = entries.filter { it.isIncome }.sumOf { it.amount }
    val totalExpenses = entries.filterNot { it.isIncome }.sumOf { it.amount }
    val balance = totalIncome - totalExpenses

    MaterialTheme(
        colorScheme = lightColorScheme(primary = Green, onPrimary = Color.White, background = Canvas, surface = Color.White, onSurface = Ink, secondary = Color(0xFFB5E5C8))
    ) {
        Scaffold(
            containerColor = Canvas,
            topBar = {
                TopAppBar(
                    title = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("PengeOverblik", fontWeight = FontWeight.Bold, color = Ink, style = MaterialTheme.typography.titleLarge)
                            Text("DIN ØKONOMI · ENKELT", style = MaterialTheme.typography.labelSmall, color = Muted, letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified)
                        }
                    },
                    actions = { IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Indstillinger", tint = Ink) } },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Canvas)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    NavigationBarItem(selected = tab == 0, onClick = { tab = 0 }, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Overblik") })
                    NavigationBarItem(selected = tab == 1, onClick = { tab = 1 }, icon = { Icon(Icons.Default.ListAlt, null) }, label = { Text("Poster") })
                    NavigationBarItem(selected = tab == 2, onClick = { tab = 2 }, icon = { Icon(Icons.Default.Savings, null) }, label = { Text("Mål") })
                }
            },
            floatingActionButton = {
                if (tab != 2) FloatingActionButton(onClick = { incomeMode = false; showAdd = true }, containerColor = Green, contentColor = Color.White) {
                    Icon(Icons.Default.Add, contentDescription = "Tilføj post")
                }
            }
        ) { padding ->
            when (tab) {
                0 -> Dashboard(Modifier.padding(padding), totalIncome, totalExpenses, balance, entries,
                    onAddIncome = { incomeMode = true; showAdd = true }, onDelete = { id -> persist(entries.filterNot { it.id == id }) })
                1 -> EntriesScreen(Modifier.padding(padding), entries,
                    onDelete = { id -> persist(entries.filterNot { it.id == id }) },
                    onAdd = { isIncome -> incomeMode = isIncome; showAdd = true })
                else -> GoalsScreen(Modifier.padding(padding), balance)
            }
        }

        if (showAdd) AddEntryDialog(
            initialIncome = incomeMode,
            onDismiss = { showAdd = false },
            onSave = { entry -> persist(entries + entry); showAdd = false }
        )
        if (showSettings) AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Om PengeOverblik") },
            text = { Text("Version 1.0.0\n\nBudgetposter gemmes lokalt på denne telefon. Appen forbinder ikke til din bank og sender ikke budgetdata til en server. Afinstallation kan slette dataene. Eksport og gendannelse tilføjes i en senere version.") },
            confirmButton = { TextButton(onClick = { showSettings = false }) { Text("Luk") } }
        )
    }
}

private fun loadEntries(prefs: android.content.SharedPreferences): List<Entry> {
    val raw = prefs.getString("entries", null) ?: return emptyList()
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            Entry(o.optString("id", UUID.randomUUID().toString()), o.optString("title"), o.optDouble("amount"), o.optBoolean("income"), o.optString("category"), o.optBoolean("recurring"))
        }
    } catch (_: Exception) { emptyList() }
}

@Composable
private fun Dashboard(modifier: Modifier, income: Double, expenses: Double, balance: Double, entries: List<Entry>, onAddIncome: () -> Unit, onDelete: (String) -> Unit) {
    val expenseShare = if (income > 0.0) (expenses / income).coerceIn(0.0, 1.0).toFloat() else 0f
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 12.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Dit månedsoverblik", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                Text("Her er status på din økonomi lige nu.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            }
        }
        item {
            Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Green), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).background(Color(0x33FFFFFF), CircleShape), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Savings, contentDescription = null, tint = Color.White, modifier = Modifier.size(19.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Text("TILBAGE EFTER REGISTREREDE POSTER", color = Color(0xFFD9E9E1), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(15.dp))
                    Text(money(balance), color = Color.White, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0x55FFFFFF))
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Indtægter", color = Color(0xFFD9E9E1), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(money(income), color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Udgifter", color = Color(0xFFD9E9E1), style = MaterialTheme.typography.labelMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(money(expenses), color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
        item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Forbrug af indtægter", color = Ink, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        Text("${(expenseShare * 100).toInt()}%", color = Green, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(progress = { expenseShare }, modifier = Modifier.fillMaxWidth().height(8.dp), color = Green, trackColor = Mint)
                    Text(if (income > 0) "Du har registreret udgifter svarende til ${(expenseShare * 100).toInt()}% af dine indtægter." else "Tilføj dine indtægter for at se, hvor stor en del af dem dine udgifter udgør.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        item { Text("Hurtigt overblik", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard("Indtægter", income, Mint, Modifier.weight(1f))
                MetricCard("Udgifter", expenses, Peach, Modifier.weight(1f))
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Seneste poster", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), color = Ink)
                Text("${entries.size} i alt", color = Muted, style = MaterialTheme.typography.labelMedium)
            }
        }
        if (entries.isEmpty()) item { EmptyState("Start med din første post", "Tilføj en indtægt eller udgift, så bygger dit overblik sig op her.") }
        items(entries.takeLast(4).reversed(), key = { it.id }) { entry -> EntryRow(entry, onDelete) }
        item {
            OutlinedButton(onClick = onAddIncome, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Line)) {
                Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Tilføj indtægt", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, amount: Double, tint: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = tint)) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Muted, style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(9.dp))
            Text(money(amount), color = Ink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EntriesScreen(modifier: Modifier, entries: List<Entry>, onDelete: (String) -> Unit, onAdd: (Boolean) -> Unit) {
    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Text("Dine budgetposter", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Text("Beløb vises som månedlige beløb. Du kan slette poster med papirkurven.", color = Muted, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { onAdd(true) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("+ Indtægt") }
            OutlinedButton(onClick = { onAdd(false) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) { Text("+ Udgift") }
        }
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) EmptyState("Ingen budgetposter", "Tilføj en indtægt eller udgift for at komme i gang.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 24.dp)) { items(entries, key = { it.id }) { EntryRow(it, onDelete) } }
    }
}

@Composable
private fun EntryRow(entry: Entry, onDelete: (String) -> Unit) {
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).background(if (entry.isIncome) Mint else Peach, CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (entry.isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, null, tint = if (entry.isIncome) Green else Color(0xFFB64C32))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(entry.title, fontWeight = FontWeight.SemiBold, color = Ink)
                Text(if (entry.recurring) "${entry.category} · Fast post" else entry.category, color = Muted, style = MaterialTheme.typography.labelMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text((if (entry.isIncome) "+ " else "− ") + money(entry.amount), fontWeight = FontWeight.Bold, color = if (entry.isIncome) Green else Ink)
                IconButton(onClick = { onDelete(entry.id) }, modifier = Modifier.size(30.dp)) { Icon(Icons.Default.Delete, "Slet ${entry.title}", tint = Muted, modifier = Modifier.size(18.dp)) }
            }
        }
    }
}

@Composable
private fun GoalsScreen(modifier: Modifier, balance: Double) {
    Column(modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Opsparing og mål", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp)) {
                Box(Modifier.size(48.dp).background(Color(0xFFE2F4E9), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Default.Savings, null, tint = Green) }
                Spacer(Modifier.height(14.dp))
                Text("Muligt månedligt råderum", color = Muted)
                Text(money(balance), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Ink)
                Spacer(Modifier.height(8.dp))
                Text("Dette er forskellen mellem dine registrerede indtægter og udgifter – ikke en garanti for, at beløbet kan spares op.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        EmptyState("Opsparingsmål kommer snart", "I en senere version kan du oprette mål, følge fremskridt og registrere gældsafbetaling.")
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.Bold, color = Ink)
            Spacer(Modifier.height(6.dp)); Text(message, color = Muted, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEntryDialog(initialIncome: Boolean, onDismiss: () -> Unit, onSave: (Entry) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(initialIncome) }
    var category by remember { mutableStateOf(if (initialIncome) "Indkomst" else "Andet") }
    var recurring by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val categories = if (isIncome) listOf("Indkomst", "Løn", "Børnepenge", "Andet") else listOf("Bolig", "Mad", "Transport", "Forsikring", "Regninger", "Gæld", "Opsparing", "Andet")
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tilføj budgetpost", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = isIncome, onClick = { isIncome = true; category = "Indkomst" }, label = { Text("Indtægt") })
                    FilterChip(selected = !isIncome, onClick = { isIncome = false; category = "Andet" }, label = { Text("Udgift") })
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Navn, fx husleje") }, singleLine = true)
                OutlinedTextField(value = amount, onValueChange = { amount = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' } }, label = { Text("Beløb i kr. pr. måned") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), singleLine = true)
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                    OutlinedTextField(value = category, onValueChange = {}, readOnly = true, label = { Text("Kategori") }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }, modifier = Modifier.menuAnchor().fillMaxWidth())
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        categories.forEach { item -> DropdownMenuItem(text = { Text(item) }, onClick = { category = item; expanded = false }) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(checked = recurring, onCheckedChange = { recurring = it }); Text("Fast månedlig post") }
            }
        },
        confirmButton = { Button(onClick = { val parsed = amount.toDoubleOrNull(); if (title.isNotBlank() && parsed != null && parsed > 0) onSave(Entry(title = title.trim(), amount = parsed, isIncome = isIncome, category = category, recurring = recurring)) }, enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0) { Text("Gem") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuller") } }
    )
}
