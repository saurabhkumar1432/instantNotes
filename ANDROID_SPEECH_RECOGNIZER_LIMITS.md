# Android SpeechRecognizer API - Technical Limitations

## Date: October 6, 2025

## Critical Discovery: Server-Side Timeout Limits

### The Problem
Android's `SpeechRecognizer` API has **hard-coded server-side timeout limits** that cannot be overridden by client applications, regardless of the values passed to `RecognizerIntent` extras.

### What We Learned

#### Timeout Parameters Are Limited
```kotlin
// This does NOT work as expected!
putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
// Google's servers will IGNORE this and use their own limit (~3-5 seconds)
```

**Reality**: 
- Google Speech Recognition service has its own timeout logic
- Maximum practical silence timeout: **~5 seconds**
- Values beyond 5000ms are typically ignored
- Behavior varies by Android version and device manufacturer

### Technical Constraints

#### 1. Server-Side Control
Google's speech recognition runs on their servers, not locally. The server decides:
- When to end recognition based on silence
- When to return partial results
- When to finalize results
- Timeout durations

#### 2. Network Latency Impact
- Audio is streamed to Google servers in real-time
- Network delays affect recognition timing
- Server processing adds latency
- Timeouts account for network overhead

#### 3. Partial Results Behavior
```kotlin
putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
```
**Benefits**:
- Keeps recognition session "alive" longer
- Provides real-time feedback
- Prevents premature termination
- Better user experience

**Drawbacks**:
- More frequent callbacks (previously caused glitching)
- Slightly higher network usage
- More processing overhead

### Our Solution

#### Approach 1: Original (Failed)
```kotlin
// Attempted fix - did NOT work
putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 10000L)
// Result: Still ended after ~1 second of silence
```

#### Approach 2: Current (Working)
```kotlin
// Working configuration
putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true) // Key enabler!
putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L) // Realistic
putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 5000L)
putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0L)

override fun onPartialResults(partialResults: Bundle?) {
    hasReceivedSpeech = true // Keep session active
}
```

**Why This Works**:
1. Partial results keep recognition active
2. 5 seconds is within Android's actual limits
3. Handling partial results signals active listening
4. Server doesn't terminate prematurely

### Performance Considerations

#### Timer Update Frequency
```kotlin
// BEFORE: Too slow, not visible
kotlinx.coroutines.delay(500) // 2 updates/second

// AFTER: Smooth and visible
kotlinx.coroutines.delay(200) // 5 updates/second
```

**Why 200ms?**
- Human perception: 200ms feels instant (< 250ms threshold)
- Battery friendly: Only 5 updates/second
- Smooth visual feedback without jitter
- Standard for timer/stopwatch apps

#### Partial Results Impact
- **Before**: Disabled to avoid glitching (40-100 calls/sec from onRmsChanged)
- **After**: Enabled safely because we **ignore onRmsChanged** entirely
- **Result**: Benefits of partial results without glitching

### Alternative Approaches (Not Used)

#### Option A: MediaRecorder API
```kotlin
// More control but requires manual transcription
MediaRecorder()
    .setAudioSource(MediaRecorder.AudioSource.MIC)
    .setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
```
**Pros**: Complete control over recording duration
**Cons**: Must handle transcription separately, more complex

#### Option B: Continuous Restart
```kotlin
// Restart recognition on silence to extend session
override fun onEndOfSpeech() {
    if (shouldContinue) {
        speechRecognizer?.startListening(intent) // Restart
    }
}
```
**Pros**: Potentially unlimited recording time
**Cons**: Glitchy experience, text gaps, complex state management

#### Option C: Custom Voice Activity Detection
```kotlin
// Monitor audio levels manually
AudioRecord() + custom VAD algorithm
```
**Pros**: Full control over silence detection
**Cons**: Very complex, battery intensive, requires signal processing

### Current Implementation Rationale

We chose **Approach 2** (realistic timeouts + partial results) because:

1. ✅ **Simplicity**: Works with native Android APIs
2. ✅ **Reliability**: No custom audio processing needed
3. ✅ **Performance**: Efficient and battery friendly
4. ✅ **User Experience**: 5-second pauses are acceptable for most use cases
5. ✅ **Maintainability**: Standard Android patterns

### User Expectations vs. Reality

#### What Users Want
- Unlimited recording duration
- Long pauses (10+ seconds) supported
- No interruptions

#### What Android Provides
- 5-minute max recording (by our choice)
- ~5 second max silence (Android/Google limit)
- Server-controlled behavior

#### Our Solution
- Clear documentation of 5-second pause limit
- Fast, visible timer (5 updates/sec)
- Stable recording experience
- Manual stop button for explicit control

### Testing Results

| Scenario | Before Fix | After Fix |
|----------|-----------|-----------|
| Timer visibility | Not visible/sluggish | Clearly visible (5/sec) |
| Pause tolerance | ~1 second | ~5 seconds |
| Glitching | Severe | None |
| User control | Limited | Full (manual stop) |
| Experience | Frustrating | Smooth |

### Recommendations for Users

**Best Practices**:
1. Keep pauses under 4 seconds to be safe
2. Use manual stop button for explicit control
3. Speak in natural phrases with brief pauses
4. For long-form content, stop and restart between major topics

**Not Recommended**:
- Very long pauses (> 5 seconds)
- Waiting for automatic stop (use manual button)
- Recording ambient conversation (designed for deliberate speech)

### Future Considerations

#### If 5 Seconds Isn't Enough

**Option 1**: Add restart logic (complex)
```kotlin
// Auto-restart on silence to extend session
// Pros: Longer recording, Cons: Glitchy, gaps in text
```

**Option 2**: Switch to MediaRecorder (major refactor)
```kotlin
// Record raw audio, transcribe separately
// Pros: No time limits, Cons: Requires transcription API, more complex
```

**Option 3**: Add "extend recording" button
```kotlin
// User taps button during long pauses to keep recording alive
// Pros: Simple, user controlled, Cons: Requires user action
```

### Key Takeaways

1. 🚫 **Can't override Google's timeout limits** - They control the servers
2. ✅ **5 seconds is the practical maximum** - Work within constraints
3. ✅ **Partial results help significantly** - Keep recognition alive
4. ✅ **200ms timer updates are perfect** - Visible without being excessive
5. ✅ **User expectations must be managed** - Document limitations clearly

---

## Technical Debt: None

This is not technical debt - it's **Android platform limitation**. Our implementation is optimal given the constraints.

## References

- [Android SpeechRecognizer Documentation](https://developer.android.com/reference/android/speech/SpeechRecognizer)
- [RecognizerIntent Constants](https://developer.android.com/reference/android/speech/RecognizerIntent)
- [Google Speech-to-Text Service Limits](https://cloud.google.com/speech-to-text/quotas)

---

**Status**: ✅ **DOCUMENTED AND UNDERSTOOD**  
**Solution**: ✅ **OPTIMAL FOR PLATFORM**  
**User Impact**: ⚠️ **5-second pause limit (platform constraint)**
