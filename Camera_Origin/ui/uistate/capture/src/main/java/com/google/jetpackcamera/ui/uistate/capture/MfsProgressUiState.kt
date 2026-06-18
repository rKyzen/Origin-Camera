/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.uistate.capture

/**
 * Defines the UI state for multi-frame stacking (MFS) progress overlay.
 */
sealed interface MfsProgressUiState {

    /** No overlay should be displayed. */
    data object Hidden : MfsProgressUiState

    /** MFS is capturing burst frames. */
    data class Capturing(val capturedFrames: Int, val totalFrames: Int) : MfsProgressUiState

    /** MFS is processing (aligning & merging) captured frames. */
    data object Processing : MfsProgressUiState

    /** MFS is saving the final result. */
    data object Saving : MfsProgressUiState

    /** MFS capture was saved successfully. */
    data object Saved : MfsProgressUiState

    /** MFS capture failed. */
    data class Failed(val errorMessage: String) : MfsProgressUiState
}
