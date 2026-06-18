package com.google.jetpackcamera.core.camera.mfs

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class MfsConfigTest {

    @Test
    fun compute_brightLight_usesFewFrames() {
        val config = MfsConfig.compute(
            sensorMP = 12f,
            lightLevel = LightLevel.BRIGHT,
            zoomFactor = 1f
        )
        assertThat(config.frameCount <= 6).isTrue()
        assertThat(config.mergeStrategy).isNotNull()
    }

    @Test
    fun compute_veryLowLight_usesManyFrames() {
        val config = MfsConfig.compute(
            sensorMP = 12f,
            lightLevel = LightLevel.VERY_LOW,
            zoomFactor = 1f
        )
        assertThat(config.frameCount >= 8).isTrue()
        assertThat(config.preFilterStrength > 0f).isTrue()
    }

    @Test
    fun compute_highZoom_increasesFrameCount() {
        val noZoom = MfsConfig.compute(12f, LightLevel.NORMAL, 1f)
        val highZoom = MfsConfig.compute(12f, LightLevel.NORMAL, 10f)
        assertThat(highZoom.frameCount >= noZoom.frameCount).isTrue()
    }

    @Test
    fun compute_highIso_boostsFrameCount() {
        val lowIso = MfsConfig.compute(12f, LightLevel.NORMAL, 1f, liveIso = 200)
        val highIso = MfsConfig.compute(12f, LightLevel.NORMAL, 1f, liveIso = 2000)
        assertThat(highIso.frameCount >= lowIso.frameCount).isTrue()
    }

    @Test
    fun compute_frameCount_clampedToMax() {
        val config = MfsConfig.compute(1f, LightLevel.VERY_LOW, 10f, liveIso = 5000)
        assertThat(config.frameCount <= 16).isTrue()
    }

    @Test
    fun compute_frameCount_atLeastOne() {
        val config = MfsConfig.compute(100f, LightLevel.BRIGHT, 1f)
        assertThat(config.frameCount >= 1).isTrue()
    }

    @Test
    fun compute_frameGapMs_nonZeroForMultiFrame() {
        val config = MfsConfig.compute(12f, LightLevel.NORMAL, 1f)
        if (config.frameCount > 1) {
            assertThat(config.frameGapMs >= 20L).isTrue()
        }
    }

    @Test
    fun compute_frameGapMs_zeroForSingleFrame() {
        val config = MfsConfig.compute(100f, LightLevel.BRIGHT, 1f)
        if (config.frameCount <= 1) {
            assertThat(config.frameGapMs).isEqualTo(0L)
        }
    }

    @Test
    fun compute_frameGapMs_doesNotExceedTotalLimit() {
        val config = MfsConfig.compute(12f, LightLevel.VERY_LOW, 5f, liveIso = 5000)
        val maxAllowed = 3000L / config.frameCount
        assertThat(config.frameGapMs <= maxAllowed).isTrue()
    }

    @Test
    fun compute_lowIso_hasLowerDenoise() {
        val lowIso = MfsConfig.compute(12f, LightLevel.LOW, 1f, liveIso = 200)
        val highIso = MfsConfig.compute(12f, LightLevel.LOW, 1f, liveIso = 2000)
        assertThat(lowIso.denoiseStrength < highIso.denoiseStrength).isTrue()
    }

    @Test
    fun compute_highZoom_increasesSharpen() {
        val lowZoom = MfsConfig.compute(12f, LightLevel.NORMAL, 1f)
        val highZoom = MfsConfig.compute(12f, LightLevel.NORMAL, 10f)
        assertThat(highZoom.sharpenStrength >= lowZoom.sharpenStrength).isTrue()
    }
}
