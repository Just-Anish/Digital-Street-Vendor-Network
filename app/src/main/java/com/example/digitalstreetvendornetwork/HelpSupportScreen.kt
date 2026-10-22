package com.example.digitalstreetvendornetwork

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.digitalstreetvendornetwork.ui.components.EmptyState

const val SUPPORT_EMAIL = "support@localvends.example.com"
const val SUPPORT_PHONE = "+919876543210"

/**
 * Data model for a FAQ item referencing string resource IDs.
 */
data class FaqResource(
    val questionResId: Int,
    val answerResId: Int
)

/**
 * Resolved FAQ item with localized strings for query filtering and display.
 */
data class ResolvedFaq(
    val id: Int,
    val question: String,
    val answer: String
)

/**
 * Interactive Help & Support screen supporting both Customer and Vendor roles.
 * Provides direct contact shortcuts (Email / Call) and a searchable list of expandable FAQs.
 */
@Composable
fun HelpSupportScreen(
    isVendor: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var expandedFaqIds by remember { mutableStateOf(setOf<Int>()) }

    // Define FAQ resource definitions depending on role
    val faqDefinitions = remember(isVendor) {
        if (isVendor) {
            listOf(
                FaqResource(R.string.faq_vendor_q_products, R.string.faq_vendor_a_products),
                FaqResource(R.string.faq_vendor_q_location, R.string.faq_vendor_a_location),
                FaqResource(R.string.faq_vendor_q_orders, R.string.faq_vendor_a_orders),
                FaqResource(R.string.faq_vendor_q_earnings, R.string.faq_vendor_a_earnings),
                FaqResource(R.string.faq_vendor_q_stall_status, R.string.faq_vendor_a_stall_status),
                FaqResource(R.string.faq_vendor_q_upi, R.string.faq_vendor_a_upi)
            )
        } else {
            listOf(
                FaqResource(R.string.faq_customer_q_place_order, R.string.faq_customer_a_place_order),
                FaqResource(R.string.faq_customer_q_delivery, R.string.faq_customer_a_delivery),
                FaqResource(R.string.faq_customer_q_payment, R.string.faq_customer_a_payment),
                FaqResource(R.string.faq_customer_q_order_status, R.string.faq_customer_a_order_status),
                FaqResource(R.string.faq_customer_q_favourites, R.string.faq_customer_a_favourites),
                FaqResource(R.string.faq_customer_q_language, R.string.faq_customer_a_language)
            )
        }
    }

    // Resolve localized strings for display and searching
    val resolvedFaqs = faqDefinitions.map { faq ->
        ResolvedFaq(
            id = faq.questionResId,
            question = stringResource(faq.questionResId),
            answer = stringResource(faq.answerResId)
        )
    }

    // Filter FAQs dynamically by question or answer
    val filteredFaqs = remember(searchQuery, resolvedFaqs) {
        if (searchQuery.isBlank()) {
            resolvedFaqs
        } else {
            val query = searchQuery.trim().lowercase()
            resolvedFaqs.filter {
                it.question.lowercase().contains(query) || it.answer.lowercase().contains(query)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Pinned Header (matching CustomerSettingsScreen style: back arrow + title)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                stringResource(R.string.help_and_support),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contact Support Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.contact_support_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.contact_support_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = { openEmailSupport(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Email,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.email_support),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Button(
                                onClick = { openCallSupport(context) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.call_support),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(stringResource(R.string.search_faqs_hint)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_close),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // FAQ Section Header
            item {
                Text(
                    text = stringResource(R.string.frequently_asked_questions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                )
            }

            // FAQs List or Empty State
            if (filteredFaqs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Filled.SearchOff,
                            title = stringResource(R.string.no_faqs_found),
                            message = stringResource(R.string.no_faqs_found_sub),
                            actionLabel = stringResource(R.string.undo),
                            onAction = { searchQuery = "" }
                        )
                    }
                }
            } else {
                items(filteredFaqs, key = { it.id }) { faq ->
                    val isExpanded = expandedFaqIds.contains(faq.id)
                    ExpandableFaqCard(
                        faq = faq,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedFaqIds = if (isExpanded) {
                                expandedFaqIds - faq.id
                            } else {
                                expandedFaqIds + faq.id
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Expandable card displaying a FAQ question and answer.
 * Tapping toggles expansion with animateContentSize, and the chevron smoothly rotates.
 */
@Composable
fun ExpandableFaqCard(
    faq: ResolvedFaq,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val rotationState by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "FaqChevronRotation"
    )

    Card(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 250)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = faq.question,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.rotate(rotationState)
                )
            }
            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = faq.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = MaterialTheme.typography.bodyMedium.lineHeight
                )
            }
        }
    }
}

/**
 * Launches an email client targeting SUPPORT_EMAIL using Intent.ACTION_SENDTO with mailto:.
 */
private fun openEmailSupport(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:$SUPPORT_EMAIL")
            putExtra(Intent.EXTRA_SUBJECT, "Local Vends Support Request")
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.no_app_available),
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Launches the phone dialer targeting SUPPORT_PHONE using Intent.ACTION_DIAL.
 */
private fun openCallSupport(context: Context) {
    try {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$SUPPORT_PHONE")
        }
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(
            context,
            context.getString(R.string.no_app_available),
            Toast.LENGTH_SHORT
        ).show()
    }
}
