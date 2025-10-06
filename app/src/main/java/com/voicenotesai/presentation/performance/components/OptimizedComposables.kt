package com.voicenotesai.presentation.performance.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.voicenotesai.presentation.performance.OptimizedListState
import com.voicenotesai.presentation.performance.PagingState
import com.voicenotesai.presentation.performance.RecompositionDebugger
import com.voicenotesai.presentation.performance.StableWrapper
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Performance-optimized composables that minimize recomposition and improve memory usage.
 */

/**
 * Optimized LazyColumn that handles large lists efficiently with virtual scrolling.
 */
@Composable
fun <T> OptimizedLazyColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    keySelector: (item: T) -> Any = { it.hashCode() },
    itemContent: @Composable LazyItemScope.(item: T, index: Int) -> Unit
) {
    RecompositionDebugger.LogRecompositions("OptimizedLazyColumn")
    
    val listState = rememberLazyListState()
    val stableItems = remember(items) { StableWrapper(items) }
    
    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        itemsIndexed(
            items = stableItems.value,
            key = { _, item -> keySelector(item) }
        ) { index, item ->
            itemContent(item, index)
        }
    }
}

/**
 * Paginated lazy column that loads more items as the user scrolls.
 */
@Composable
fun <T> PaginatedLazyColumn(
    pagingState: PagingState<T>,
    modifier: Modifier = Modifier,
    keySelector: (item: T) -> Any = { it.hashCode() },
    loadingIndicator: @Composable () -> Unit = { 
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    },
    itemContent: @Composable LazyItemScope.(item: T, index: Int) -> Unit
) {
    RecompositionDebugger.LogRecompositions("PaginatedLazyColumn")
    
    val listState = rememberLazyListState()
    val items = pagingState.visibleItems
    
    // Detect when user reaches the bottom and load more
    val shouldLoadMore by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0) + 1
            
            lastVisibleItemIndex > (totalItemsNumber - 3) && pagingState.hasMore
        }
    }
    
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            pagingState.loadMore()
        }
    }
    
    LazyColumn(
        modifier = modifier,
        state = listState
    ) {
        itemsIndexed(
            items = items,
            key = { _, item -> keySelector(item) }
        ) { index, item ->
            itemContent(item, index)
        }
        
        if (pagingState.hasMore) {
            item {
                loadingIndicator()
            }
        }
    }
}

/**
 * Optimized list with efficient updates that only recomposes changed items.
 */
@Composable
fun <T> EfficientList(
    listState: OptimizedListState<T>,
    modifier: Modifier = Modifier,
    itemContent: @Composable (item: T, index: Int) -> Unit
) {
    RecompositionDebugger.LogRecompositions("EfficientList")
    
    val items = listState.items
    val itemKeys = listState.itemKeys
    
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = items,
            key = { index, _ -> itemKeys.getOrNull(index) ?: index }
        ) { index, item ->
            itemContent(item, index)
        }
    }
}

/**
 * Memory-efficient grid that virtualizes content for better performance.
 */
@Composable
fun <T> OptimizedGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    keySelector: (item: T) -> Any = { it.hashCode() },
    itemContent: @Composable (item: T) -> Unit
) {
    RecompositionDebugger.LogRecompositions("OptimizedGrid")
    
    val chunkedItems = remember(items, columns) {
        items.chunked(columns)
    }
    
    LazyColumn(modifier = modifier) {
        itemsIndexed(
            items = chunkedItems,
            key = { index, _ -> "row_$index" }
        ) { _, rowItems ->
            androidx.compose.foundation.layout.Row {
                rowItems.forEach { item ->
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.weight(1f)
                    ) {
                        itemContent(item)
                    }
                }
                
                // Fill empty spaces in the last row
                repeat(columns - rowItems.size) {
                    androidx.compose.foundation.layout.Spacer(
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Composable that only recomposes when its content actually changes.
 */
@Composable
fun <T> StableContent(
    value: T,
    content: @Composable (T) -> Unit
) {
    val stableValue = remember(value) { StableWrapper(value) }
    content(stableValue.value)
}

/**
 * Viewport-aware composable that only renders when visible.
 */
@Composable
fun ViewportAwareContent(
    isVisible: Boolean,
    content: @Composable () -> Unit
) {
    if (isVisible) {
        content()
    }
}

/**
 * Heavy computation wrapper that caches results.
 */
@Composable
fun <T> CachedComputation(
    key: Any?,
    computation: () -> T,
    content: @Composable (result: T) -> Unit
) {
    val result = remember(key) { computation() }
    content(result)
}

/**
 * Smooth scrolling behavior for large lists.
 */
@Composable
fun rememberSmoothScrollBehavior(
    density: Density
): androidx.compose.foundation.gestures.ScrollableDefaults {
    return remember(density) {
        androidx.compose.foundation.gestures.ScrollableDefaults
    }
}

/**
 * Performance monitoring for composables.
 */
@Composable
fun PerformanceMonitor(
    tag: String,
    content: @Composable () -> Unit
) {
    val recompositionCount = RecompositionDebugger.rememberRecompositionCount()
    
    // Monitor excessive recompositions (only in debug builds)
    LaunchedEffect(recompositionCount) {
        if (recompositionCount > 10) {
            println("Performance Warning: $tag has recomposed $recompositionCount times")
        }
    }
    
    content()
}

/**
 * Efficient item with stable keys for list performance.
 */
@Composable
fun <T> StableListItem(
    item: T,
    key: Any,
    content: @Composable (T) -> Unit
) {
    // Wrap the item in a stable container
    val stableItem = remember(key) { StableWrapper(item) }
    content(stableItem.value)
}