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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.FolderShared
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import id.gustyxganteng.agent.R
import id.gustyxganteng.agent.core.database.AppSettingsRepository
import id.gustyxganteng.agent.core.designsystem.M3OnPrimary
import id.gustyxganteng.agent.core.designsystem.M3OutlineVariant
import id.gustyxganteng.agent.core.designsystem.M3Primary
import id.gustyxganteng.agent.core.designsystem.M3PrimaryContainer
import id.gustyxganteng.agent.core.designsystem.M3Surface
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainer
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerHigh
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLow
import id.gustyxganteng.agent.core.designsystem.M3SurfaceContainerLowest
import id.gustyxganteng.agent.core.designsystem.M3TextPrimary
import id.gustyxganteng.agent.core.designsystem.M3TextSecondary
import id.gustyxganteng.agent.core.model.PromptMode
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    settingsRepository: AppSettingsRepository,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 4 })

    // Permission States
    var hasStoragePermission by remember { mutableStateOf(checkStoragePermission(context)) }
    var hasNotificationPermission by remember { mutableStateOf(checkNotificationPermission(context)) }
    var hasCameraPermission by remember { mutableStateOf(checkCameraPermission(context)) }

    // User Preferences during setup
    var selectedPromptMode by remember { mutableStateOf(PromptMode.AGENT) }
    var requireDiffApproval by remember { mutableStateOf(true) } // yoloMode = !requireDiffApproval

    // Lifecycle Observer to auto-refresh permissions on return from Android Settings
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

    // Permission Request Launchers
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

    Surface(
        color = M3Surface,
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Stepper Pill Progress Header (Pixel OOBE Style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) { pageIndex ->
                    val isSelected = pagerState.currentPage == pageIndex
                    val pillWidth by animateDpAsState(
                        targetValue = if (isSelected) 32.dp else 8.dp,
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pillWidth"
                    )
                    val pillColor by animateColorAsState(
                        targetValue = if (isSelected) M3Primary else M3OutlineVariant.copy(alpha = 0.4f),
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        label = "pillColor"
                    )

                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(pillWidth)
                            .clip(CircleShape)
                            .background(pillColor)
                    )
                }
            }

            // Pager Content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                when (page) {
                    0 -> OnboardingWelcomePage()
                    1 -> OnboardingPermissionsPage(
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
                    2 -> OnboardingPreferencesPage(
                        selectedMode = selectedPromptMode,
                        onSelectMode = { selectedPromptMode = it },
                        requireDiffApproval = requireDiffApproval,
                        onToggleDiffApproval = { requireDiffApproval = it }
                    )
                    3 -> OnboardingReadyPage(
                        grantedCount = listOf(hasStoragePermission, hasNotificationPermission, hasCameraPermission).count { it },
                        selectedMode = selectedPromptMode,
                        requireDiffApproval = requireDiffApproval
                    )
                }
            }

            // Bottom Navigation Bar (Pixel OOBE Navigation)
            Surface(
                color = M3Surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Action: Back or Skip
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
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        TextButton(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(3)
                                }
                            }
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_btn_skip),
                                color = M3TextSecondary
                            )
                        }
                    }

                    // Right Action: Next or Start Exploration
                    if (pagerState.currentPage < 3) {
                        Button(
                            onClick = {
                                scope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3Primary,
                                contentColor = M3OnPrimary
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.onboarding_btn_next),
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                scope.launch {
                                    settingsRepository.updatePromptMode(selectedPromptMode)
                                    settingsRepository.updateYoloMode(!requireDiffApproval)
                                    settingsRepository.completeOnboarding()
                                    onFinished()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = M3Primary,
                                contentColor = M3OnPrimary
                            ),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.RocketLaunch,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.onboarding_btn_start),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingWelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(Modifier.height(12.dp))

        // Hero Icon Squircle
        ExpressiveHeroBadge(
            icon = Icons.Rounded.AutoAwesome,
            containerColor = M3PrimaryContainer,
            iconTint = M3Primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_step_welcome_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = M3TextPrimary
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_step_welcome_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = M3TextSecondary
        )

        Spacer(Modifier.height(28.dp))

        // Feature Value Cards
        FeatureValueCard(
            icon = Icons.Rounded.Terminal,
            accentColor = Color(0xFF64FFDA),
            title = "Autonomous Tool Execution",
            description = "Agen mengeksekusi shell terminal, git commit, dan modifikasi berkas workspace secara mandiri."
        )

        Spacer(Modifier.height(12.dp))

        FeatureValueCard(
            icon = Icons.Rounded.Web,
            accentColor = Color(0xFF82B1FF),
            title = "Interactive Live Canvas",
            description = "Live runner untuk website Vanilla JS, React 18, dan simulasi UI Android Jetpack Compose."
        )

        Spacer(Modifier.height(12.dp))

        FeatureValueCard(
            icon = Icons.Rounded.Psychology,
            accentColor = Color(0xFFFFD180),
            title = "Multi-Agent Neural Ensemble",
            description = "Kolaborasi model cerdas Xemarux Router (Kimi k2.8 & DeepSeek v4.1) bersama Agnes 3.0 Flash."
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPermissionsPage(
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
        Spacer(Modifier.height(12.dp))

        ExpressiveHeroBadge(
            icon = Icons.Rounded.Security,
            containerColor = M3SurfaceContainerHigh,
            iconTint = M3Primary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_step_permissions_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = M3TextPrimary
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_step_permissions_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = M3TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        // Permission Card 1: Storage (Workspace)
        PermissionItemCard(
            icon = Icons.Rounded.FolderShared,
            title = stringResource(R.string.onboarding_perm_storage_title),
            description = stringResource(R.string.onboarding_perm_storage_desc),
            isGranted = hasStorage,
            isRequired = true,
            onRequest = onRequestStorage
        )

        Spacer(Modifier.height(12.dp))

        // Permission Card 2: Notifications
        PermissionItemCard(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.onboarding_perm_notification_title),
            description = stringResource(R.string.onboarding_perm_notification_desc),
            isGranted = hasNotification,
            isRequired = false,
            onRequest = onRequestNotification
        )

        Spacer(Modifier.height(12.dp))

        // Permission Card 3: Camera
        PermissionItemCard(
            icon = Icons.Rounded.PhotoCamera,
            title = stringResource(R.string.onboarding_perm_camera_title),
            description = stringResource(R.string.onboarding_perm_camera_desc),
            isGranted = hasCamera,
            isRequired = false,
            onRequest = onRequestCamera
        )

        Spacer(Modifier.height(16.dp))

        // Local Privacy Notice
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = M3SurfaceContainerLow,
            border = BorderStroke(1.dp, M3OutlineVariant.copy(alpha = 0.5f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = Color(0xFF81C784),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Aman & Privat: Seluruh berkas workspace diproses secara lokal di perangkat Anda tanpa tracking terselubung.",
                    style = MaterialTheme.typography.labelSmall.copy(lineHeight = 16.sp),
                    color = M3TextSecondary
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingPreferencesPage(
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
        Spacer(Modifier.height(12.dp))

        ExpressiveHeroBadge(
            icon = Icons.Rounded.Tune,
            containerColor = M3SurfaceContainerHigh,
            iconTint = Color(0xFFFFD180)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_step_preferences_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = M3TextPrimary
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_step_preferences_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = M3TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "PILIH MODE AWAL",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp),
            color = M3Primary
        )

        Spacer(Modifier.height(8.dp))

        // Mode Choice: Agent
        SelectableModeCard(
            title = "Mode Agent (Disarankan)",
            subtitle = "Eksekusi berkas otonom, git, shell command, dan inspeksi kode langsung di folder proyek.",
            isSelected = selectedMode == PromptMode.AGENT,
            onClick = { onSelectMode(PromptMode.AGENT) }
        )

        Spacer(Modifier.height(10.dp))

        // Mode Choice: Chat
        SelectableModeCard(
            title = "Mode Chat Biasa",
            subtitle = "Percakapan dialog cepat, tanya-jawab programming, dan eksplorasi ide tanpa folder workspace.",
            isSelected = selectedMode == PromptMode.CHAT,
            onClick = { onSelectMode(PromptMode.CHAT) }
        )

        Spacer(Modifier.height(20.dp))

        // Diff Approval Safeguard
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = M3SurfaceContainerLow,
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
                        text = "Diff Approval (Human-in-the-Loop)",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = M3TextPrimary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "AI akan menampilkan dialog diff konfirmasi sebelum menulis file atau mengeksekusi shell.",
                        style = MaterialTheme.typography.bodySmall,
                        color = M3TextSecondary
                    )
                }

                Switch(
                    checked = requireDiffApproval,
                    onCheckedChange = onToggleDiffApproval,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = M3OnPrimary,
                        checkedTrackColor = M3Primary
                    )
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        // Context Window 150K Token Highlight Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = M3SurfaceContainerLowest,
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
                    modifier = Modifier.size(40.dp)
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
                        text = "Kapasitas Context Window: 150K Token",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF64FFDA)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Dikalibrasi optimal pada 150.000 token untuk efisiensi komputasi kilat dan bebas lag di Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = M3TextSecondary
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun OnboardingReadyPage(
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
        Spacer(Modifier.height(12.dp))

        ExpressiveHeroBadge(
            icon = Icons.Rounded.CheckCircle,
            containerColor = Color(0xFF81C784).copy(alpha = 0.16f),
            iconTint = Color(0xFF81C784)
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.onboarding_step_ready_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = M3TextPrimary
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboarding_step_ready_subtitle),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = M3TextSecondary
        )

        Spacer(Modifier.height(24.dp))

        // Summary Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = M3SurfaceContainerHigh,
            border = BorderStroke(1.dp, M3OutlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    text = "RINGKASAN KONFIGURASI",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    ),
                    color = M3Primary
                )

                Spacer(Modifier.height(12.dp))

                SummaryRow(
                    label = "Izin Sistem Aktif",
                    value = "$grantedCount dari 3 Izin Siap",
                    statusColor = if (grantedCount >= 1) Color(0xFF81C784) else Color(0xFFFFB4AB)
                )

                HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                SummaryRow(
                    label = "Mode Awal",
                    value = if (selectedMode == PromptMode.AGENT) "Agent Coding (Otonom)" else "Chat Cepat",
                    statusColor = M3TextPrimary
                )

                HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                SummaryRow(
                    label = "Keamanan Eksekusi",
                    value = if (requireDiffApproval) "Human-in-the-Loop (Disetujui)" else "YOLO Mode (Otomatis)",
                    statusColor = if (requireDiffApproval) Color(0xFF64FFDA) else Color(0xFFFFD180)
                )

                HorizontalDivider(color = M3OutlineVariant.copy(alpha = 0.4f), modifier = Modifier.padding(vertical = 10.dp))

                SummaryRow(
                    label = "Context Window Ceiling",
                    value = "150.000 Token (150K)",
                    statusColor = Color(0xFF82B1FF)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExpressiveHeroBadge(
    icon: ImageVector,
    containerColor: Color,
    iconTint: Color
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = Spring.StiffnessMediumLow),
        label = "heroScale"
    )

    Surface(
        shape = RoundedCornerShape(26.dp),
        color = containerColor,
        border = BorderStroke(1.dp, M3OutlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .size(76.dp)
            .scale(scale)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
private fun FeatureValueCard(
    icon: ImageVector,
    accentColor: Color,
    title: String,
    description: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = M3SurfaceContainerLow,
        border = BorderStroke(1.dp, M3OutlineVariant.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = M3TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                    color = M3TextSecondary
                )
            }
        }
    }
}

@Composable
private fun PermissionItemCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onRequest: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = if (isGranted) Color(0xFF81C784) else Color(0xFFFFD180),
        label = "permStatusColor"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = M3SurfaceContainerLow,
        border = BorderStroke(1.dp, if (isGranted) Color(0xFF81C784).copy(alpha = 0.4f) else M3OutlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.padding(end = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = M3TextPrimary
                        )
                        if (isRequired) {
                            Spacer(Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = M3Primary.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "WAJIB",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = M3Primary,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                        color = M3TextSecondary
                    )
                }
            }

            // Action: Granted Badge vs Button
            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF81C784).copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color(0xFF81C784).copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784)
                        )
                    }
                }
            } else {
                FilledTonalButton(
                    onClick = onRequest,
                    shape = RoundedCornerShape(16.dp),
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
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
private fun SelectableModeCard(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) M3Primary else M3OutlineVariant.copy(alpha = 0.6f),
        label = "modeBorderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) M3PrimaryContainer.copy(alpha = 0.16f) else M3SurfaceContainerLow,
        label = "modeContainerColor"
    )

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
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
                        color = if (isSelected) M3Primary else M3OutlineVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(M3Primary)
                    )
                }
            }

            Spacer(Modifier.width(14.dp))

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = M3TextPrimary
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = M3TextSecondary
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(
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
