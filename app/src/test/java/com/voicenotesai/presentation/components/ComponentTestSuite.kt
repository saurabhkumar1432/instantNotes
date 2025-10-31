package com.voicenotesai.presentation.components

import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for all component tests.
 * 
 * This suite runs all component-related unit tests to verify:
 * - UI component functionality
 * - User interaction handling
 * - Accessibility compliance
 * - Visual state management
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    GradientHeaderTest::class,
    StatsCardTest::class,
    NoteCardTest::class,
    TaskCardTest::class
)
class ComponentTestSuite