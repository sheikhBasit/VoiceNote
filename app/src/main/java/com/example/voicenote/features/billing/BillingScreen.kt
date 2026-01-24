package com.example.voicenote.features.billing

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Wallet
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    onBack: () -> Unit,
    viewModel: BillingViewModel = hiltViewModel()
) {
    val walletState by viewModel.walletState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()
    val checkoutUrl by viewModel.checkoutUrl.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(checkoutUrl) {
        checkoutUrl?.let { url ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            viewModel.clearCheckoutUrl()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI Credits & Wallet", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Black // Assuming themed background
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Balance Card
            walletState?.getOrNull()?.let { wallet ->
                GlassCard(intensity = 1.2f) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Balance", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                "${wallet.balance} Credits",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF)
                            )
                        }
                        Icon(Icons.Default.Wallet, null, modifier = Modifier.size(48.dp), tint = Color.White.copy(alpha = 0.2f))
                    }
                }
            } ?: Box(modifier = Modifier.height(100.dp).fillMaxWidth()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }

            // Top-up Plans
            Text("Refill Plans", style = MaterialTheme.typography.titleMedium, color = Color.White)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CreditPlanCard("500", "$5.00", Modifier.weight(1f)) { viewModel.initCheckout(500) }
                CreditPlanCard("2000", "$15.00", Modifier.weight(1f)) { viewModel.initCheckout(2000) } // Bonus math
            }

            // Transaction History
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.History, null, tint = Color.White.copy(alpha = 0.6f))
                Spacer(Modifier.width(8.dp))
                Text("Ledger History", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }

            walletState?.getOrNull()?.let { wallet ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(wallet.recentTransactions) { tx ->
                        TransactionItem(tx)
                    }
                }
            }
        }
    }
}

@Composable
fun CreditPlanCard(credits: String, price: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    ElevatedButton(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.elevatedButtonColors(
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = Color.White
        )
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(credits, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Credits", style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Surface(
                color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(price, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelMedium, color = Color(0xFF00E5FF))
            }
        }
    }
}

@Composable
fun TransactionItem(tx: com.example.voicenote.data.remote.TransactionDTO) {
    val df = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
    GlassCard(intensity = 0.5f) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(tx.description, style = MaterialTheme.typography.bodyMedium, color = Color.White)
                Text(df.format(Date(tx.timestamp)), style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
            }
            Text(
                text = (if (tx.amount > 0) "+" else "") + tx.amount,
                color = if (tx.amount > 0) Color.Green else Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
