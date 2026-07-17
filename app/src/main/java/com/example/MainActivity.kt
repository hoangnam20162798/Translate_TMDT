package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppContainer()
            }
        }
    }
}

enum class ShoppingTab(
    val title: String,
    val icon: ImageVector,
    val tag: String
) {
    BROWSER("Trình duyệt", Icons.Default.Language, "tab_browser"),
    CALCULATOR("Tính giá", Icons.Default.Calculate, "tab_calculator"),
    SIZE("Chọn size", Icons.Default.Straighten, "tab_size"),
    SIMULATOR("Dịch nổi", Icons.AutoMirrored.Filled.ScreenShare, "tab_simulator"),
    GUIDE("Cẩm nang", Icons.AutoMirrored.Filled.MenuBook, "tab_guide")
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainAppContainer() {
    var currentTab by remember { mutableStateOf(ShoppingTab.BROWSER) }
    
    // Shared parameters for screen navigation
    var calcPriceParam by remember { mutableStateOf(0.0) }
    var calcCurrencyParam by remember { mutableStateOf("USD") }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("app_navigation_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                ShoppingTab.values().forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        modifier = Modifier.testTag(tab.tag),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentTab,
            transitionSpec = {
                fadeIn(animationSpec = tween(150)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "tab_fade",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { tab ->
            when (tab) {
                ShoppingTab.BROWSER -> {
                    BrowserScreen(
                        onNavigateToCalculator = { price, currency ->
                            calcPriceParam = price
                            calcCurrencyParam = currency
                            currentTab = ShoppingTab.CALCULATOR
                        }
                    )
                }
                ShoppingTab.CALCULATOR -> {
                    CalculatorScreen(
                        initialPrice = calcPriceParam,
                        initialCurrency = calcCurrencyParam
                    )
                    // Reset params after loading to prevent sticky state
                    DisposableEffect(Unit) {
                        onDispose {
                            calcPriceParam = 0.0
                            calcCurrencyParam = "USD"
                        }
                    }
                }
                ShoppingTab.SIZE -> {
                    SizeScreen()
                }
                ShoppingTab.SIMULATOR -> {
                    SimulationScreen()
                }
                ShoppingTab.GUIDE -> {
                    HistoryScreen()
                }
            }
        }
    }
}
