package com.wishring.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.wishring.app.ui.theme.*

/**
 * SNS 공유 다이얼로그
 * 사용자가 공유할 메시지와 해시태그를 입력할 수 있음
 */
@Composable
fun ShareDialog(
    count: Int,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shareMessage by remember { 
        mutableStateOf("오늘 나는 ${count}번 더 성장했습니다 ✨") 
    }
    var shareHashtags by remember { mutableStateOf("") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 제목
                Text(
                    text = "SNS 공유하기",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Text_Primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                // 메시지 입력
                Text(
                    text = "공유 메시지",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Text_Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = shareMessage,
                    onValueChange = { shareMessage = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    placeholder = {
                        Text(
                            text = "공유할 메시지를 입력하세요",
                            color = Text_Secondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple_Medium,
                        unfocusedBorderColor = Gray_Light,
                        cursorColor = Purple_Medium
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // 해시태그 입력
                Text(
                    text = "해시태그 (선택사항)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = Text_Primary,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = shareHashtags,
                    onValueChange = { shareHashtags = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "#WishRing #성장 #습관 등을 입력하세요",
                            color = Text_Secondary
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple_Medium,
                        unfocusedBorderColor = Gray_Light,
                        cursorColor = Purple_Medium
                    ),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 버튼 영역
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 취소 버튼
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Text_Primary
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Gray_Light
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "취소",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                    
                    // 공유 버튼
                    Button(
                        onClick = { 
                            onConfirm(shareMessage.trim(), shareHashtags.trim())
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple_Medium
                        ),
                        shape = RoundedCornerShape(8.dp),
                        enabled = shareMessage.trim().isNotEmpty()
                    ) {
                        Text(
                            text = "공유하기",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShareDialogPreview() {
    WishRingTheme {
        ShareDialog(
            count = 750,
            onConfirm = { _, _ -> },
            onDismiss = { }
        )
    }
}