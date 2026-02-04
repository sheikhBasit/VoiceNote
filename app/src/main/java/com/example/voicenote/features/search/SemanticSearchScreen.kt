package com.example.voicenote.features.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicenote.ui.theme.*

@Composable
fun SemanticSearchScreen(onBack: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    0.0f to InsightsPrimary.copy(alpha = 0.15f),
                    1.0f to Color.Transparent,
                    center = androidx.compose.ui.geometry.Offset(0f, 0f),
                    radius = 1000f
                )
            )
            .background(InsightsBackgroundDark)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            SearchTopBar(onBack = onBack)
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 100.dp)
            ) {
                item { SearchInputSection() }
                item { FilterChipsSection() }
                item { HighlyRelevantSection() }
                item { ConceptualSimilaritySection() }
                item { ResultCards() }
            }
        }
    }
}

@Composable
private fun SearchTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(InsightsBackgroundDark.copy(alpha = 0.8f))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(InsightsGlassWhite)
                .border(1.dp, InsightsGlassBorder, CircleShape)
                .clickable { onBack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = null, tint = Color.White)
        }
        Text("Semantic Search", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        
        var isAiActive by remember { mutableStateOf(false) }
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (isAiActive) InsightsPrimary.copy(alpha = 0.2f) else InsightsGlassWhite)
                .border(1.dp, if (isAiActive) InsightsPrimary else InsightsGlassBorder, CircleShape)
                .clickable { isAiActive = !isAiActive },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AutoAwesome, 
                contentDescription = "AI Search", 
                tint = if (isAiActive) InsightsPrimary else Color.White 
            )
        }
    }
}

@Composable
private fun SearchInputSection() {
    Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 24.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(InsightsGlassWhite)
                .border(1.dp, InsightsGlassBorder, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFFa19db9))
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Project strategy refinement",
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.Mic, contentDescription = null, tint = Color(0xFFa19db9))
            }
        }
    }
}

@Composable
private fun FilterChipsSection() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(bottom = 16.dp)
    ) {
        item { FilterChip(label = "Relevance", isSelected = true) }
        item { FilterChip(label = "Date", isSelected = false) }
        item { FilterChip(label = "Sentiment", isSelected = false) }
        item { FilterChip(label = "Meetings", isSelected = false) }
    }
}

@Composable
private fun FilterChip(label: String, isSelected: Boolean) {
    Box(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) InsightsPrimary.copy(alpha = 0.2f) else InsightsGlassWhite)
            .border(1.dp, if (isSelected) InsightsPrimary.copy(alpha = 0.4f) else InsightsGlassBorder, CircleShape)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun HighlyRelevantSection() {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Highly Relevant", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                "TOP MATCHES", 
                color = InsightsPrimary, 
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                modifier = Modifier.background(InsightsPrimary.copy(alpha = 0.1f), CircleShape).padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(InsightsGlassWhite)
                .border(1.dp, InsightsGlassBorder, RoundedCornerShape(24.dp))
        ) {
            Column {
                Box(modifier = Modifier.fillMaxWidth().aspectRatio(21f/9f)) {
                    AsyncImage(
                        model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCxZx9js3AIl7zMRfhG7om5Bftrioi34V1eTdooOTvo4ACshINURBds7-MSCQsPFj5Ektk5q34bFS3kARvAk9Eurila1oC5Ny7K8HFP6U3TTBT9VlEwxjp13IVdSap31pgzhtBwExmF2gzqbPDOquPp95jpaXlAKYncbXJjW0NYwqZ3E-He8jibKi4FW15twfnWh1cYweUFoMlzbegOmKfg82R0q0cS08sF_rh5K9d0lNL8HAagLp6y9AuAlj_4VAO8y98uveImJyOK",
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, InsightsBackgroundDark.copy(alpha=0.8f)))))
                    Box(modifier = Modifier.padding(12.dp).background(InsightsPrimary.copy(alpha=0.8f), CircleShape).padding(horizontal = 12.dp, vertical = 4.dp)) {
                       Text("98% Match", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold) 
                    }
                }
                
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         Text("MEETING • POSITIVE", color = InsightsPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                         Icon(Icons.Default.MoreHoriz, contentDescription = null, tint = Color(0xFFa19db9))
                    }
                    
                    Text(
                        text = "Q4 Project Strategy Refinement", 
                        color = Color.White, 
                        fontSize = 20.sp, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    
                    Text(
                        text = "Discussed the shift towards conceptual retrieval and user-centric design with the core engineering team.",
                        color = Color(0xFFa19db9),
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(32.dp).background(InsightsPrimary.copy(alpha=0.2f), CircleShape), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Schedule, contentDescription=null, tint=InsightsPrimary, modifier=Modifier.size(16.dp))
                            }
                            Text("2 days ago", color = Color(0xFFa19db9), fontSize = 14.sp)
                        }
                        
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = InsightsPrimary),
                            contentPadding = PaddingValues(horizontal = 24.dp)
                        ) {
                            Text("View Analysis")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConceptualSimilaritySection() {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Conceptual Similarity", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Text("Related to \"strategy\" and \"user design\"", color = Color(0xFFa19db9), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun ResultCards() {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Result 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(InsightsGlassWhite)
                .border(1.dp, InsightsGlassBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("VOICE NOTE • NEUTRAL", color = Color.White.copy(alpha=0.4f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(
                    text = "Personal thoughts on UX scaling", 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "\"...the long-term strategy must prioritize the user's mental model over technical constraints...\"",
                    color = Color(0xFFa19db9),
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 24.sp
                )
                
                HorizontalDivider(color = Color.White.copy(alpha=0.05f), modifier = Modifier.padding(vertical = 16.dp))
                
                Row(
                   modifier = Modifier.fillMaxWidth(),
                   horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(modifier = Modifier.size(24.dp).background(Color.Gray, CircleShape).border(2.dp, InsightsBackgroundDark, CircleShape))
                        Box(modifier = Modifier.size(24.dp).background(InsightsPrimary, CircleShape).border(2.dp, InsightsBackgroundDark, CircleShape), contentAlignment = Alignment.Center) {
                            Text("AI", fontSize = 8.sp, color=Color.White)
                        }
                    }
                    Text("85% match • Oct 24", color = Color(0xFFa19db9), fontSize = 12.sp)
                }
            }
        }
        
        // Result 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(InsightsGlassWhite)
                .border(1.dp, InsightsGlassBorder, RoundedCornerShape(24.dp))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("TASK • FOLLOW-UP", color = Color(0xFFe2b02a), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
                Text(
                    text = "Refine design documentation", 
                    color = Color.White, 
                    fontSize = 18.sp, 
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Text(
                    text = "Generated from the Strategy Session: Need to update the Figma components to reflect new search architecture.",
                    color = Color(0xFFa19db9),
                    fontSize = 16.sp,
                    lineHeight = 24.sp
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top=16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.background(Color.White.copy(alpha=0.05f), CircleShape).padding(horizontal=12.dp, vertical=4.dp)) {
                        Text("From Strategy Meeting", color=Color.White.copy(alpha=0.6f), fontSize=12.sp)
                    }
                    Box(
                        modifier = Modifier.size(40.dp).border(1.dp, Color.White.copy(alpha=0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ArrowForward, contentDescription=null, tint=Color(0xFFa19db9))
                    }
                }
            }
        }
    }
}
