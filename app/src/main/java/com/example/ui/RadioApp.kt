package com.example.ui

import android.widget.Toast
import kotlinx.coroutines.delay
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.center
import androidx.compose.ui.geometry.center
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.RadioStation
import com.example.data.Recording
import com.example.media.PlaybackState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun ShakeDetector(
    onShake: () -> Unit
) {
    val context = LocalContext.current
    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        
        var lastUpdate: Long = 0
        var lastX = 0f
        var lastY = 0f
        var lastZ = 0f
        val shakeThreshold = 14.0f // Adjust sensitivity
        var lastShakeTime: Long = 0

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]

                    val curTime = System.currentTimeMillis()
                    if ((curTime - lastUpdate) > 100) {
                        val diffTime = curTime - lastUpdate
                        lastUpdate = curTime

                        // Calculate speed of movement
                        val speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000
                        if (speed > 800) { // Shake detected
                            if (curTime - lastShakeTime > 1500) { // Limit trigger frequency to 1.5s
                                lastShakeTime = curTime
                                onShake()
                            }
                        }
                        lastX = x
                        lastY = y
                        lastZ = z
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_UI)
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }
}

@Composable
fun DynamicGlowOrb(
    isPlaying: Boolean,
    isShaking: Boolean,
    modifier: Modifier = Modifier
) {
    // Shaking offset
    val shakeOffset = remember { Animatable(0f) }
    
    LaunchedEffect(isShaking) {
        if (isShaking) {
            repeat(4) {
                shakeOffset.animateTo(12f, animationSpec = tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-12f, animationSpec = tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, animationSpec = spring(dampingRatio = Spring.DampingRatioHighBouncy))
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "orb_pulse")
    
    // Breathing scale animation
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 700 else 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    // Rotating outer ring degrees
    val rotationDegrees by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 5000 else 15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_rotation"
    )

    // Counter rotating inner ring degrees
    val counterRotationDegrees by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 7000 else 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orb_counter_rotation"
    )

    // Wave ripple phase offset
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_offset"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = modifier
            .offset(x = shakeOffset.value.dp)
            .size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer pulsing glows
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .scale(pulseScale)
        ) {
            val centerPoint = size.center
            val baseRadius = size.minDimension / 4.8f
            
            drawCircle(
                color = primaryColor.copy(alpha = if (isPlaying) 0.12f else 0.04f),
                radius = baseRadius * 2.2f,
                center = centerPoint
            )
            drawCircle(
                color = secondaryColor.copy(alpha = if (isPlaying) 0.20f else 0.07f),
                radius = baseRadius * 1.7f,
                center = centerPoint
            )
            drawCircle(
                color = tertiaryColor.copy(alpha = if (isPlaying) 0.30f else 0.10f),
                radius = baseRadius * 1.3f,
                center = centerPoint
            )
        }

        // Concentric ring 1 (clockwise)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotationDegrees)
        ) {
            val centerPoint = size.center
            val baseRadius = size.minDimension / 4.8f
            
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(primaryColor, Color.Transparent, secondaryColor, Color.Transparent, primaryColor)
                ),
                radius = baseRadius * 1.5f,
                style = Stroke(width = 2.5.dp.toPx()),
                center = centerPoint
            )
            
            // Orbiting node dot
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = centerPoint.copy(x = centerPoint.x + baseRadius * 1.5f)
            )
        }

        // Concentric ring 2 (counter-clockwise)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(counterRotationDegrees)
        ) {
            val centerPoint = size.center
            val baseRadius = size.minDimension / 4.8f
            
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(tertiaryColor, Color.Transparent, primaryColor, Color.Transparent, tertiaryColor)
                ),
                radius = baseRadius * 1.25f,
                style = Stroke(width = 1.5.dp.toPx()),
                center = centerPoint
            )
            
            drawCircle(
                color = tertiaryColor,
                radius = 3.5.dp.toPx(),
                center = centerPoint.copy(y = centerPoint.y + baseRadius * 1.25f)
            )
        }

        // Glow center sphere core
        Canvas(
            modifier = Modifier
                .size(90.dp)
                .scale(pulseScale)
        ) {
            val centerPoint = size.center
            val radius = size.minDimension / 2.1f
            
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White,
                        secondaryColor.copy(alpha = 0.9f),
                        primaryColor.copy(alpha = 0.85f),
                        Color.Transparent
                    ),
                    center = centerPoint,
                    radius = radius
                ),
                radius = radius,
                center = centerPoint
            )
            
            // Draw responsive voice/sound ripples when playing
            if (isPlaying) {
                val wavePoints = 64
                val path = androidx.compose.ui.graphics.Path()
                for (i in 0 until wavePoints) {
                    val angle = (i.toFloat() / wavePoints) * 2f * Math.PI.toFloat()
                    val waveMultiplier = 1f + 0.08f * kotlin.math.sin(angle * 6f + waveOffset)
                    val r = radius * waveMultiplier
                    val x = centerPoint.x + r * kotlin.math.cos(angle)
                    val y = centerPoint.y + r * kotlin.math.sin(angle)
                    if (i == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }
                path.close()
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.5f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
        
        // Audio wave/radio icon inside the center of core
        Icon(
            imageVector = if (isPlaying) Icons.Default.MusicNote else Icons.Default.Radio,
            contentDescription = "Playback Wave Center Indicator",
            tint = Color.White,
            modifier = Modifier
                .size(28.dp)
                .graphicsLayer {
                    scaleX = if (isPlaying) 1.15f else 1.0f
                    scaleY = if (isPlaying) 1.15f else 1.0f
                }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RadioApp(viewModel: RadioViewModel) {
    var currentTab by remember { mutableStateOf(0) }
    val context = LocalContext.current

    // Observe State Flows
    val allStations by viewModel.allStations.collectAsStateWithLifecycle()
    val favoriteStations by viewModel.favoriteStations.collectAsStateWithLifecycle()
    val allRecordings by viewModel.allRecordings.collectAsStateWithLifecycle()

    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val playbackState by viewModel.playbackState.collectAsStateWithLifecycle()
    val currentStation by viewModel.currentPlayingStation.collectAsStateWithLifecycle()
    val currentRecording by viewModel.currentPlayingRecording.collectAsStateWithLifecycle()
    val playbackProgressMs by viewModel.playbackProgressMs.collectAsStateWithLifecycle()

    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val currentRecordingStation by viewModel.currentRecordingStation.collectAsStateWithLifecycle()
    val recordingDurationMs by viewModel.recordingDurationMs.collectAsStateWithLifecycle()
    val recordingFileSize by viewModel.recordingFileSize.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Radio,
                            contentDescription = "Radio Icon",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = "ВОЛНА",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Player") },
                    label = { Text("Эфир") },
                    modifier = Modifier.testTag("tab_player")
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Radio, contentDescription = "Stations") },
                    label = { Text("Станции") },
                    modifier = Modifier.testTag("tab_stations")
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Recordings") },
                    label = { Text("Записи") },
                    modifier = Modifier.testTag("tab_recordings")
                )
            }
        },
        floatingActionButton = {
            if (currentTab == 1) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_station_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Custom Station")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Content Screen depending on active Tab
                Box(modifier = Modifier.weight(1f)) {
                    when (currentTab) {
                        0 -> PlayerScreen(
                            isPlaying = isPlaying,
                            playbackState = playbackState,
                            currentStation = currentStation,
                            currentRecording = currentRecording,
                            playbackProgressMs = playbackProgressMs,
                            isRecording = isRecording,
                            currentRecordingStation = currentRecordingStation,
                            recordingDurationMs = recordingDurationMs,
                            recordingFileSize = recordingFileSize,
                            favoriteStations = favoriteStations,
                            onShuffle = { viewModel.shuffleStation() },
                            onPlayPause = { viewModel.togglePlayPause() },
                            onStop = { viewModel.stopPlayback() },
                            onToggleRecording = { station -> viewModel.toggleRecording(station) },
                            onStationSelect = { station -> viewModel.selectStation(station) },
                            onSeek = { pos -> viewModel.seekTo(pos) }
                        )
                        1 -> StationsScreen(
                            stations = allStations,
                            favoriteStations = favoriteStations,
                            currentPlayingStation = currentStation,
                            isPlaying = isPlaying,
                            onSelect = { station ->
                                viewModel.selectStation(station)
                                currentTab = 0 // Auto navigate to player
                            },
                            onToggleFavorite = { station -> viewModel.toggleFavorite(station) },
                            onDelete = { station -> viewModel.deleteStation(station) }
                        )
                        2 -> RecordingsScreen(
                            recordings = allRecordings,
                            currentPlayingRecording = currentRecording,
                            isPlaying = isPlaying,
                            onPlay = { recording ->
                                viewModel.playRecording(recording)
                                currentTab = 0 // Switch to player to see controls
                            },
                            onDelete = { recording -> viewModel.deleteRecording(recording) }
                        )
                    }
                }

                // Mini Player overlay displayed on other tabs when something is playing or loading
                if (currentTab != 0 && (currentStation != null || currentRecording != null)) {
                    MiniPlayer(
                        isPlaying = isPlaying,
                        playbackState = playbackState,
                        currentStation = currentStation,
                        currentRecording = currentRecording,
                        onPlayPause = { viewModel.togglePlayPause() },
                        onStop = { viewModel.stopPlayback() },
                        onTap = { currentTab = 0 }
                    )
                }
            }

            // Dialog for adding a Custom Station
            if (showAddDialog) {
                AddCustomStationDialog(
                    onDismiss = { showAddDialog = false },
                    onConfirm = { name, url, genre ->
                        viewModel.addCustomStation(name, url, genre)
                        showAddDialog = false
                        Toast.makeText(context, "Станция успешно добавлена", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

@Composable
fun PlayerScreen(
    isPlaying: Boolean,
    playbackState: PlaybackState,
    currentStation: RadioStation?,
    currentRecording: Recording?,
    playbackProgressMs: Long,
    isRecording: Boolean,
    currentRecordingStation: RadioStation?,
    recordingDurationMs: Long,
    recordingFileSize: Long,
    favoriteStations: List<RadioStation>,
    onShuffle: () -> RadioStation?,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onToggleRecording: (RadioStation) -> Unit,
    onStationSelect: (RadioStation) -> Unit,
    onSeek: (Long) -> Unit
) {
    val context = LocalContext.current
    var isOrbShaking by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Central Dynamic Glow Orb visualizer
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DynamicGlowOrb(
                    isPlaying = isPlaying && playbackState == PlaybackState.PLAYING,
                    isShaking = isOrbShaking,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Text(
                    text = "ПРЯМОЙ ЭФИР",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = currentStation?.name ?: currentRecording?.stationName ?: "Волна Радио",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = currentStation?.genre ?: if (currentRecording != null) "Записанный эфир" else "Выберите радиостанцию в меню",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Interactive "Shake to Shuffle" wave controller
        item {
            val haptic = LocalHapticFeedback.current

            // Custom lifecycle-aware shake detector
            ShakeDetector(
                onShake = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val shakenStation = onShuffle()
                    if (shakenStation != null) {
                        isOrbShaking = true
                        Toast.makeText(context, "📻 Радио встряхнули! Волна: ${shakenStation.name}", Toast.LENGTH_SHORT).show()
                    }
                }
            )

            // Auto reset shaking state after brief animation
            LaunchedEffect(isOrbShaking) {
                if (isOrbShaking) {
                    delay(800)
                    isOrbShaking = false
                }
            }

            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shake gesture",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier
                                .size(28.dp)
                                .padding(end = 8.dp)
                        )
                        Column {
                            Text(
                                text = "Встряхнуть радиоприёмник",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Встряхните телефон, чтобы случайно переключить волну!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val shakenStation = onShuffle()
                            if (shakenStation != null) {
                                isOrbShaking = true
                                Toast.makeText(context, "⚡ Волна переключена: ${shakenStation.name}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("shake_manually_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Manual Shuffle Icon",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "ВСТРЯХНУТЬ ВРУЧНУЮ ⚡",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Animated Equalizer Visualizer & Recording status
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisualizer(
                        isPlaying = isPlaying && playbackState == PlaybackState.PLAYING,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (playbackState == PlaybackState.PREPARING) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Подключение к потоку...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else if (playbackState == PlaybackState.ERROR) {
                        Text(
                            "Ошибка воспроизведения. Попробуйте еще раз.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    } else if (isPlaying) {
                        Text(
                            "В эфире • Высокое качество (HQ)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            "Пауза",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Recording indicator overlay
                    if (isRecording && currentRecordingStation != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        // Pulsing red record indicator
                        val infiniteTransition = rememberInfiniteTransition(label = "recording_pulse")
                        val pulseAlpha by infiniteTransition.animateFloat(
                            initialValue = 0.3f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse_alpha"
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.Red.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.FiberManualRecord,
                                contentDescription = "Запись",
                                tint = Color.Red.copy(alpha = pulseAlpha),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "ЗАПИСЬ HQ: ${formatDuration(recordingDurationMs)} (${formatFileSize(recordingFileSize)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Recording playback slider controls (Visible ONLY for local recordings)
        if (currentRecording != null) {
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = playbackProgressMs.toFloat(),
                        onValueChange = { onSeek(it.toLong()) },
                        valueRange = 0f..currentRecording.durationMs.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("recording_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatDuration(playbackProgressMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDuration(currentRecording.durationMs),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Interactive Player Controls
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Record Button (Disable for local recordings play)
                IconButton(
                    onClick = {
                        val station = currentStation
                        if (station != null) {
                            onToggleRecording(station)
                        } else {
                            Toast.makeText(context, "Для записи запустите радиостанцию", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Color.Red.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                        .testTag("record_button"),
                    enabled = currentStation != null
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = "Record",
                        tint = if (isRecording) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Play / Pause main button
                IconButton(
                    onClick = onPlayPause,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .testTag("play_pause_button"),
                    enabled = currentStation != null || currentRecording != null
                ) {
                    Icon(
                        imageVector = if (isPlaying && playbackState == PlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Stop Button
                IconButton(
                    onClick = onStop,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("stop_button"),
                    enabled = currentStation != null || currentRecording != null
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Favorite Stations Quick Access
        if (favoriteStations.isNotEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "Избранные станции",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(favoriteStations) { station ->
                            val isSelected = currentStation?.id == station.id
                            Card(
                                onClick = { onStationSelect(station) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                modifier = Modifier
                                    .width(130.dp)
                                    .testTag("fav_quick_${station.id}")
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Radio,
                                        contentDescription = "Station",
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = station.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = station.genre,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StationsScreen(
    stations: List<RadioStation>,
    favoriteStations: List<RadioStation>,
    currentPlayingStation: RadioStation?,
    isPlaying: Boolean,
    onSelect: (RadioStation) -> Unit,
    onToggleFavorite: (RadioStation) -> Unit,
    onDelete: (RadioStation) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedGenre by remember { mutableStateOf("Все") }

    // Derive all available genres + custom
    val genres = remember(stations) {
        val unique = stations.map { it.genre }.distinct().sorted()
        listOf("Все") + unique
    }

    val filteredStations = remember(searchQuery, selectedGenre, stations) {
        stations.filter { station ->
            val matchesSearch = station.name.contains(searchQuery, ignoreCase = true) ||
                    station.genre.contains(searchQuery, ignoreCase = true)
            val matchesGenre = selectedGenre == "Все" || station.genre == selectedGenre
            matchesSearch && matchesGenre
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Search Box
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Поиск станций или жанров...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Clear Search")
                    }
                }
            },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("station_search"),
            shape = RoundedCornerShape(12.dp)
        )

        // Genre filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genres) { genre ->
                FilterChip(
                    selected = selectedGenre == genre,
                    onClick = { selectedGenre = genre },
                    label = { Text(genre) },
                    modifier = Modifier.testTag("genre_chip_$genre")
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Stations List
        if (filteredStations.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Empty list",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Станции не найдены",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredStations, key = { it.id }) { station ->
                    val isPlayingThis = currentPlayingStation?.id == station.id
                    Card(
                        onClick = { onSelect(station) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("station_card_${station.id}"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPlayingThis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            else MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(12.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Status / Play Icon
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isPlayingThis) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlayingThis && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = if (isPlayingThis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // Name and Genre
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = station.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPlayingThis) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = station.genre,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Favorite Icon
                            IconButton(
                                onClick = { onToggleFavorite(station) },
                                modifier = Modifier.testTag("fav_btn_${station.id}")
                            ) {
                                Icon(
                                    imageVector = if (station.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "Toggle Favorite",
                                    tint = if (station.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Custom stations can be deleted
                            if (station.isCustom) {
                                IconButton(
                                    onClick = { onDelete(station) },
                                    modifier = Modifier.testTag("delete_btn_${station.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete custom station",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingsScreen(
    recordings: List<Recording>,
    currentPlayingRecording: Recording?,
    isPlaying: Boolean,
    onPlay: (Recording) -> Unit,
    onDelete: (Recording) -> Unit
) {
    if (recordings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = "No recordings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Записи отсутствуют",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Запишите прямой эфир любимого радио в высоком качестве",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(recordings, key = { it.id }) { recording ->
                val isPlayingThis = currentPlayingRecording?.id == recording.id
                Card(
                    onClick = { onPlay(recording) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recording_card_${recording.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPlayingThis) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Play Icon / Mic Indicator
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isPlayingThis) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlayingThis && isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = "Play Recording",
                                tint = if (isPlayingThis) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Details (Station, Timestamp, duration and file size)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = recording.stationName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = formatDateTime(recording.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Длина: ${formatDuration(recording.durationMs)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = formatFileSize(recording.fileSize),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Delete recording button
                        IconButton(
                            onClick = { onDelete(recording) },
                            modifier = Modifier.testTag("delete_rec_btn_${recording.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete recording",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MiniPlayer(
    isPlaying: Boolean,
    playbackState: PlaybackState,
    currentStation: RadioStation?,
    currentRecording: Recording?,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onTap: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable(onClick = onTap)
            .testTag("mini_player"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Radio,
                contentDescription = "Active playback icon",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(36.dp)
                    .padding(4.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = currentStation?.name ?: currentRecording?.stationName ?: "Волна",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (playbackState == PlaybackState.PREPARING) "Подключение..."
                    else if (currentRecording != null) "Воспроизведение записи"
                    else "Прямой эфир",
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1
                )
            }

            // Controls
            IconButton(onClick = onPlayPause) {
                if (playbackState == PlaybackState.PREPARING) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Icon(
                        imageVector = if (isPlaying && playbackState == PlaybackState.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "PlayPause"
                    )
                }
            }

            IconButton(onClick = onStop) {
                Icon(
                    imageVector = Icons.Default.Stop,
                    contentDescription = "Stop"
                )
            }
        }
    }
}

@Composable
fun AddCustomStationDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }

    var isErrorUrl by remember { mutableStateOf(false) }
    var isErrorName by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить станцию") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        isErrorName = it.isBlank()
                    },
                    label = { Text("Название станции") },
                    isError = isErrorName,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_name"),
                    supportingText = {
                        if (isErrorName) {
                            Text("Название не может быть пустым")
                        }
                    }
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        isErrorUrl = !it.startsWith("http://") && !it.startsWith("https://")
                    },
                    label = { Text("URL потока (http://...)") },
                    isError = isErrorUrl,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_url"),
                    supportingText = {
                        if (isErrorUrl) {
                            Text("URL должен начинаться с http:// или https://")
                        }
                    }
                )

                OutlinedTextField(
                    value = genre,
                    onValueChange = { genre = it },
                    label = { Text("Жанр (необязательно)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_input_genre")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val hasErrorName = name.isBlank()
                    val hasErrorUrl = !url.startsWith("http://") && !url.startsWith("https://")
                    
                    isErrorName = hasErrorName
                    isErrorUrl = hasErrorUrl

                    if (!hasErrorName && !hasErrorUrl) {
                        onConfirm(name.trim(), url.trim(), genre.trim())
                    }
                },
                modifier = Modifier.testTag("dialog_confirm_btn")
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_btn")
            ) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun AnimatedVisualizer(isPlaying: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "visualizer")
    val heights = listOf(0.4f, 0.7f, 0.5f, 0.9f, 0.3f, 0.8f, 0.6f, 0.95f, 0.5f, 0.7f, 0.4f, 0.6f)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, baseHeight ->
            val animatedHeight by if (isPlaying) {
                infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = baseHeight,
                    animationSpec = infiniteRepeatable(
                        animation = tween(
                            durationMillis = 350 + (index * 60) % 250,
                            easing = FastOutSlowInEasing
                        ),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$index"
                )
            } else {
                remember { mutableStateOf(0.15f) }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(animatedHeight)
                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    )
            )
        }
    }
}

// Helpers
fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.getDefault(), "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.getDefault(), "%.1f MB", mb)
}

fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
