package com.pampoukidis.streamcoretv.feature.profiles.common.profiles

import com.pampoukidis.streamcore.sdk.api.ProfileService
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreError
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreResult
import com.pampoukidis.streamcore.sdk.model.error.StreamCoreContextFailureReason
import com.pampoukidis.streamcore.sdk.model.profile.*
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinFailure
import com.pampoukidis.streamcoretv.feature.profiles.common.pin.ProfilePinUiState
import com.pampoukidis.streamcoretv.feature.profiles.common.testing.ProfilesPreviewData
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProfilesViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() { Dispatchers.setMain(dispatcher) }

    @AfterTest
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun freshEntryUsesSdkDecisionOnceAndRefreshDoesNotEnterAgain(): TestResult {
        return runTest(dispatcher) {
            val service = FakeProfileService()
            val subject = ProfilesViewModel(service, autoEnterSingleProfile = true)
            val effects = mutableListOf<ProfilesEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { subject.effects.toList(effects) }
            advanceUntilIdle()
            assertEquals(1, service.entries)
            assertEquals(0, service.selections)
            assertEquals(service.profiles, subject.uiState.value.profiles)
            assertEquals(listOf<ProfilesEffect>(ProfilesEffect.EntryStarted), effects)
            subject.onAction(ProfilesAction.Refresh)
            advanceUntilIdle()
            assertEquals(1, service.entries)
            assertEquals(1, service.loads)
        }
    }

    @Test
    fun readyAndEmptyFreshEntryFollowSdkResults(): TestResult {
        return runTest(dispatcher) {
            val ready = FakeProfileService().apply { entry = StreamCoreResult.Success(StreamCoreProfileEntryReady(profile)) }
            val subject = ProfilesViewModel(ready, true)
            val effects = mutableListOf<ProfilesEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { subject.effects.toList(effects) }
            advanceUntilIdle()
            assertEquals(1, effects.filterIsInstance<ProfilesEffect.ProfileSelected>().size)
            assertEquals(0, ready.selections)
            assertEquals(listOf(ready.profile), subject.uiState.value.profiles)
            subject.onAction(ProfilesAction.RouteEntered)
            advanceUntilIdle()
            assertEquals(0, ready.loads, "The first route mount uses its completed initialization")
            subject.onAction(ProfilesAction.RouteEntered)
            advanceUntilIdle()
            assertEquals(1, ready.loads, "Returning from Home refreshes the chooser")
            assertEquals(1, ready.entries, "Returning never repeats automatic entry")
            val empty = ProfilesViewModel(FakeProfileService().apply { entry = StreamCoreResult.Success(StreamCoreProfileEntryNoProfiles) }, true)
            advanceUntilIdle()
            assertFalse(empty.uiState.value.isLoading)
            assertTrue(empty.uiState.value.profiles.isEmpty())
        }
    }

    @Test
    fun ordinaryEntryNeverAutoSelectsAndCancelDoesNotRepeatFreshEntry(): TestResult {
        return runTest(dispatcher) {
            val service = FakeProfileService()
            val ordinary = ProfilesViewModel(service)
            advanceUntilIdle()
            assertEquals(0, service.entries)
            assertEquals(0, service.selections)
            ordinary.onAction(ProfilesAction.SelectProfile(service.profile.id))
            advanceUntilIdle()
            assertEquals(1, service.selections)
            assertEquals(service.profile, ordinary.uiState.value.pin?.profile)
            ordinary.onAction(ProfilesAction.CancelPin)
            advanceUntilIdle()
            assertNull(ordinary.uiState.value.pin)
            assertEquals(service.profile.id, ordinary.uiState.value.restoreFocusProfileId)
            assertEquals(listOf("challenge"), service.cancelled)
            assertEquals(0, service.entries)
        }
    }

    @Test
    fun providerDigitCountAutoSubmitsOnceAndWrongPinClearsDraft(): TestResult {
        return runTest(dispatcher) {
            val service = FakeProfileService(digitCount = 6).apply { confirmation = { StreamCoreResult.Failure(StreamCoreError.PinRejected()) } }
            val subject = pinSubject(service)
            subject.onAction(ProfilesAction.PinDraftChanged("12345"))
            runCurrent()
            assertEquals(0, service.confirmations)
            subject.onAction(ProfilesAction.PinDraftChanged("123456"))
            subject.onAction(ProfilesAction.PinDraftChanged("123456"))
            subject.onAction(ProfilesAction.RetryPin)
            advanceUntilIdle()
            assertEquals(1, service.confirmations)
            assertEquals("", subject.uiState.value.pin?.draft)
            assertEquals(ProfilePinFailure.Incorrect, subject.uiState.value.pin?.failure)
            assertFalse(subject.uiState.value.pin!!.isSubmitting)
        }
    }

    @Test
    fun rapidDigitAndDeleteDeltasUseLatestDraftAndSubmitExactlyOnce(): TestResult {
        return runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val service = FakeProfileService().apply {
                confirmation = { gate.await(); StreamCoreResult.Success(profile) }
            }
            val subject = pinSubject(service)
            subject.onAction(ProfilesAction.PinDigitEntered(1))
            subject.onAction(ProfilesAction.PinDigitEntered(2))
            subject.onAction(ProfilesAction.PinDeleteDigit)
            subject.onAction(ProfilesAction.PinDeleteDigit)
            assertEquals("", subject.uiState.value.pin!!.draft)
            repeat(4) { subject.onAction(ProfilesAction.PinDigitEntered(7)) }
            assertEquals("7777", subject.uiState.value.pin!!.draft)
            assertTrue(subject.uiState.value.pin!!.isSubmitting)
            subject.onAction(ProfilesAction.PinDigitEntered(9))
            subject.onAction(ProfilesAction.PinDeleteDigit)
            subject.onAction(ProfilesAction.RetryPin)
            runCurrent()
            assertEquals(1, service.confirmations)
            assertEquals("7777", subject.uiState.value.pin!!.draft)
            assertEquals("PinDigitEntered(digit=<redacted>)", ProfilesAction.PinDigitEntered(7).toString())
            gate.complete(Unit)
            advanceUntilIdle()
            assertNull(subject.uiState.value.pin)
        }
    }

    @Test
    fun networkFailureRequiresExplicitRetryAndSuccessEmitsOnce(): TestResult {
        return runTest(dispatcher) {
            val service = FakeProfileService().apply { confirmation = { StreamCoreResult.Failure(StreamCoreError.Network()) } }
            val subject = pinSubject(service)
            val effects = mutableListOf<ProfilesEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { subject.effects.toList(effects) }
            subject.onAction(ProfilesAction.PinDraftChanged("1234"))
            advanceUntilIdle()
            assertTrue(subject.uiState.value.pin!!.canRetry)
            assertEquals("1234", subject.uiState.value.pin!!.draft)
            subject.onAction(ProfilesAction.PinDraftChanged("1234"))
            advanceUntilIdle()
            assertEquals(1, service.confirmations)
            service.confirmation = { StreamCoreResult.Success(service.profile) }
            subject.onAction(ProfilesAction.RetryPin)
            subject.onAction(ProfilesAction.RetryPin)
            advanceUntilIdle()
            assertEquals(2, service.confirmations)
            assertNull(subject.uiState.value.pin)
            assertEquals(1, effects.filterIsInstance<ProfilesEffect.ProfileSelected>().size)
        }
    }

    @Test
    fun backCancelsDuringVerificationAndIgnoresLateSuccess(): TestResult {
        return runTest(dispatcher) {
            val release = CompletableDeferred<Unit>()
            val service = FakeProfileService().apply {
                confirmation = { withContext(NonCancellable) { release.await(); StreamCoreResult.Success(profile) } }
            }
            val subject = pinSubject(service)
            val effects = mutableListOf<ProfilesEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { subject.effects.toList(effects) }
            subject.onAction(ProfilesAction.PinDraftChanged("1234"))
            runCurrent()
            assertTrue(subject.uiState.value.pin!!.isSubmitting)
            subject.onAction(ProfilesAction.CancelPin)
            assertNull(subject.uiState.value.pin)
            assertEquals(listOf("challenge"), service.cancelled)
            release.complete(Unit)
            advanceUntilIdle()
            assertTrue(effects.none { it is ProfilesEffect.ProfileSelected })
        }
    }

    @Test
    fun expiredChallengeReturnsToChooserWithoutExposingPin(): TestResult {
        return runTest(dispatcher) {
            val service = FakeProfileService().apply {
                confirmation = { StreamCoreResult.Failure(StreamCoreError.InvalidContext(reason = StreamCoreContextFailureReason.PinChallengeExpired)) }
            }
            val subject = pinSubject(service)
            val effects = mutableListOf<ProfilesEffect>()
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { subject.effects.toList(effects) }
            subject.onAction(ProfilesAction.PinDraftChanged("1234"))
            advanceUntilIdle()
            assertNull(subject.uiState.value.pin)
            assertEquals(1, effects.filterIsInstance<ProfilesEffect.ShowError>().size)
            assertFalse(ProfilePinUiState(service.profile, 4, "1234").toString().contains("1234"))
            assertFalse(ProfilesAction.PinDraftChanged("1234").toString().contains("1234"))
        }
    }

    private suspend fun kotlinx.coroutines.test.TestScope.pinSubject(service: FakeProfileService): ProfilesViewModel {
        service.entry = StreamCoreResult.Success(StreamCoreProfileEntryPinRequired(service.challenge))
        val subject = ProfilesViewModel(service, true)
        advanceUntilIdle()
        return subject
    }

    private class FakeProfileService(digitCount: Int = 4) : ProfileService {
        val profile = ProfilesPreviewData.profiles.first()
        val profiles = ProfilesPreviewData.profiles
        val challenge = StreamCoreProfilePinChallenge("challenge", profile, digitCount)
        var entry: StreamCoreResult<StreamCoreProfileEntryResult> = StreamCoreResult.Success(StreamCoreProfileEntryChooseProfile(profiles))
        var confirmation: suspend () -> StreamCoreResult<StreamCoreProfile> = { StreamCoreResult.Success(profile) }
        var entries = 0
        var loads = 0
        var selections = 0
        var confirmations = 0
        val cancelled = mutableListOf<String>()

        override suspend fun beginEntry(): StreamCoreResult<StreamCoreProfileEntryResult> { entries++; return entry }
        override suspend fun getProfiles(): StreamCoreResult<List<StreamCoreProfile>> { loads++; return StreamCoreResult.Success(profiles) }
        override suspend fun selectProfile(profileId: String): StreamCoreResult<StreamCoreProfileSelectionResult> { selections++; return StreamCoreResult.Success(StreamCoreProfileEntryPinRequired(challenge)) }
        override suspend fun confirmPin(challengeId: String, pin: String): StreamCoreResult<StreamCoreProfile> { confirmations++; return confirmation() }
        override fun cancelPin(challengeId: String): StreamCoreResult<Unit> { cancelled += challengeId; return StreamCoreResult.Success(Unit) }
        override suspend fun clearSelection(): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
        override suspend fun getProfileEditorOptions(): StreamCoreResult<StreamCoreProfileEditorOptions> { return StreamCoreResult.Success(ProfilesPreviewData.editorOptions) }
        override suspend fun createProfile(profile: StreamCoreCreateProfile): StreamCoreResult<StreamCoreProfile> { return StreamCoreResult.Success(this.profile) }
        override suspend fun updateProfile(profile: StreamCoreUpdateProfile): StreamCoreResult<StreamCoreProfile> { return StreamCoreResult.Success(this.profile) }
        override suspend fun deleteProfile(profileId: String): StreamCoreResult<Unit> { return StreamCoreResult.Success(Unit) }
    }
}
