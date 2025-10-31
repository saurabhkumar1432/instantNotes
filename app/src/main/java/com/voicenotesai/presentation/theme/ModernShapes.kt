package com.voicenotesai.presentation.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Modern shapes with 12dp card corners and 4dp chip corners
 * as specified in the UI consolidation modernization requirements.
 */
object ModernShapes {
    /**
     * Card corners with 12dp radius
     */
    val cardCorners = RoundedCornerShape(12.dp)
    
    /**
     * Chip corners with 4dp radius
     */
    val chipCorners = RoundedCornerShape(4.dp)
    
    /**
     * Standard border width
     */
    val borderWidth = 1.dp
    
    /**
     * Additional shape variations
     */
    val buttonCorners = RoundedCornerShape(8.dp)
    val dialogCorners = RoundedCornerShape(16.dp)
    val bottomSheetCorners = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
}