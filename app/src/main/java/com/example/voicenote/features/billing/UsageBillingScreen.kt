package com.example.voicenote.features.billing

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.voicenote.ui.components.GlassCard
import com.example.voicenote.data.remote.WalletDTO
import com.example.voicenote.ui.theme.*

@Composable
fun UsageBillingScreen(
    viewModel: BillingViewModel = hiltViewModel(),
    onBack: () -> Unit = {}
) {
    val walletState by viewModel.walletState.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val wallet = walletState?.getOrNull()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                       colors = listOf(InsightsPrimary.copy(alpha = 0.15f), Color.Transparent),
                       center = androidx.compose.ui.geometry.Offset(0f, 0f),
                       radius = 800f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                       colors = listOf(InsightsPrimary.copy(alpha = 0.1f), Color.Transparent),
                       center = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                       radius = 800f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item { UsageTopBar(onBack) }
            item { CurrentPlanCard(wallet) }
            item { UsageStatisticsSection(wallet) }
            item { UpgradeHeroCard() }
            item { InvoicesSection(wallet) }
            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
private fun UsageTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(InsightsGlassWhite).border(1.dp, InsightsGlassBorder, CircleShape).clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
        }
        Text("Usage & Billing", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(InsightsGlassWhite).border(1.dp, InsightsGlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
private fun CurrentPlanCard(wallet: WalletDTO?) {
    GlassCard(color = InsightsGlassWhite.copy(alpha = 0.03f), borderColor = InsightsGlassBorder) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text("CURRENT PLAN", color = InsightsPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text(if (wallet?.stripeSubscriptionId != null) "Pro Plan" else "Free Plan", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, InsightsPrimary.copy(alpha=0.3f), RoundedCornerShape(100))
                        .background(InsightsPrimary.copy(alpha=0.2f), RoundedCornerShape(100))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text("ACTIVE", color = InsightsPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            
            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                 Box(modifier = Modifier.size(32.dp).background(Color(0xFF27272a), CircleShape).border(2.dp, InsightsBackgroundDark, CircleShape), contentAlignment = Alignment.Center) {
                     Icon(Icons.Default.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                 }
                 Text(if (wallet?.stripeSubscriptionId != null) "Renewing soon" else "No active subscription", color = Color(0xFFa1a1aa), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
            
            Button(
                onClick = {},
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.05f)),
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
            ) {
                Text("Manage Subscription", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun UsageStatisticsSection(wallet: WalletDTO?) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text("Usage Statistics", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("Updated just now", color = Color(0xFF71717a), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
        
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            UsageMeter(
                icon = Icons.Default.Mic,
                iconColor = InsightsPrimary,
                label = "Credits Balance",
                value = (wallet?.balance ?: 0).toString(),
                limit = "Unlimited",
                percentage = 1.0f,
                footerText = "Available for transcription",
                actionText = "Top up"
            )
            
            UsageMeter(
                icon = Icons.Default.Psychology,
                iconColor = Color(0xFFc084fc),
                label = "AI Analysis Calls",
                value = "Active",
                limit = "Premium",
                percentage = 1.0f,
                footerText = "Full access enabled",
                actionText = null
            )
        }
    }
}

@Composable
private fun UsageMeter(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    limit: String,
    percentage: Float,
    footerText: String,
    actionText: String?
) {
    GlassCard(color = InsightsGlassWhite.copy(alpha = 0.03f), borderColor = InsightsGlassBorder) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                    Text(label, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Row {
                    Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(" / $limit", color = Color(0xFF71717a), fontSize = 14.sp)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(if(actionText != null) InsightsPrimary else Color.White)
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                 Text(footerText, color = Color(0xFFa1a1aa), fontSize = 12.sp)
                 if(actionText != null) {
                      Text(actionText, color = InsightsPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { })
                 }
            }
        }
    }
}

@Composable
private fun UpgradeHeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(colors = listOf(InsightsPrimary, Color(0xFF312e81))))
    ) {
         Icon(
             imageVector = Icons.Default.Groups,
             contentDescription = null,
             tint = Color.White.copy(alpha=0.2f),
             modifier = Modifier.align(Alignment.TopEnd).size(120.dp).offset(x = 24.dp, y= (-16).dp)
         )
         
         Column(modifier = Modifier.padding(24.dp)) {
             Text("Go Team Plan", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
             Text(
                 "Unlock shared workspaces, unlimited AI analysis, and central billing for your whole team.",
                 color = Color(0xFFe0e7ff),
                 fontSize = 14.sp,
                 fontWeight = FontWeight.Medium,
                 modifier = Modifier.padding(top = 12.dp, bottom = 12.dp).fillMaxWidth(0.7f)
             )
             Button(
                 onClick = {},
                 colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = InsightsPrimary),
                 shape = CircleShape
             ) {
                 Text("Upgrade Now", fontWeight = FontWeight.Bold)
             }
         }
    }
}

@Composable
private fun InvoicesSection(wallet: WalletDTO?) {
    Column {
         Text("Recent Transactions", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp, vertical = 12.dp))
         Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
             if (wallet?.recentTransactions.isNullOrEmpty()) {
                 Text("No recent transactions found.", color = Gray500, modifier = Modifier.padding(16.dp))
             } else {
                 wallet?.recentTransactions?.forEach { tx ->
                     InvoiceRow(tx.timestamp.toString(), "$${tx.amount / 100.0}", tx.description)
                 }
             }
         }
    }
}

@Composable
private fun InvoiceRow(date: String, amount: String, description: String) {
    GlassCard(color = InsightsGlassWhite.copy(alpha = 0.03f), borderColor = InsightsGlassBorder) {
         Row(
             modifier = Modifier.padding(16.dp).fillMaxWidth(),
             horizontalArrangement = Arrangement.SpaceBetween,
             verticalAlignment = Alignment.CenterVertically
         ) {
             Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                  Box(
                      modifier = Modifier.size(40.dp).background(Color.White.copy(alpha=0.05f), CircleShape),
                      contentAlignment = Alignment.Center
                  ) {
                      Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFa1a1aa))
                  }
                  Column {
                      Text(description, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                      Text(date, color = Color(0xFF71717a), fontSize = 12.sp)
                  }
             }
             
             Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                 Text(amount, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                 Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFFa1a1aa))
             }
         }
    }
}
