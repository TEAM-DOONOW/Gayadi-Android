package com.gayadi.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.gayadi.android.domain.model.AgentChangeProposal
import com.gayadi.android.domain.model.AgentProposalOption
import com.gayadi.android.ui.components.GayadiTopAppBar
import com.gayadi.android.ui.theme.AlertBlue
import com.gayadi.android.ui.theme.AlertBlueText
import com.gayadi.android.ui.theme.Border
import com.gayadi.android.ui.theme.GayadiTheme
import com.gayadi.android.ui.theme.PrimaryAction
import com.gayadi.android.ui.theme.SurfaceCard
import com.gayadi.android.ui.theme.TextPrimary
import com.gayadi.android.ui.theme.TextSecondary

@Composable
fun AgentScreen(
    tripName: String?,
    uiState: AgentUiState,
    onBack: () -> Unit,
    onAnalyze: () -> Unit,
    onRetry: () -> Unit,
    onSelectOption: (String, String) -> Unit,
    onApprove: (AgentChangeProposal) -> Unit,
    onReject: (AgentChangeProposal) -> Unit,
) {
    Column(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.White)) {
        GayadiTopAppBar(title = "가야디 에이전트", onBack = onBack, showDivider = true)
        if (tripName == null) {
            EmptyAgentContent("진행할 여행이 없어요", "여행을 만든 뒤 에이전트가 상황을 확인해 드릴게요.")
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = AlertBlue),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = AlertBlueText)
                            Spacer(Modifier.size(8.dp))
                            Text("$tripName 상황 도우미", fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "날씨와 혼잡도를 확인하고 필요한 경우 대체 장소와 일정 변경안을 추천해요.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onAnalyze,
                            enabled = !uiState.isAnalyzing,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (uiState.isAnalyzing) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = androidx.compose.ui.graphics.Color.White,
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.size(8.dp))
                                Text("분석 중")
                            } else {
                                Text("현재 상황 분석하기")
                            }
                        }
                    }
                }
            }

            uiState.errorMessage?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(message, modifier = Modifier.weight(1f), color = TextPrimary)
                            TextButton(onClick = onRetry) {
                                Icon(Icons.Filled.Refresh, contentDescription = null)
                                Text("다시 시도")
                            }
                        }
                    }
                }
            }

            uiState.latestResponse?.let { response ->
                item {
                    AgentSummaryCard(
                        summary = response.situationSummary,
                        nextAction = response.nextAction,
                        reasoning = response.reasoning,
                    )
                }
                if (response.recommendations.isNotEmpty()) {
                    item { SectionTitle("추천 대안") }
                    items(response.recommendations, key = { it.placeId.ifBlank { it.name } }) { place ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                                Text(place.name, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text(place.category, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = AlertBlueText)
                                if (place.reason.isNotBlank()) {
                                    Spacer(Modifier.height(6.dp))
                                    Text(place.reason, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            item { SectionTitle("에이전트 알림") }
            when {
                uiState.isLoading -> item {
                    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.Center) {
                        CircularProgressIndicator(color = PrimaryAction)
                    }
                }
                uiState.proposals.isEmpty() -> item {
                    EmptyAgentCard("새로운 변경 알림이 없어요", "상황 분석을 실행하면 필요한 대안을 여기에 보여드려요.")
                }
                else -> items(uiState.proposals, key = AgentChangeProposal::id) { proposal ->
                    ProposalCard(
                        proposal = proposal,
                        selectedOptionKey = uiState.selectedOptionKeys[proposal.id],
                        isDeciding = uiState.decidingProposalId == proposal.id,
                        onSelectOption = { onSelectOption(proposal.id, it) },
                        onApprove = { onApprove(proposal) },
                        onReject = { onReject(proposal) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AgentSummaryCard(summary: String, nextAction: String, reasoning: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("분석 결과", fontWeight = FontWeight.Bold, color = TextPrimary)
            if (summary.isNotBlank()) Text(summary, color = TextPrimary)
            if (nextAction.isNotBlank()) Text(nextAction, color = AlertBlueText)
            if (reasoning.isNotBlank()) {
                Text(reasoning, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

@Composable
private fun ProposalCard(
    proposal: AgentChangeProposal,
    selectedOptionKey: String?,
    isDeciding: Boolean,
    onSelectOption: (String) -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
) {
    val pending = proposal.status == "PENDING"
    Card(
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Border),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("일정 변경 제안", fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(proposal.status.toKoreanStatus(), color = if (pending) AlertBlueText else TextSecondary)
            }
            if (proposal.reason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(proposal.reason, color = TextSecondary)
            }
            proposal.options.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = pending) { onSelectOption(option.key) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = selectedOptionKey == option.key,
                        onClick = { if (pending) onSelectOption(option.key) },
                        enabled = pending,
                    )
                    Column(Modifier.weight(1f)) {
                        Text(option.placeName, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        if (option.description.isNotBlank()) {
                            Text(option.description, style = androidx.compose.material3.MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }
            }
            if (pending) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onReject, enabled = !isDeciding, modifier = Modifier.weight(1f)) {
                        Text("거절")
                    }
                    Button(
                        onClick = onApprove,
                        enabled = !isDeciding && !selectedOptionKey.isNullOrBlank(),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryAction),
                    ) {
                        Text(if (isDeciding) "처리 중" else "적용")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = androidx.compose.material3.MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
}

@Composable
private fun EmptyAgentCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceCard), shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text(description, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun EmptyAgentContent(title: String, description: String) {
    Column(
        Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.AutoAwesome,
            contentDescription = null,
            tint = AlertBlueText,
            modifier = Modifier.background(AlertBlue, CircleShape).padding(14.dp).size(28.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(Modifier.height(6.dp))
        Text(description, color = TextSecondary)
    }
}

private fun String.toKoreanStatus(): String = when (this) {
    "PENDING" -> "확인 필요"
    "APPROVED" -> "적용됨"
    "REJECTED" -> "거절됨"
    "EXPIRED" -> "만료됨"
    else -> this
}

@Preview(showBackground = true)
@Composable
private fun AgentScreenPreview() {
    GayadiTheme {
        AgentScreen(
            tripName = "서울 여행",
            uiState = AgentUiState(
                proposals = listOf(
                    AgentChangeProposal(
                        id = "1",
                        reason = "비 예보로 실내 장소를 추천했어요.",
                        status = "PENDING",
                        baseRevisionNo = 1,
                        options = listOf(
                            AgentProposalOption("museum", "2", "국립중앙박물관", "실내 관람이 가능해요.", true),
                        ),
                    ),
                ),
            ),
            onBack = {},
            onAnalyze = {},
            onRetry = {},
            onSelectOption = { _, _ -> },
            onApprove = {},
            onReject = {},
        )
    }
}
