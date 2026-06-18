/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.MfsState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CaptureButtonUiStateAdapterTest {
    private val defaultCameraAppSettings = CameraAppSettings(captureMode = CaptureMode.STANDARD)
    private val defaultCameraState = CameraState(isCameraRunning = true)

    @Test
    fun from_cameraNotRunning_returnsIdleAndDisabled() {
        val cameraState = CameraState(isCameraRunning = false)
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isFalse()
        assertThat((uiState as CaptureButtonUiState.Enabled.Idle).captureMode)
            .isEqualTo(CaptureMode.STANDARD)
    }

    @Test
    fun from_cameraRunning_recordingInactive_returnsIdleAndEnabled() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Inactive()
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isTrue()
        assertThat((uiState as CaptureButtonUiState.Enabled.Idle).captureMode)
            .isEqualTo(CaptureMode.STANDARD)
    }

    @Test
    fun from_cameraRunning_recordingPressed_returnsPressedRecording() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Active.Recording(0L, 0.0, 0L)
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState)
            .isInstanceOf(CaptureButtonUiState.Enabled.Recording.PressedRecording::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }

    @Test
    fun from_cameraRunning_recordingLocked_returnsLockedRecording() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Active.Recording(0L, 0.0, 0L)
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = true
        )

        assertThat(uiState)
            .isInstanceOf(CaptureButtonUiState.Enabled.Recording.LockedRecording::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }

    @Test
    fun from_cameraRunning_recordingStarting_returnsIdleAndEnabled() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Starting(null)
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }

    @Test
    fun from_mfsCapturing_returnsIdleAndDisabled() {
        val cameraState = defaultCameraState.copy(
            mfsState = MfsState.Capturing(3, 8)
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isFalse()
    }

    @Test
    fun from_mfsMerging_returnsIdleAndDisabled() {
        val cameraState = defaultCameraState.copy(
            mfsState = MfsState.Merging
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isFalse()
    }

    @Test
    fun from_mfsSaving_returnsIdleAndDisabled() {
        val cameraState = defaultCameraState.copy(
            mfsState = MfsState.Saving
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isFalse()
    }

    @Test
    fun from_mfsSaved_returnsIdleAndEnabled() {
        val cameraState = defaultCameraState.copy(
            mfsState = MfsState.Saved
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }

    @Test
    fun from_mfsFailed_returnsIdleAndEnabled() {
        val cameraState = defaultCameraState.copy(
            mfsState = MfsState.Failed("test error")
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Enabled.Idle::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }
}
