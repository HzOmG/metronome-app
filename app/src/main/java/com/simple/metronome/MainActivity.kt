package com.simple.metronome

import android.media.SoundPool
import android.os.Bundle
import android.os.PowerManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    private lateinit var soundPool: SoundPool
    private var tickSoundId: Int = 0
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "Metronome::WakeLockTag")

        soundPool = SoundPool.Builder().setMaxStreams(4).build()
        tickSoundId = soundPool.load(this, android.R.raw.effect_tick, 1)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MetronomeScreen(
                        playNormal = {
                            soundPool.play(tickSoundId, 1f, 1f, 0, 0, 1.0f)
                        },
                        playHighTone = {
                            soundPool.play(tickSoundId, 1f, 1f, 0, 0, 1.6f)
                        },
                        playGroupFinishTone = {
                            soundPool.play(tickSoundId,1f,1f,0,0,1.8f)
                        },
                        keepScreenOn = {
                            if(!wakeLock!!.isHeld) wakeLock!!.acquire()
                        },
                        releaseScreenLock = {
                            if(wakeLock!!.isHeld) wakeLock!!.release()
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(wakeLock?.isHeld == true){
            wakeLock?.release()
        }
    }
}

@Composable
fun MetronomeScreen(
    playNormal: () -> Unit,
    playHighTone: () -> Unit,
    playGroupFinishTone: () -> Unit,
    keepScreenOn: () -> Unit,
    releaseScreenLock: () -> Unit
) {
    var isRunning by remember { mutableStateOf(false) }
    var isCountDown by remember { mutableStateOf(false) }
    var countDownValue by remember { mutableIntStateOf(0) }

    var totalSeconds by remember { mutableLongStateOf(0L) }
    var beatCount by remember { mutableIntStateOf(0) }
    var groupCount by remember { mutableIntStateOf(0) }

    val bpm = 19
    val beatIntervalMs = (60_000L / bpm)
    val maxBeatPerGroup = 76

    LaunchedEffect(isRunning) {
        if (isRunning) {
            keepScreenOn()
            isCountDown = true
            countDownValue = 5
            while (countDownValue > 0) {
                playNormal()
                delay(1000)
                countDownValue--
            }
            isCountDown = false

            playHighTone()

            while (beatCount < maxBeatPerGroup) {
                playNormal()
                beatCount += 1
                delay(beatIntervalMs)
                totalSeconds += beatIntervalMs / 1000L
            }

            playGroupFinishTone()
            groupCount += 1
            beatCount = 0
            isRunning = false
        } else {
            isCountDown = false
            releaseScreenLock()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = "总计时间", fontSize = 18.sp)
                Text(
                    text = formatTime(totalSeconds),
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "节拍次数", fontSize = 18.sp)
                Text(
                    text = "$beatCount",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "组数", fontSize = 18.sp)
                Text(
                    text = "$groupCount",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isCountDown) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = "准备 $countDownValue",
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { isRunning = !isRunning },
                modifier = Modifier.fillMaxWidth()
            ) {
                val btnText = when {
                    isCountDown -> "准备中…"
                    isRunning -> "停止"
                    else -> "开始"
                }
                Text(text = btnText, fontSize = 22.sp)
            }
        }

        if (!isRunning && !isCountDown) {
            Button(
                onClick = {
                    totalSeconds = 0L
                    beatCount = 0
                    groupCount = 0
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("重置清零", fontSize = 18.sp)
            }
        }
    }
}

fun formatTime(totalSec: Long): String {
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
