package com.fittakip.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fittakip.app.data.OFF_DAY
import com.fittakip.app.data.WEEKDAY_NAMES
import com.fittakip.app.data.WEEK_PRESETS
import java.time.LocalDate

/** Haftalık program: her güne bir antrenman ya da off day ata. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramScreen(vm: AppViewModel, onBack: () -> Unit) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val sets by vm.sets.collectAsStateWithLifecycle()
    var editingDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var newTemplate by rememberSaveable { mutableStateOf(false) }
    val todayIdx = LocalDate.now().dayOfWeek.value - 1
    val templates = settings.allTemplates

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { BackHeader("Haftalık program", onBack) }

        item {
            SectionCard("Program nasıl ilerlesin?") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SelectCard(!settings.rotation, { vm.setPlanMode(false) }, "📅 Günlere bağlı", Modifier.weight(1f), "Pazartesi hep Upper A")
                    SelectCard(settings.rotation, { vm.setPlanMode(true) }, "🔁 Sırayla", Modifier.weight(1f), "Kaçırınca sıra kaymaz")
                }
                if (settings.rotation) {
                    val order = com.fittakip.app.domain.PlanResolver.order(settings)
                    val next = com.fittakip.app.domain.PlanResolver.templateFor(settings, sets, vm.today)
                    if (order.isEmpty()) HintText("Aşağıda günlere antrenman ata; sıra o atamaların sırasına göre oluşur.")
                    else {
                        Text("Sıra: " + order.joinToString(" → ") { it.name }, style = MaterialTheme.typography.titleSmall)
                        next?.let { Pill("Sıradaki: ${it.name}", FitColors.Lime) }
                        HintText("İstediğin gün spora git; uygulama her seferinde sıradaki antrenmanı verir. Hangi gün gittiğin önemli değil, sıra korunur. Arka arkaya iki gün antrenman yaparsan dinlenmeyi hatırlatırım.")
                        TextButton(onClick = { vm.resetRotation() }) { Text("Sırayı baştan başlat (${order.first().name})") }
                    }
                } else {
                    HintText("Her gün o güne atadığın antrenman gelir. Bir gün kaçırırsan o antrenman o hafta atlanır. Haftada belirli günler gidiyorsan bu mod uygundur.")
                }
            }
        }

        item {
            SectionCard("Hazır programla başla", icon = Icons.Rounded.AutoAwesome) {
                WEEK_PRESETS.forEach { p ->
                    Surface(
                        onClick = { vm.applyPreset(p.days) }, shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.05f), modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(p.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                                if (settings.plan == p.days) Pill("Aktif", FitColors.Lime)
                            }
                            HintText(p.desc)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                p.days.forEachIndexed { i, d ->
                                    Box(
                                        Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (d == OFF_DAY) Color.White.copy(alpha = 0.05f) else FitColors.Lime.copy(alpha = 0.22f))
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(WEEKDAY_NAMES[i].take(2), style = MaterialTheme.typography.labelSmall, color = if (d == OFF_DAY) MaterialTheme.colorScheme.onSurfaceVariant else FitColors.Lime)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionCard("Günler", icon = Icons.Rounded.CalendarMonth) {
                HintText("Bir güne dokun ve antrenman ya da off day seç.")
                WEEKDAY_NAMES.forEachIndexed { i, name ->
                    val v = settings.plan[i]
                    val tpl = templates.firstOrNull { it.name == v }
                    Surface(
                        onClick = { editingDay = if (editingDay == i) null else i },
                        shape = RoundedCornerShape(18.dp),
                        color = if (i == todayIdx) FitColors.Lime.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.04f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(name + if (i == todayIdx) " · bugün" else "", style = MaterialTheme.typography.titleSmall)
                                    HintText(
                                        when {
                                            v == OFF_DAY -> "Dinlenme günü"
                                            tpl != null -> tpl.items.joinToString(" · ") { it.exercise }
                                            else -> "Atanmadı"
                                        }
                                    )
                                }
                                Pill(
                                    when { v == OFF_DAY -> "Off 😌"; v.isEmpty() -> "Seç"; else -> v },
                                    when { v == OFF_DAY -> Color(0xFF8E9BFF); v.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant; else -> FitColors.Lime }
                                )
                            }
                            if (editingDay == i) {
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Pill("😌 Off day", Color(0xFF8E9BFF), onClick = { vm.setPlanDay(i, OFF_DAY); editingDay = null })
                                    templates.forEach { t ->
                                        Pill(t.name, if (t.name == v) FitColors.Lime else MaterialTheme.colorScheme.onSurface, onClick = { vm.setPlanDay(i, t.name); editingDay = null })
                                    }
                                    Pill("Boş bırak", MaterialTheme.colorScheme.onSurfaceVariant, onClick = { vm.setPlanDay(i, ""); editingDay = null })
                                }
                            }
                        }
                    }
                }
                val trainDays = settings.plan.count { it.isNotEmpty() && it != OFF_DAY }
                HintText("Haftada $trainDays antrenman, ${settings.plan.count { it == OFF_DAY }} off day.")
            }
        }

        item {
            SectionCard("Antrenmanlar") {
                templates.forEach { t ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(t.name, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            if (settings.templates.any { it.name == t.name }) {
                                TextButton(onClick = { vm.deleteTemplate(t.name) }) { Text("Sil", color = MaterialTheme.colorScheme.error) }
                            }
                        }
                        HintText(t.items.joinToString(" · ") { "${it.exercise} ${it.label}" })
                    }
                }
                PrimaryButton("Kendi antrenmanını oluştur", onClick = { newTemplate = true }, icon = Icons.Rounded.Add)
            }
        }

        if (settings.hasPlan) {
            item { TextButton(onClick = { vm.clearPlan() }, modifier = Modifier.fillMaxWidth()) { Text("Programı temizle", color = MaterialTheme.colorScheme.error) } }
        }
        item { HintText("Sabah bildirimini açarsan her sabah günün programını sana söylerim (Ayarlar > Hatırlatma).") }
    }

    if (newTemplate) NewTemplateDialog(onDismiss = { newTemplate = false }) { n, items ->
        vm.saveTemplate(n, items)
        newTemplate = false
    }
}
