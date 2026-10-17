package com.example.chessbinge.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.mutableStateOf
import com.example.chessbinge.R
import java.util.concurrent.ConcurrentHashMap

enum class ChessSound {
    MOVE,
    CAPTURE,
    CHECK,
    VICTORY,
    DEFEAT,
    CLICK,
    SUCCESS,
    GAME_START
}

object SoundManager {
    private var soundPool: SoundPool? = null
    private val soundMap = mutableMapOf<ChessSound, Int>()
    private val loadedSamples = ConcurrentHashMap.newKeySet<Int>()
    private var vibrator: Vibrator? = null

    val isSoundEnabled = mutableStateOf(true)
    val isVibrationEnabled = mutableStateOf(true)

    fun initialize(context: Context) {
        if (soundPool != null) return

        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val pool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(attributes)
            .build()

        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) {
                loadedSamples.add(sampleId)
            }
        }

        soundMap[ChessSound.MOVE] = pool.load(context, R.raw.token_step, 1)
        soundMap[ChessSound.CAPTURE] = pool.load(context, R.raw.capture, 1)
        soundMap[ChessSound.CHECK] = pool.load(context, R.raw.token_pop, 1)
        soundMap[ChessSound.VICTORY] = pool.load(context, R.raw.victory, 1)
        soundMap[ChessSound.DEFEAT] = pool.load(context, R.raw.game_over_lost, 1)
        soundMap[ChessSound.CLICK] = pool.load(context, R.raw.wheel_click, 1)
        soundMap[ChessSound.SUCCESS] = pool.load(context, R.raw.success, 1)
        soundMap[ChessSound.GAME_START] = pool.load(context, R.raw.match_start_2, 1)

        soundPool = pool

        // Initialize Vibrator
        vibrator = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (_: Exception) {
            null
        }
    }

    fun play(sound: ChessSound, pitch: Float = 1.0f) {
        if (isVibrationEnabled.value) {
            when (sound) {
                ChessSound.MOVE -> vibrate(25)
                ChessSound.CAPTURE -> vibrate(60)
                ChessSound.CHECK -> vibrate(80)
                ChessSound.VICTORY -> vibrate(120)
                ChessSound.DEFEAT -> vibrate(100)
                ChessSound.CLICK -> vibrate(15)
                ChessSound.SUCCESS -> vibrate(45)
                ChessSound.GAME_START -> vibrate(40)
            }
        }

        if (!isSoundEnabled.value) return
        val id = soundMap[sound] ?: return
        val pool = soundPool ?: return

        if (loadedSamples.contains(id)) {
            pool.play(id, 1f, 1f, 1, 0, pitch)
        }
    }

    fun vibrate(durationMs: Long) {
        if (!isVibrationEnabled.value) return
        try {
            val v = vibrator ?: return
            if (!v.hasVibrator()) return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                v.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }

    fun toggleSound(): Boolean {
        isSoundEnabled.value = !isSoundEnabled.value
        return isSoundEnabled.value
    }

    fun toggleVibration(): Boolean {
        isVibrationEnabled.value = !isVibrationEnabled.value
        return isVibrationEnabled.value
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        loadedSamples.clear()
        soundMap.clear()
    }
}
