package com.voicenotesai.presentation.performance

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Performance utilities for optimizing Compose recomposition and memory usage.
 */

/**
 * Stable wrapper for data classes to prevent unnecessary recompositions.
 * Use this for complex data structures that are passed to Composables.
 */
@Stable
data class StableWrapper<T>(val value: T)

/**
 * Creates a stable wrapper for any value to prevent recompositions
 * when the value itself is stable but Compose can't determine that.
 */
@Composable
fun <T> rememberStable(value: T): StableWrapper<T> {
    return remember(value) { StableWrapper(value) }
}

/**
 * Optimized state holder that prevents unnecessary recompositions
 * by only updating when values actually change.
 */
@Stable
class OptimizedState<T>(initialValue: T) {
    private var _value by mutableStateOf(initialValue)
    
    var value: T
        get() = _value
        set(newValue) {
            if (_value != newValue) {
                _value = newValue
            }
        }
    
    fun update(newValue: T) {
        value = newValue
    }
}

/**
 * Creates an optimized state that only triggers recomposition when the value actually changes.
 */
@Composable
fun <T> rememberOptimizedState(initialValue: T): OptimizedState<T> {
    return remember { OptimizedState(initialValue) }
}

/**
 * Memory-efficient list state that handles large collections efficiently.
 */
@Stable
class OptimizedListState<T>(
    private val keySelector: (T) -> Any = { it.hashCode() }
) {
    private var _items by mutableStateOf<List<T>>(emptyList())
    private var _itemKeys by mutableStateOf<List<Any>>(emptyList())
    
    val items: List<T> get() = _items
    val itemKeys: List<Any> get() = _itemKeys
    
    fun updateItems(newItems: List<T>) {
        val newKeys = newItems.map(keySelector)
        
        // Only update if the list actually changed
        if (_itemKeys != newKeys) {
            _items = newItems
            _itemKeys = newKeys
        }
    }
    
    fun addItem(item: T) {
        val newItems = _items + item
        updateItems(newItems)
    }
    
    fun removeItem(predicate: (T) -> Boolean) {
        val newItems = _items.filterNot(predicate)
        updateItems(newItems)
    }
    
    fun clear() {
        _items = emptyList()
        _itemKeys = emptyList()
    }
}

/**
 * Creates an optimized list state for handling large collections efficiently.
 */
@Composable
fun <T> rememberOptimizedListState(
    keySelector: (T) -> Any = { it.hashCode() }
): OptimizedListState<T> {
    return remember { OptimizedListState(keySelector) }
}

/**
 * Performance-optimized ViewModel base class with built-in state management.
 */
abstract class OptimizedViewModel : ViewModel() {
    
    /**
     * Creates a StateFlow that only emits when the value actually changes.
     */
    protected fun <T> MutableStateFlow<T>.asOptimizedStateFlow(): StateFlow<T> {
        return this.stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
            initialValue = this.value
        )
    }
    
    /**
     * Combines multiple flows efficiently for complex UI state.
     */
    protected fun <T1, T2, R> combineOptimized(
        flow1: StateFlow<T1>,
        flow2: StateFlow<T2>,
        transform: (T1, T2) -> R
    ): StateFlow<R> {
        return combine(flow1, flow2, transform)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
                initialValue = transform(flow1.value, flow2.value)
            )
    }
    
    /**
     * Maps a StateFlow with optimization.
     */
    protected fun <T, R> StateFlow<T>.mapOptimized(
        transform: (T) -> R
    ): StateFlow<R> {
        return this.map(transform)
            .distinctUntilChanged()
            .stateIn(
                scope = viewModelScope,
                started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000),
                initialValue = transform(this.value)
            )
    }
    
    /**
     * Safely launches coroutines with error handling.
     */
    protected fun safeLaunch(
        onError: (Throwable) -> Unit = {},
        block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }
}

/**
 * Memory management utilities for handling large data sets efficiently.
 */
object MemoryOptimizer {
    
    /**
     * Creates a chunked flow for processing large lists in batches.
     */
    fun <T> Flow<List<T>>.chunked(size: Int): Flow<List<List<T>>> {
        return map { list -> list.chunked(size) }
    }
    
    /**
     * Limits the number of items in a flow to prevent memory issues.
     */
    fun <T> Flow<List<T>>.limitItems(maxItems: Int): Flow<List<T>> {
        return map { list -> 
            if (list.size > maxItems) {
                list.takeLast(maxItems)
            } else {
                list
            }
        }
    }
    
    /**
     * Debounces a flow to reduce the number of emissions.
     */
    fun <T> Flow<T>.debounceOptimized(timeoutMillis: Long = 300): Flow<T> {
        return debounce(timeoutMillis).distinctUntilChanged()
    }
}

/**
 * Recomposition debugging utilities for development.
 */
object RecompositionDebugger {
    
    /**
     * Logs recomposition count for debugging purposes.
     * Only active in debug mode or preview.
     */
    @Composable
    fun LogRecompositions(tag: String) {
        if (LocalInspectionMode.current) {
            // Only log in preview/debug mode
            var recompositionCount by remember { mutableStateOf(0) }
            recompositionCount++
            
            // In a real app, you'd use proper logging
            println("Recomposition #$recompositionCount for $tag")
        }
    }
    
    /**
     * Tracks the number of recompositions for a given composable.
     */
    @Composable
    fun rememberRecompositionCount(): Int {
        var count by remember { mutableStateOf(0) }
        count++
        return count
    }
}

/**
 * Lazy computation helper for expensive operations.
 */
@Stable
class LazyComputation<T>(
    private val computation: () -> T
) {
    private var cachedResult: T? = null
    private var isComputed = false
    
    fun getValue(): T {
        if (!isComputed) {
            cachedResult = computation()
            isComputed = true
        }
        @Suppress("UNCHECKED_CAST")
        return cachedResult as T
    }
    
    fun invalidate() {
        cachedResult = null
        isComputed = false
    }
}

/**
 * Creates a lazy computation that only executes when first accessed.
 */
@Composable
fun <T> rememberLazyComputation(
    key: Any? = Unit,
    computation: () -> T
): LazyComputation<T> {
    return remember(key) { LazyComputation(computation) }
}

/**
 * Efficient paging state for large lists.
 */
@Stable
class PagingState<T>(
    private val pageSize: Int = 20
) {
    private var _allItems by mutableStateOf<List<T>>(emptyList())
    private var _visibleItems by mutableStateOf<List<T>>(emptyList())
    private var _currentPage by mutableStateOf(0)
    
    val visibleItems: List<T> get() = _visibleItems
    val hasMore: Boolean get() = _currentPage * pageSize < _allItems.size
    val currentPage: Int get() = _currentPage
    
    fun setAllItems(items: List<T>) {
        _allItems = items
        _currentPage = 0
        updateVisibleItems()
    }
    
    fun loadMore() {
        if (hasMore) {
            _currentPage++
            updateVisibleItems()
        }
    }
    
    fun reset() {
        _currentPage = 0
        updateVisibleItems()
    }
    
    private fun updateVisibleItems() {
        val endIndex = minOf((_currentPage + 1) * pageSize, _allItems.size)
        _visibleItems = _allItems.take(endIndex)
    }
}

/**
 * Creates a paging state for efficiently displaying large lists.
 */
@Composable
fun <T> rememberPagingState(pageSize: Int = 20): PagingState<T> {
    return remember { PagingState<T>(pageSize) }
}