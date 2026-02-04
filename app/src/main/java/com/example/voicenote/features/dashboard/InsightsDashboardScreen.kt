package com.example.voicenote.features.dashboard

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.voicenote.ui.theme.*

@Composable
fun InsightsDashboardScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(InsightsBackgroundDark)
    ) {
        // Background Decor
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = InsightsPrimary.copy(alpha = 0.15f),
                radius = 300.dp.toPx(),
                center = Offset(x = 0f, y = 0f), // Top Left
            )
            drawCircle(
                color = InsightsAccentViolet.copy(alpha = 0.15f),
                radius = 300.dp.toPx(),
                center = Offset(x = size.width, y = size.height * 0.8f), // Bottom Right
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp), // Space for fab/bottom nav
            contentPadding = PaddingValues(16.dp)
        ) {
            item { TopAppBarSection() }
            item { HeadlineSection() }
            item { ChartsSection() }
            item { HeatmapSection() }
            item { FocusProjectsSection() }
        }

        FloatingBottomNavigation(modifier = Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun TopAppBarSection() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             AsyncImage(
                model = "https://lh3.googleusercontent.com/aida-public/AB6AXuCaHRGThUoEsxFl4HbbCXq4NwzUly6aPAbZnuOJVc-DGb0Z1NF6wbiX_5-vfh13JgjgUNU_9QIIFwzqg0ujwCS9FTg7YWui-bQKWKwAQmXY8TkM5_zU4SXC_TYEuMvFvR2nR88atf_gAGlcpcIJ7pVnXuOr_mLm0SBCPfE6jjxbtjU-B4PgWO8anF1zAQNPUVlxfHYMgcVf6IsX186WoPMDYq6Y_GFFaXxkTbwROaeXi2_tM1uN-pqDDuybm6g0sMhdYrjrhWkYv24E",
                contentDescription = "Profile",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(2.dp, InsightsPrimary, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "INSIGHTS",
                    color = Gray400,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "Oct 12, 2023",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlassIconButton(icon = Icons.Default.Notifications)
            GlassIconButton(icon = Icons.Default.Search)
        }
    }
}

@Composable
fun GlassIconButton(icon: ImageVector) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(InsightsGlassWhite, CircleShape)
            .border(1.dp, InsightsGlassBorder, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun HeadlineSection() {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = "Your Productivity",
            color = Color.White,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 36.sp
        )
        Text(
            text = "AI-powered analysis from last 7 days",
            color = Gray400,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ChartsSection() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Meetings Chart Card
        GlassCard {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = "MEETINGS ANALYZED",
                            color = Gray400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "42",
                            color = Color.White,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF22c55e).copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+12%",
                            color = Color(0xFF4ade80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Simulated Chart
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .padding(top = 16.dp)
                ) {
                   Canvas(modifier = Modifier.fillMaxSize()) {
                       val path = Path().apply {
                           moveTo(0f, size.height * 0.7f)
                           cubicTo(
                               size.width * 0.2f, size.height * 0.8f,
                               size.width * 0.4f, size.height * 0.2f,
                               size.width * 0.6f, size.height * 0.4f
                           )
                           cubicTo(
                               size.width * 0.8f, size.height * 0.6f,
                               size.width * 0.9f, size.height * 0.1f,
                               size.width, size.height * 0.3f
                           )
                       }
                       
                       drawPath(
                           path = path,
                           color = InsightsPrimary,
                           style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                       )
                       
                       // Fill gradient below path (Simplified for this example)
                       val fillPath = Path().apply {
                           addPath(path)
                           lineTo(size.width, size.height)
                           lineTo(0f, size.height)
                           close()
                       }
                       drawPath(
                           path = fillPath,
                           brush = Brush.verticalGradient(
                               colors = listOf(InsightsPrimary.copy(alpha = 0.3f), Color.Transparent),
                               startY = 0f,
                               endY = size.height
                           )
                       )
                   }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val days = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
                    days.forEach { day ->
                        Text(text = day, color = Gray500, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Productivity Insight Card
        GlassCard(color = InsightsPrimary.copy(alpha = 0.05f), borderColor = InsightsPrimary.copy(alpha = 0.2f)) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(InsightsPrimary.copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null, tint = InsightsPrimary)
                }
                Column {
                    Text(text = "Peak Performance", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = "You're most productive on Tuesdays between 10 AM - 12 PM.", color = Gray400, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HeatmapSection() {
    Column(modifier = Modifier.padding(vertical = 24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Meeting Heatmap", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(text = "DETAILS", color = InsightsPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        
        GlassCard {
            Column(modifier = Modifier.padding(16.dp)) {
                // Simplified Heatmap Grid using Row/Column for structure
                 val days = listOf("M", "T", "W", "T", "F", "S", "S")
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                     days.forEach { 
                         Text(
                             text = it, 
                             color = Gray500, 
                             fontSize = 9.sp, 
                             fontWeight = FontWeight.Bold,
                             modifier = Modifier.width(32.dp),
                             textAlign = androidx.compose.ui.text.style.TextAlign.Center
                         ) 
                     }
                 }
                 
                 Spacer(modifier = Modifier.height(8.dp))
                 
                 // 4 rows of 7 items
                 for(i in 0..3) {
                     Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                         for(j in 0..6) {
                             Box(
                                 modifier = Modifier
                                    .size(32.dp)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        when((i*7 + j) % 5) {
                                            0 -> InsightsPrimary
                                            1 -> InsightsPrimary.copy(alpha = 0.6f)
                                            2 -> InsightsPrimary.copy(alpha = 0.4f)
                                            3 -> InsightsPrimary.copy(alpha = 0.2f)
                                            else -> InsightsGlassWhite
                                        }
                                    )
                             )
                         }
                     }
                 }
                 
                 Spacer(modifier = Modifier.height(16.dp))
                 HorizontalDivider(color = InsightsGlassBorder, thickness = 1.dp)
                 Spacer(modifier = Modifier.height(12.dp))
                 
                 Row(
                     modifier = Modifier.fillMaxWidth(),
                     horizontalArrangement = Arrangement.SpaceBetween,
                     verticalAlignment = Alignment.CenterVertically
                 ) {
                     Text(
                        text = "Total Hours: 18h 45m",
                        color = Gray400,
                        fontSize = 11.sp
                     ) // Note: Simplified text styling for simplicity
                     
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "LOW", color = Gray500, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                            Box(modifier = Modifier.size(8.dp).background(InsightsPrimary.copy(alpha = 0.2f), CircleShape))
                            Box(modifier = Modifier.size(8.dp).background(InsightsPrimary.copy(alpha = 0.5f), CircleShape))
                            Box(modifier = Modifier.size(8.dp).background(InsightsPrimary, CircleShape))
                        }
                        Text(text = "HIGH", color = Gray500, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                 }
            }
        }
    }
}

@Composable
fun FocusProjectsSection() {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(text = "Focus Projects", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Project 1
            GlassCard(
                modifier = Modifier.weight(1f).aspectRatio(0.8f),
                backgroundImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuCZK5WBGIk5OIcy7adw_h88onE8tEW0x--6By-z62i1vcnHLxPGUqd225djZ-RyT1Jbi75nHcGiwhXB9JyI3A7fIq2QOMk2Gurfbjhd-jrfL0Axxl8sUt0CmltpskuDYlOyX3A9liAGlQp00aCtJVsGospCDsifbUNrj8BovVly8_BLn-TCEIoMcDTnAT45zLWtbPfeD8GK9eT69xoW8vdAK3vrlhPA4kC_BtyJnzvl4FlmOlh5THErR4P9beSmwKi_FkbTeH04hahg"
            ) {
               Column(
                   modifier = Modifier
                       .fillMaxSize()
                       .padding(16.dp)
                       .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))),
                   verticalArrangement = Arrangement.Bottom
               ) {
                   Text(text = "Weekly Velocity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                   Text(text = "82% COMPLETE", color = InsightsPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
               }
            }
            
            // Project 2
            GlassCard(
                modifier = Modifier.weight(1f).aspectRatio(0.8f),
                backgroundImageUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuDXBO9IEpOg5hPTjWs8rY4yla2BH0k49D6uFuT3YfzTaWKQ60n3X6CofdXSnUxOI_o8MZfgxTvbKChKhdmZ0ozz69-gdumoLdsjSzR0bBUjgm73p683a8SS__USlwrB7-qk6RHYcbODD0MS0R3Vw7LSAJs6eFdIyLSirtUFlI0ItcUnxCO1k3Thd74Np66JfbDOSNcBHOINSkU91CZB8cjVvRHUpQfHbb_sW9j_xMonKbTz0ziF54Aweq3abw6k4zWmrda1yT3SZvEg"
            ) {
                 Column(
                   modifier = Modifier
                       .fillMaxSize()
                       .padding(16.dp)
                       .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))),
                   verticalArrangement = Arrangement.Bottom
               ) {
                   Text(text = "Sentiment Trends", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, lineHeight = 18.sp)
                   Text(text = "MOSTLY POSITIVE", color = InsightsAccentViolet, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
               }
            }
        }
    }
}

@Composable
fun FloatingBottomNavigation(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(bottom = 24.dp)
            .width(300.dp) // Approximate width
            .height(64.dp)
            .background(InsightsGlassWhite, CircleShape)
            .border(1.dp, InsightsGlassBorder, CircleShape)
            .clip(CircleShape)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Default.Home, contentDescription = null, tint = InsightsPrimary, modifier = Modifier.size(28.dp))
            Icon(imageVector = Icons.Default.Mic, contentDescription = null, tint = Gray500, modifier = Modifier.size(28.dp))
            
            // FAB
            Box(
                modifier = Modifier
                    .offset(y = (-20).dp) // Pop out effect
                    .size(56.dp)
                    .background(InsightsPrimary, CircleShape)
                    .border(4.dp, InsightsBackgroundDark, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
            }
            
            Icon(imageVector = Icons.Default.Analytics, contentDescription = null, tint = Gray500, modifier = Modifier.size(28.dp))
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Gray500, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    color: Color = InsightsGlassWhite.copy(alpha = 0.03f),
    borderColor: Color = InsightsGlassBorder,
    backgroundImageUrl: String? = null,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(color)
            .border(1.dp, borderColor, RoundedCornerShape(24.dp))
    ) {
        if (backgroundImageUrl != null) {
            AsyncImage(
                model = backgroundImageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        content()
    }
}
