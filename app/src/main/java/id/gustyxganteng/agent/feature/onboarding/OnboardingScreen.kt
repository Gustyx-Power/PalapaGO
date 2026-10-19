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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.SwitchDefaults
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
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.model.PromptMode
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private val PixelBackgroundDark = Color(0xFF111216)
private val PixelSolidCardColor = Color(0xFF1B1D22)
private val PixelPillButtonColor = Color(0xFFA8C7FA)
private val PixelPillButtonText = Color(0xFF041C35)
private val PixelStrokeLavender = Color(0xFFC4B5FD)
private val PixelStrokeRose = Color(0xFFF48FB1)
private val PixelSuccessGreen = Color(0xFF81C784)

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
            .background(PixelBackgroundDark)
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
                            tint = M3TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.onboarding_btn_back),
                            color = M3TextSecondary,
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
                        containerColor = PixelPillButtonColor,
                        contentColor = PixelPillButtonText,
                        disabledContainerColor = Color(0xFF262A33),
                        disabledContentColor = Color(0xFF6B7280)
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
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, M3OutlineVariant.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
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
            color = Color(0xFFF2F0F4)
        )

        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displayMedium.copy(
                fontSize = 48.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 54.sp
            ),
            color = Color(0xFFF2F0F4)
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
                tint = Color(0xFFD0BCFF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_language),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFFE2E1E6)
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
                tint = Color(0xFFD0BCFF),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.onboarding_welcome_platform),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = Color(0xFFE2E1E6)
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
            color = Color(0xFFF2F0F4)
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.onboarding_step_permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
            color = M3TextSecondary
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
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF241C15),
                border = BorderStroke(1.dp, Color(0xFFFFB74D).copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFFB74D),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.onboarding_perm_mandatory_notice),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                        color = Color(0xFFFFE0B2)
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
            color = Color(0xFFF2F0F4)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_step_preferences_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = M3TextSecondary
        )

        Spacer(Modifier.height(22.dp))

        Text(
            text = stringResource(R.string.onboarding_pref_section_mode),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
            color = Color(0xFFD0BCFF)
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
            shape = RoundedCornerShape(18.dp),
            color = PixelSolidCardColor,
            border = BorderStroke(1.dp, M3OutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        color = Color(0xFFF2F0F4)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.onboarding_pref_diff_desc),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = M3TextSecondary
                    )
                }

                Switch(
                    checked = requireDiffApproval,
                    onCheckedChange = onToggleDiffApproval,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PixelPillButtonText,
                        checkedTrackColor = PixelPillButtonColor
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // 150K Token Calibration Notice Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = PixelSolidCardColor,
            border = BorderStroke(1.dp, Color(0xFF64FFDA).copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF64FFDA).copy(alpha = 0.12f),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = Color(0xFF64FFDA),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(Modifier.width(14.dp))

                Column {
                    Text(
                        text = stringResource(R.string.onboarding_pref_context_title),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF64FFDA)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.onboarding_pref_context_desc),
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = M3TextSecondary
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
            border = BorderStroke(1.dp, PixelSuccessGreen.copy(alpha = 0.4f)),
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
            color = Color(0xFFF2F0F4)
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboarding_step_ready_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = M3TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PixelSolidCardColor,
            border = BorderStroke(1.dp, M3OutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = stringResource(R.string.onboarding_ready_summary_title),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = Color(0xFFD0BCFF)
                )

                Spacer(Modifier.height(14.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_perm),
                    value = "$grantedCount dari 3 Izin Diberikan",
                    statusColor = PixelSuccessGreen
                )

                HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_mode),
                    value = if (selectedMode == PromptMode.AGENT) "Agent Coding Otonom" else "Chat Cepat",
                    statusColor = Color(0xFFF2F0F4)
                )

                HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_diff),
                    value = if (requireDiffApproval) "Human-in-the-Loop" else "Auto-Approve",
                    statusColor = if (requireDiffApproval) Color(0xFF64FFDA) else Color(0xFFFFD180)
                )

                HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                PixelSummaryRow(
                    label = stringResource(R.string.onboarding_ready_summary_context),
                    value = "150.000 Token (150K)",
                    statusColor = Color(0xFF82B1FF)
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
    val borderColor by animateColorAsState(
        targetValue = if (isGranted) PixelSuccessGreen.copy(alpha = 0.5f) else M3OutlineVariant,
        label = "permBorderColor"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = PixelSolidCardColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isGranted) PixelSuccessGreen.copy(alpha = 0.15f) else Color(0xFF262A33),
                modifier = Modifier.size(38.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isGranted) PixelSuccessGreen else Color(0xFFD0BCFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

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
                        color = Color(0xFFF2F0F4),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isMandatory) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFFFB4AB).copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_perm_mandatory_badge),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB4AB),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = M3TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isGranted) {
                Surface(
                    shape = CircleShape,
                    color = PixelSuccessGreen.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, PixelSuccessGreen.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = PixelSuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.onboarding_perm_granted),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            ),
                            color = PixelSuccessGreen
                        )
                    }
                }
            } else {
                Button(
                    onClick = onRequest,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PixelPillButtonColor,
                        contentColor = PixelPillButtonText
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = stringResource(R.string.onboarding_perm_grant_btn),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
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
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) PixelPillButtonColor else M3OutlineVariant,
        label = "optionBorderColor"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = PixelSolidCardColor,
        border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .border(
                        width = 2.dp,
                        color = if (isSelected) PixelPillButtonColor else M3OutlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(PixelPillButtonColor)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFF2F0F4)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 19.sp),
                    color = M3TextSecondary
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
            color = M3TextSecondary
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
            color = PixelStrokeLavender.copy(alpha = 0.55f),
            style = Stroke(width = 1.8.dp.toPx())
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
            color = PixelStrokeRose.copy(alpha = 0.45f),
            style = Stroke(width = 1.6.dp.toPx())
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
            color = PixelStrokeRose.copy(alpha = 0.35f),
            style = Stroke(width = 1.4.dp.toPx())
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
