package com.smartlease.app

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

private data class TabItem(val label: String, val icon: ImageVector)

private val tabs = listOf(
    TabItem("Home", Icons.Outlined.Home),
    TabItem("Properties", Icons.Outlined.Business),
    TabItem("Tenants", Icons.Outlined.People),
    TabItem("Payments", Icons.Outlined.AccountBalanceWallet),
    TabItem("Messages", Icons.Outlined.Chat)
)

@Composable
fun SmartLeaseApp(vm: AppViewModel = viewModel()) {
    val ctx = LocalContext.current
    LaunchedEffect(vm.message) {
        vm.message?.let { Toast.makeText(ctx, it, Toast.LENGTH_LONG).show(); vm.message = null }
    }
    if (vm.session == null) AuthScreen(vm) else MainShell(vm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(vm: AppViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var thread by rememberSaveable { mutableStateOf<String?>(null) }
    val inThread = tab == 4 && thread != null
    BackHandler(enabled = inThread) { thread = null }

    Scaffold(
        containerColor = Col.background,
        bottomBar = { if (!inThread) TabBar(tab) { tab = it; thread = null } }
    ) { pad ->
        if (inThread) {
            ThreadScreen(vm, thread!!, onBack = { thread = null }, modifier = Modifier.padding(pad))
        } else {
            PullToRefreshBox(isRefreshing = vm.loading, onRefresh = { vm.refresh() }, modifier = Modifier.padding(pad).fillMaxSize()) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
                    when (tab) {
                        0 -> DashboardScreen(vm, openThread = { tab = 4; thread = it }, goTab = { tab = it })
                        1 -> PropertiesScreen(vm)
                        2 -> TenantsScreen(vm, openThread = { tab = 4; thread = it })
                        3 -> PaymentsScreen(vm)
                        else -> InboxScreen(vm, openThread = { thread = it })
                    }
                }
            }
        }
    }
}

@Composable
private fun TabBar(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.background(Col.card)) {
        HorizontalDivider(color = Col.border)
        Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
            tabs.forEachIndexed { i, t ->
                val c = if (i == selected) Col.primary else Col.mutedFg
                Column(
                    Modifier.weight(1f).clickable { onSelect(i) }.padding(vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(t.icon, t.label, tint = c, modifier = Modifier.size(20.dp))
                    Text(t.label, color = c, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                }
            }
        }
    }
}
