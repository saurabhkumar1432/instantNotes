# Voice Notes AI - Image and Media Handling Strategy

## Overview
This document outlines the strategy for handling images, media files, and large binary data in the Voice Notes AI application to prevent memory issues and ensure optimal performance.

## Current State
**Version 1.0** of Voice Notes AI is a text-only application with no image handling. This document serves as a guide for future development when image features are added.

## Potential Use Cases

### 1. User Profile Pictures
**Status**: Not implemented  
**Consideration**: Small, circular avatars (max 500KB)

### 2. Note Attachments (Future Feature)
**Status**: Planned  
**Use Case**: Users want to attach images to notes for context

### 3. Image-to-Text Recognition (Future Feature)
**Status**: Planned  
**Use Case**: Extract text from photos using OCR, then process with AI

### 4. PDF Export with Images
**Status**: Planned  
**Use Case**: Export notes as PDF with embedded images

## Design Principles

### 1. Never Load Full Bitmaps into Memory
**Problem**: Loading high-resolution images (8MP+) can cause OutOfMemoryError

**Solution**:
```kotlin
// BAD - Loads full bitmap
val bitmap = BitmapFactory.decodeFile(imagePath)

// GOOD - Calculate and use inSampleSize
val options = BitmapFactory.Options().apply {
    inJustDecodeBounds = true
    BitmapFactory.decodeFile(imagePath, this)
    inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
    inJustDecodeBounds = false
}
val bitmap = BitmapFactory.decodeFile(imagePath, options)
```

### 2. Use Coil for Image Loading
**Library**: [Coil](https://coil-kt.github.io/coil/) - Kotlin-first image loading library

**Benefits**:
- Automatic memory management
- Disk and memory caching
- Compose integration
- Downsampling built-in
- Lifecycle-aware

**Implementation**:
```kotlin
// build.gradle.kts
implementation("io.coil-kt:coil-compose:2.4.0")

// Usage in Compose
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUri)
        .crossfade(true)
        .size(800, 600) // Automatically downsamples
        .build(),
    contentDescription = "Note attachment",
    modifier = Modifier.fillMaxWidth()
)
```

### 3. Store Images Externally, Not in Room Database
**Problem**: Storing BLOBs in SQLite degrades performance

**Solution**:
```kotlin
// Store file path, not bitmap
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val timestamp: Long,
    val imagePaths: String? = null // Comma-separated paths or JSON array
)
```

**File Storage Location**:
```kotlin
// Use app-specific directory (deleted on uninstall)
val imagesDir = File(context.filesDir, "note_images")
if (!imagesDir.exists()) imagesDir.mkdirs()

// Save with unique filename
val filename = "${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg"
val imageFile = File(imagesDir, filename)
```

### 4. Compress Images Before Storage
**Target**: Max 1920x1080, JPEG quality 85

**Implementation**:
```kotlin
fun compressImage(sourceUri: Uri, context: Context): File {
    val inputStream = context.contentResolver.openInputStream(sourceUri)
    
    // Decode with inSampleSize
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
        BitmapFactory.decodeStream(inputStream, null, this)
        inSampleSize = calculateInSampleSize(this, 1920, 1080)
        inJustDecodeBounds = false
    }
    
    inputStream?.close()
    val newInputStream = context.contentResolver.openInputStream(sourceUri)
    val bitmap = BitmapFactory.decodeStream(newInputStream, null, options)
    newInputStream?.close()
    
    // Compress to JPEG
    val outputFile = File(context.filesDir, "compressed_${System.currentTimeMillis()}.jpg")
    FileOutputStream(outputFile).use { out ->
        bitmap?.compress(Bitmap.CompressFormat.JPEG, 85, out)
    }
    
    bitmap?.recycle() // Free memory immediately
    return outputFile
}
```

### 5. Implement Image Caching Strategy

**Three-Level Cache**:
1. **Memory Cache**: LRU cache for recently viewed images (max 25% of available memory)
2. **Disk Cache**: Persistent cache in `cache/images/` directory
3. **Original Files**: In `files/note_images/` directory

**Coil Handles This Automatically**:
```kotlin
val imageLoader = ImageLoader.Builder(context)
    .memoryCache {
        MemoryCache.Builder(context)
            .maxSizePercent(0.25) // 25% of available memory
            .build()
    }
    .diskCache {
        DiskCache.Builder()
            .directory(context.cacheDir.resolve("image_cache"))
            .maxSizeBytes(50 * 1024 * 1024) // 50 MB
            .build()
    }
    .build()
```

## Memory Management Best Practices

### 1. Always Recycle Bitmaps
```kotlin
fun processBitmap(bitmap: Bitmap) {
    try {
        // Process bitmap
    } finally {
        bitmap.recycle() // Free native memory
    }
}
```

### 2. Use WeakReferences for Cached Bitmaps
```kotlin
class ImageCache {
    private val cache = mutableMapOf<String, WeakReference<Bitmap>>()
    
    fun get(key: String): Bitmap? = cache[key]?.get()
    
    fun put(key: String, bitmap: Bitmap) {
        cache[key] = WeakReference(bitmap)
    }
}
```

### 3. Handle Configuration Changes
```kotlin
// Don't store Bitmaps in ViewModel (survives config changes)
// DO store file paths or URIs
class NoteViewModel : ViewModel() {
    private val _imagePaths = MutableStateFlow<List<String>>(emptyList())
    val imagePaths: StateFlow<List<String>> = _imagePaths
}
```

### 4. Implement Pagination for Image Lists
```kotlin
// When displaying many images, use LazyColumn with pagination
@Composable
fun ImageGallery(images: List<String>) {
    LazyColumn {
        items(images) { imagePath ->
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
    }
}
```

## OCR Integration (Future Feature)

### Recommended Library: ML Kit Text Recognition
```kotlin
// build.gradle.kts
implementation("com.google.mlkit:text-recognition:16.0.0")

// Usage
val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

fun recognizeText(bitmap: Bitmap): Task<Text> {
    val image = InputImage.fromBitmap(bitmap, 0)
    return recognizer.process(image)
        .addOnSuccessListener { visionText ->
            val extractedText = visionText.text
            // Pass to AI for note generation
        }
}
```

## PDF Export with Images (Future Feature)

### Recommended Library: iText or PDFBox Android
```kotlin
// Pseudo-code for PDF generation
fun exportNoteToPdf(note: Note, images: List<File>): File {
    val pdfFile = File(context.cacheDir, "note_${note.id}.pdf")
    
    PdfWriter(pdfFile).use { writer ->
        val document = Document(writer)
        
        // Add text content
        document.add(Paragraph(note.content))
        
        // Add compressed images
        images.forEach { imageFile ->
            val compressedBitmap = compressForPdf(imageFile)
            val image = Image.getInstance(compressedBitmap.toByteArray())
            image.scaleToFit(500f, 500f) // Limit size in PDF
            document.add(image)
        }
        
        document.close()
    }
    
    return pdfFile
}
```

## Testing Strategy

### Unit Tests
```kotlin
@Test
fun `test image compression reduces file size`() {
    val originalBitmap = createLargeBitmap(4000, 3000)
    val compressedFile = compressImage(originalBitmap)
    
    assertTrue(compressedFile.length() < originalBitmap.byteCount)
}
```

### Memory Leak Tests
```kotlin
@Test
fun `test bitmap recycling prevents memory leaks`() {
    val bitmap = BitmapFactory.decodeResource(resources, R.drawable.test_image)
    
    assertFalse(bitmap.isRecycled)
    processBitmap(bitmap)
    assertTrue(bitmap.isRecycled)
}
```

### Performance Tests
```kotlin
@Test
fun `test image loading does not block UI thread`() {
    val startTime = System.currentTimeMillis()
    
    // Load image on IO dispatcher
    val job = launch(Dispatchers.IO) {
        loadAndProcessImage(largeImageUri)
    }
    
    val duration = System.currentTimeMillis() - startTime
    assertTrue(duration < 100) // Should not block
}
```

## Recommended Dependencies

### Core Libraries
```kotlin
// Image loading and caching
implementation("io.coil-kt:coil-compose:2.4.0")

// OCR (when needed)
implementation("com.google.mlkit:text-recognition:16.0.0")

// PDF generation (when needed)
implementation("com.itextpdf:itext7-core:7.2.5")

// Image compression
implementation("id.zelory:compressor:3.0.1")
```

### ProGuard Rules for Image Libraries
```proguard
# Coil
-keep class coil.** { *; }
-dontwarn coil.**

# ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# iText PDF
-keep class com.itextpdf.** { *; }
-dontwarn com.itextpdf.**
```

## Monitoring and Metrics

### Key Metrics to Track
1. **Average image load time**: < 500ms
2. **Memory usage during image operations**: < 100MB
3. **Crash rate related to OutOfMemoryError**: 0%
4. **Image cache hit rate**: > 80%

### Firebase Performance Monitoring
```kotlin
val trace = Firebase.performance.newTrace("image_load")
trace.start()

// Load image
loadImage(uri)

trace.stop()
```

## Migration Strategy

### Phase 1: Foundation (Not Started)
- [ ] Integrate Coil library
- [ ] Implement file storage structure
- [ ] Add image compression utility
- [ ] Update Note entity with imagePaths field

### Phase 2: Features (Not Started)
- [ ] Add "Attach Image" button in MainScreen
- [ ] Implement image picker
- [ ] Display thumbnails in NotesScreen
- [ ] Full image viewer in NoteDetailScreen

### Phase 3: Advanced (Not Started)
- [ ] OCR integration
- [ ] PDF export with images
- [ ] Image search/filtering
- [ ] Cloud backup for images

## Conclusion

This strategy ensures that when image features are added to Voice Notes AI:
1. **Memory leaks are prevented** through proper lifecycle management
2. **Performance is maintained** with efficient loading and caching
3. **Storage is optimized** with compression and external file storage
4. **User experience is smooth** with progressive loading and placeholders

## References
- [Android Bitmap Management](https://developer.android.com/topic/performance/graphics)
- [Coil Documentation](https://coil-kt.github.io/coil/)
- [ML Kit Text Recognition](https://developers.google.com/ml-kit/vision/text-recognition)
- [Android Storage Best Practices](https://developer.android.com/training/data-storage)
