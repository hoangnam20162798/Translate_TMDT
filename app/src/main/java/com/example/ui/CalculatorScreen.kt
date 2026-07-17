package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    initialPrice: Double = 0.0,
    initialCurrency: String = "USD",
    modifier: Modifier = Modifier
) {
    var priceInput by remember { mutableStateOf(if (initialPrice > 0.0) initialPrice.toString() else "") }
    var selectedCurrency by remember { mutableStateOf(initialCurrency) }
    var weightInput by remember { mutableStateOf("") }
    var domesticShippingInput by remember { mutableStateOf("") }
    var taxRateInput by remember { mutableStateOf("10") } // default 10% customs tax
    var intShippingRateInput by remember { mutableStateOf("200000") } // default 200,000 VND per kg
    var surchargeInput by remember { mutableStateOf("50000") } // default 50,000 VND handling fee

    // Current exchange rates to VND (Vietnamese Dong)
    val exchangeRates = mapOf(
        "USD" to 25400.0, // 1 USD = 25,400 VND
        "CNY" to 3500.0,  // 1 CNY = 3,500 VND
        "JPY" to 165.0,   // 1 JPY = 165 VND
        "EUR" to 27500.0  // 1 EUR = 27,500 VND
    )

    // Parse values safely
    val price = priceInput.toDoubleOrNull() ?: 0.0
    val weight = weightInput.toDoubleOrNull() ?: 0.0
    val domesticShipping = domesticShippingInput.toDoubleOrNull() ?: 0.0
    val taxRate = (taxRateInput.toDoubleOrNull() ?: 0.0) / 100.0
    val intShippingRate = intShippingRateInput.toDoubleOrNull() ?: 0.0
    val surcharge = surchargeInput.toDoubleOrNull() ?: 0.0

    val rate = exchangeRates[selectedCurrency] ?: 25400.0

    // Calculations
    val baseVnd = price * rate
    val domesticShippingVnd = domesticShipping * rate
    val subtotalVnd = baseVnd + domesticShippingVnd
    val taxVnd = subtotalVnd * taxRate
    val internationalShippingVnd = weight * intShippingRate
    val totalVnd = subtotalVnd + taxVnd + internationalShippingVnd + surcharge

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("vi-VN"))

    // Sync input when navigation arguments change
    LaunchedEffect(initialPrice, initialCurrency) {
        if (initialPrice > 0.0) {
            priceInput = initialPrice.toString()
        }
        if (initialCurrency.isNotEmpty() && exchangeRates.containsKey(initialCurrency)) {
            selectedCurrency = initialCurrency
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Screen Header
        Text(
            text = "Tính Giá Mua Sắm AI",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )
        Text(
            text = "Quy đổi giá gốc, tính phí cân nặng, thuế & dịch vụ về Việt Nam",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        // Cost Calculation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .testTag("calculator_result_card"),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "TỔNG GIÁ VỀ TAY (VND CHẠM NGÕ)",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.2.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currencyFormatter.format(totalVnd),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))

                Spacer(modifier = Modifier.height(12.dp))

                // Breakdown list
                CostRow(label = "Tiền hàng gốc:", value = currencyFormatter.format(baseVnd))
                if (domesticShippingVnd > 0) {
                    CostRow(label = "Vận chuyển nội địa:", value = currencyFormatter.format(domesticShippingVnd))
                }
                if (taxVnd > 0) {
                    CostRow(label = "Thuế hải quan (${(taxRate * 100).toInt()}%):", value = currencyFormatter.format(taxVnd))
                }
                if (internationalShippingVnd > 0) {
                    CostRow(label = "Cước bay quốc tế (${weight}kg):", value = currencyFormatter.format(internationalShippingVnd))
                }
                if (surcharge > 0) {
                    CostRow(label = "Phí mua hộ & phụ phí:", value = currencyFormatter.format(surcharge))
                }
            }
        }

        Text(
            text = "THÔNG SỐ ĐẦU VÀO",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
        )

        // Inputs Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Price & Currency Selection Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = priceInput,
                        onValueChange = { priceInput = it },
                        modifier = Modifier
                            .weight(0.6f)
                            .testTag("calc_price_input"),
                        label = { Text("Giá sản phẩm") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Payments, contentDescription = null)
                        }
                    )

                    // Currency selector chips
                    Column(
                        modifier = Modifier.weight(0.4f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Tiền tệ", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            exchangeRates.keys.forEach { currency ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (selectedCurrency == currency) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .clickable { selectedCurrency = currency }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currency,
                                        fontSize = 11.sp,
                                        color = if (selectedCurrency == currency) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Weight and Domestic shipping
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_weight_input"),
                        label = { Text("Cân nặng (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Scale, contentDescription = null)
                        }
                    )

                    OutlinedTextField(
                        value = domesticShippingInput,
                        onValueChange = { domesticShippingInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_dom_shipping_input"),
                        label = { Text("Ship nội địa ($selectedCurrency)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Customs Tax & Int. Shipping Rate & Surcharge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = taxRateInput,
                        onValueChange = { taxRateInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("calc_tax_input"),
                        label = { Text("Thuế Hải quan (%)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Percent, contentDescription = null)
                        }
                    )

                    OutlinedTextField(
                        value = intShippingRateInput,
                        onValueChange = { intShippingRateInput = it },
                        modifier = Modifier
                            .weight(1.2f)
                            .testTag("calc_int_shipping_rate_input"),
                        label = { Text("Giá bay/kg (VND)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.FlightTakeoff, contentDescription = null)
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = surchargeInput,
                    onValueChange = { surchargeInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("calc_surcharge_input"),
                    label = { Text("Phí dịch vụ & Phụ phí mua hộ (VND)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.SupportAgent, contentDescription = null)
                    }
                )
            }
        }

        // Help Tips Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Help,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tỷ giá hối đoái tham khảo:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "💵 1 USD ≈ 25.400 VND\n🇨🇳 1 CNY ≈ 3.500 VND\n🇯🇵 1 JPY ≈ 165 VND\n🇪🇺 1 EUR ≈ 27.500 VND",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
fun CostRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
