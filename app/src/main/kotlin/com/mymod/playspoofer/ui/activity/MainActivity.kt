package com.mymod.playspoofer.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mymod.playspoofer.R
import com.mymod.playspoofer.ui.theme.PlaySpooferTheme
import com.mymod.playspoofer.xposed.statusIsModuleActivated

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PlaySpooferTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // 标题卡片
            Card(
            modifier = Modifier
                .fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {                Text(
                    text = stringResource(R.string.app_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // 模块状态
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isActivated = statusIsModuleActivated
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActivated) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.errorContainer
                            }
                        ),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {                        Text(
                            text = if (isActivated) stringResource(R.string.status_activated) else stringResource(R.string.status_not_activated),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActivated) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onErrorContainer
                            }
                        )
                    }
                      Text(
                        text = if (isActivated) {
                            stringResource(R.string.status_activated_desc)
                        } else {
                            stringResource(R.string.status_not_activated_desc)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                  // 使用说明
                Text(
                    text = stringResource(R.string.usage_instructions),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = stringResource(R.string.usage_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = stringResource(R.string.step_1),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Text(
                    text = stringResource(R.string.step_2),
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = stringResource(R.string.step_3),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                // 验证说明
                Text(
                    text = stringResource(R.string.verify_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.verify_old_version),
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.verify_new_version),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val context = LocalContext.current
        OutlinedButton(
            onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/byemaxx/PlayVersionSpoofer"))
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.visit_project_homepage))
        }
    }
}
