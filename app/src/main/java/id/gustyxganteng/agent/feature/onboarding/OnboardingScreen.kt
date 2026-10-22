package id.gustyxganteng.agent.feature.onboarding

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.model.PromptMode
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val PixelSuccessGreen = Color(0xFF81C784)
private val PixelAmber = Color(0xFFFFB74D)
private val PixelTeal = Color(0xFF64FFDA)
private val PixelAmberSoft = Color(0xFFFFE0B2)
private val PixelLavender = Color(0xFFD0BCFF)
private val PixelSky = Color(0xFF82B1FF)

@Composable
fun OnboardingScreen(
    settingsRepository: AppSettingsRepository,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    var hasStoragePermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasCameraPermission by remember { mutableStateOf(checkCameraPermission(context)) }

    var selectedPromptMode by remember { mutableStateOf(PromptMode.AGENT) }
    var requireDiffApproval by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasStoragePermission = checkStoragePermission(context)
                hasNotificationPermission = checkNotificationPermission(context)
                hasCameraPermission = checkCameraPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val storageSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        hasStoragePermission = checkStoragePermission(context)
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasNotificationPermission = isGranted
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    val legacyStorageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasStoragePermission = isGranted
    }

    val requestStorageAction = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uri = Uri.parse("package:${context.packageName}")
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri)
                storageSettingsLauncher.launch(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                storageSettingsLauncher.launch(fallbackIntent)
            }
        } else {
            legacyStorageLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    val canProceedFromPermissions = hasStoragePermission && hasNotificationPermission

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        ExpressivePixelOrganicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = if (pagerState.currentPage == 1) canProceedFromPermissions else true,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> PixelWelcomePage()
                    1 -> PixelPermissionsPage(
                        hasStorage = hasStoragePermission,
                        hasNotification = hasNotificationPermission,
                        hasCamera = hasCameraPermission,
                        onRequestStorage = requestStorageAction,
                        onRequestNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                hasNotificationPermission = true
                            }
                        },
                        onRequestCamera = {
                            cameraLauncher.launch(Manifest.permission.CAMERA)
                        }
                    )
                    2 -> PixelPreferencesPage(
                        selectedMode = selectedPromptMode,
                        onSelectMode = { selectedPromptMode = it },
                        requireDiffApproval = requireDiffApproval,
                        onToggleDiffApproval = { requireDiffApproval = it }
                    )
                    3 -> PixelReadyPage(
                        grantedCount = listOf(hasStoragePermission, hasNotificationPermission, hasCameraPermission).count { it },
                        selectedMode = selectedPromptMode,
                        requireDiffApproval = requireDiffApproval
                    )
                }
            }

            // Bottom Navigation Bar (Google Pixel OOBE Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage > 0) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.onboarding_btn_back),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Primary Pixel Pill Action Button
                val isNextEnabled = when (pagerState.currentPage) {
                    1 -> canProceedFromPermissions
                    else -> true
                }

                val nextButtonText = when (pagerState.currentPage) {
                    3 -> stringResource(R.string.onboarding_btn_start)
                    else -> stringResource(R.string.onboarding_btn_next)
                }

                Button(
                    onClick = {
                        if (pagerState.currentPage < 3) {
                            if (pagerState.currentPage == 1 && !canProceedFromPermissions) {
                                if (!hasStoragePermission) requestStorageAction()
                                else if (!hasNotificationPermission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        } else {
                            scope.launch {
                                settingsRepository.updatePromptMode(selectedPromptMode)
                                settingsRepository.updateYoloMode(!requireDiffApproval)
                                settingsRepository.completeOnboarding()
                                onFinished()
                            }
                        }
                    },
                    enabled = isNextEnabled,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = nextButtonText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelWelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp)
    ) {
        Spacer(Modifier.height(36.dp))

        // Real App Logo (from logo.png / app_logo.png)
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = stringResource(R.string.app_name),
            modifier = Modifier
                .size(76.dp)
                .clip(RoundedCornerShape(24.dp))
        )

        Spacer(Modifier.weight(1f))

        // Pixel OOBE Massive Confident Typography in Indonesian
        Text(
            text = "Selamat Datang di",
            style = MaterialTheme.typography.headlineLarge.copy(
                fontSize = 38.sp,
                fontWeight = FontWeight.Normal,
                lineHeight = 44.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 54.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(28.dp))

        // Pixel OOBE Feature Rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_language),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Terminal,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_platform),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(Modifier.height(36.dp))
    }
}

@Composable
private fun PixelPermissionsPage(
    hasStorage: Boolean,
    hasNotification: Boolean,
    hasCamera: Boolean,
    onRequestStorage: () -> Unit,
    onRequestNotification: () -> Unit,
    onRequestCamera: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboarding_step_permissions_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 36.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_step_permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(18.dp))

        // Mandatory Permission 1: Storage (Workspace)
        PixelPermissionM3Card(
            icon = Icons.Rounded.FolderShared,
            title = stringResource(R.string.onboarding_perm_storage_title),
            description = stringResource(R.string.onboarding_perm_storage_desc),
            isGranted = hasStorage,
            isMandatory = true,
            onRequest = onRequestStorage
        )

        Spacer(Modifier.height(14.dp))

        // Mandatory Permission 2: Notifications
        PixelPermissionM3Card(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.onboarding_perm_notification_title),
            description = stringResource(R.string.onboarding_perm_notification_desc),
            isGranted = hasNotification,
            isMandatory = true,
            onRequest = onRequestNotification
        )

        Spacer(Modifier.height(14.dp))

        // Optional Permission 3: Camera
        PixelPermissionM3Card(
            icon = Icons.Rounded.PhotoCamera,
            title = stringResource(R.string.onboarding_perm_camera_title),
            description = stringResource(R.string.onboarding_perm_camera_desc),
            isGranted = hasCamera,
            isMandatory = false,
            onRequest = onRequestCamera
        )

        Spacer(Modifier.height(18.dp))

        // Mandatory Permission Warning Banner
        if (!hasStorage || !hasNotification) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = PixelAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_perm_mandatory_notice),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = PixelAmberSoft
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PixelPreferencesPage(
    selectedMode: PromptMode,
    onSelectMode: (PromptMode) -> Unit,
    requireDiffApproval: Boolean,
    onToggleDiffApproval: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.onboarding_step_preferences_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_step_preferences_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.onboarding_pref_section_mode),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(10.dp))

        PixelSelectableOptionCard(
            title = stringResource(R.string.onboarding_pref_mode_agent_title),
            subtitle = stringResource(R.string.onboarding_pref_mode_agent_desc),
            isSelected = selectedMode == PromptMode.AGENT,
            onClick = { onSelectMode(PromptMode.AGENT) }
        )

        Spacer(Modifier.height(10.dp))

        PixelSelectableOptionCard(
            title = stringResource(R.string.onboarding_pref_mode_chat_title),
            subtitle = stringResource(R.string.onboarding_pref_mode_chat_desc),
            isSelected = selectedMode == PromptMode.CHAT,
            onClick = { onSelectMode(PromptMode.CHAT) }
        )

        Spacer(Modifier.height(20.dp))

        // Diff Approval Safeguard
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.onboarding_pref_diff_title),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_pref_diff_desc),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = requireDiffApproval,
                    onCheckedChange = onToggleDiffApproval
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 150K Token Calibration Notice Card
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Memory,
                    contentDescription = null,
                    tint = PixelTeal,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = stringResource(R.string.onboarding_pref_context_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = PixelTeal
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboarding_pref_context_desc),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PixelReadyPage(
    grantedCount: Int,
    selectedMode: PromptMode,
    requireDiffApproval: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(28.dp))

        Surface(
            shape = CircleShape,
            color = PixelSuccessGreen.copy(alpha = 0.15f),
            modifier = Modifier.size(64.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = PixelSuccessGreen,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.onboarding_step_ready_title),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 40.sp
            ),
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_step_ready_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_ready_summary_title),
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(14.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_perm),
                    value = "$grantedCount dari 3 Izin Diberikan",
                    statusColor = PixelSuccessGreen
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_mode),
                    value = if (selectedMode == PromptMode.AGENT) "Agent Coding Otonom" else "Chat Cepat",
                    statusColor = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_diff),
                    value = if (requireDiffApproval) "Human-in-the-Loop" else "Auto-Approve",
                    statusColor = if (requireDiffApproval) PixelTeal else PixelAmber
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_context),
                    value = "150.000 Token (150K)",
                    statusColor = PixelSky
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PixelPermissionM3Card(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isMandatory: Boolean,
    onRequest: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isGranted) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer,
        label = "permCardColor"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) PixelSuccessGreen else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isMandatory) {
                        Text(
                            text = stringResource(R.string.onboarding_perm_mandatory_badge),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isGranted) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = PixelSuccessGreen,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Button(
                    onClick = onRequest,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_perm_grant_btn),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun PixelSelectableOptionCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val cardColor by animateColorAsState(
        targetValue = if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLow,
        label = "optionCardColor"
    )

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = cardColor,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PixelSummaryRow(
    label: String,
    value: String,
    statusColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = statusColor
        )
    }
}

/**
 * Google Pixel Setup Wizard (OOBE) dynamic background:
 * 1. Rotating scalloped daisy outline stroke embracing punch hole camera.
 * 2. Sweeping bezier arc curves traversing the obsidian screen.
 */
@Composable
private fun ExpressivePixelOrganicBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "pixelOrganicMotion")

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 80000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scallopRotation"
    )

    val primaryStroke = MaterialTheme.colorScheme.primary

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        // 1. Scalloped Wavy Daisy Outline (Top camera area)
        val cx = width * 0.5f
        val cy = 36.dp.toPx()
        val baseRadius = 140.dp.toPx()
        val amplitude = 12.dp.toPx()
        val lobes = 14
        val rotationRad = Math.toRadians(rotationAngle.toDouble())

        val scallopPath = Path()
        val steps = 120
        for (i in 0..steps) {
            val theta = (i.toDouble() / steps.toDouble()) * (2.0 * Math.PI)
            val r = baseRadius + amplitude * cos(lobes * (theta + rotationRad))
            val x = (cx + r * cos(theta)).toFloat()
            val y = (cy + r * sin(theta)).toFloat()

            if (i == 0) {
                scallopPath.moveTo(x, y)
            } else {
                scallopPath.lineTo(x, y)
            }
        }
        scallopPath.close()

        drawPath(
            path = scallopPath,
            color = PixelLavender.copy(alpha = 0.3f),
            style = Stroke(width = 1.4.dp.toPx())
        )

        // 2. Large Sweeping Elliptical Arc
        val arcPath = Path().apply {
            moveTo(-40f, height * 0.58f)
            cubicTo(
                width * 0.3f, height * 0.32f,
                width * 0.75f, height * 0.38f,
                width + 60f, height * 0.48f
            )
        }

        drawPath(
            path = arcPath,
            color = primaryStroke.copy(alpha = 0.22f),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // 3. Subtle Secondary Sweeping Arc at bottom left
        val bottomArc = Path().apply {
            moveTo(-20f, height * 0.82f)
            cubicTo(
                width * 0.25f, height * 0.90f,
                width * 0.45f, height * 0.98f,
                width * 0.65f, height + 40f
            )
        }

        drawPath(
            path = bottomArc,
            color = primaryStroke.copy(alpha = 0.16f),
            style = Stroke(width = 1.2.dp.toPx())
        )
    }
}

private fun checkStoragePermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

private fun checkNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

private fun checkCameraPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
}
