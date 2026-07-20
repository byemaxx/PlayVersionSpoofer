package com.mymod.playspoofer.ui.activity

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.mymod.playspoofer.BuildConfig
import com.mymod.playspoofer.R
import com.mymod.playspoofer.preferences.FallbackPreferences
import com.mymod.playspoofer.ui.theme.PlaySpooferTheme
import com.mymod.playspoofer.xposed.SpoofPolicy
import com.mymod.playspoofer.xposed.statusIsModuleActivated

class MainActivity : ComponentActivity() {
    private var playStoreVersionState by mutableStateOf<PlayStoreVersionState>(
        PlayStoreVersionState.Unavailable
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshPlayStoreVersion()
        setContent {
            PlaySpooferTheme {
                MainScreen(playStoreVersionState)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshPlayStoreVersion()
    }

    private fun refreshPlayStoreVersion() {
        playStoreVersionState = PlayStoreVersionReader.read(this)
    }
}

@Composable
private fun MainScreen(playStoreVersionState: PlayStoreVersionState) {
    val context = LocalContext.current
    val isActivated = statusIsModuleActivated
    var showLauncherIcon by remember { mutableStateOf(LauncherIconManager.isVisible(context)) }
    var legacyFallbackEnabled by remember {
        mutableStateOf(FallbackPreferences.isLegacyPackageInfoFallbackEnabled(context))
    }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    val openPlayStoreAppInfo = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            "package:${SpoofPolicy.TARGET_PACKAGE}".toUri()
        )
        context.startActivity(intent)
    }
    val openProjectHomepage = {
        val intent = Intent(
            Intent.ACTION_VIEW,
            "https://github.com/byemaxx/PlayVersionSpoofer".toUri()
        )
        context.startActivity(intent)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AppMenuBar(
                expanded = showMoreMenu,
                onExpandedChange = { showMoreMenu = it },
                onOpenAbout = {
                    showMoreMenu = false
                    showAboutDialog = true
                },
            )

            HeroCard(isActivated = isActivated)

            PlayStoreVersionCard(
                state = playStoreVersionState,
                onOpenPlayStoreSettings = openPlayStoreAppInfo,
            )

            VerificationCard()

            PreferenceSwitchRow(
                title = stringResource(R.string.legacy_fallback_title),
                summary = stringResource(R.string.legacy_fallback_summary),
                checked = legacyFallbackEnabled,
                onCheckedChange = { checked ->
                    legacyFallbackEnabled = checked
                    FallbackPreferences.setLegacyPackageInfoFallbackEnabled(context, checked)
                }
            )

            if (legacyFallbackEnabled) {
                InfoNotice(text = stringResource(R.string.legacy_fallback_warning))
            }

            PreferenceSwitchRow(
                title = stringResource(R.string.show_launcher_icon),
                summary = stringResource(R.string.show_launcher_icon_summary),
                checked = showLauncherIcon,
                onCheckedChange = { checked ->
                    showLauncherIcon = checked
                    LauncherIconManager.setVisible(context, checked)
                }
            )

            if (!showLauncherIcon) {
                InfoNotice(text = stringResource(R.string.launcher_icon_note_body))
            }

            AppFooter()
        }

        if (showAboutDialog) {
            AboutDialog(
                onDismiss = { showAboutDialog = false },
                onOpenProjectHomepage = openProjectHomepage,
            )
        }
    }
}

@Composable
private fun AppMenuBar(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onOpenAbout: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Box {
            val menuDescription = stringResource(R.string.more_options)
            IconButton(
                onClick = { onExpandedChange(true) },
                modifier = Modifier.semantics { contentDescription = menuDescription },
            ) {
                Text(
                    text = "\u22ee",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about_menu_item)) },
                    onClick = onOpenAbout,
                )
            }
        }
    }
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onOpenProjectHomepage: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.about_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.about_description),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.about_maintainer_prefix),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.about_maintainer_name),
                        modifier = Modifier.clickable(onClick = onOpenProjectHomepage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                    )
                    Text(
                        text = stringResource(R.string.about_maintainer_suffix),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.app_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenProjectHomepage) {
                Text(stringResource(R.string.project_homepage))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}

@Composable
private fun PlayStoreVersionCard(
    state: PlayStoreVersionState,
    onOpenPlayStoreSettings: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.play_store_version_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.installed_version),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))

            when (state) {
                is PlayStoreVersionState.Installed -> {
                    Text(
                        text = stringResource(
                            R.string.version_value,
                            state.versionName ?: stringResource(R.string.unknown_version),
                            state.versionCode,
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                PlayStoreVersionState.NotInstalled -> VersionStatusText(
                    text = stringResource(R.string.play_store_not_installed)
                )

                PlayStoreVersionState.Unavailable -> VersionStatusText(
                    text = stringResource(R.string.play_store_version_unavailable)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onOpenPlayStoreSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.open_play_store_settings))
            }
        }
    }
}

@Composable
private fun VersionStatusText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun HeroCard(
    isActivated: Boolean,
) {
    val statusContainer = if (isActivated) Color(0xFFE7F8EC) else Color(0xFFFCE8E8)
    val statusTextColor = if (isActivated) Color(0xFF166534) else Color(0xFFB42318)
    val heroBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF08131F),
            Color(0xFF134E4A),
            Color(0xFF15803D)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(heroBrush, RoundedCornerShape(28.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = statusContainer
            ) {
                Text(
                    text = if (isActivated) {
                        stringResource(R.string.status_activated)
                    } else {
                        stringResource(R.string.status_not_activated)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = statusTextColor
                )
            }

            Text(
                text = stringResource(R.string.app_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun VerificationCard() {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.details_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            StepLine(number = 1, text = stringResource(R.string.detail_setup))
            Spacer(modifier = Modifier.height(10.dp))
            StepLine(number = 2, text = stringResource(R.string.detail_verify))
        }
    }
}

@Composable
private fun AppFooter() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(
                R.string.app_version,
                BuildConfig.VERSION_NAME,
                BuildConfig.VERSION_CODE,
            ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PreferenceSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = checked,
                    role = Role.Switch,
                    onValueChange = onCheckedChange,
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = null
            )
        }
    }
}

@Composable
private fun InfoNotice(
    text: String,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer
        )
    }
}

@Composable
private fun StepLine(number: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
