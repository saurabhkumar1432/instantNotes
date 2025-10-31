package com.voicenotesai.domain

import com.voicenotesai.data.ai.AIConfigurationValidationTest
import com.voicenotesai.data.notification.ReminderNotificationTest
import com.voicenotesai.domain.usecase.TaskManagementTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

/**
 * Test suite for task management functionality.
 * 
 * This suite runs all task management related tests to verify:
 * - Task creation and management
 * - AI-powered task extraction
 * - AI configuration validation
 * - Reminder scheduling and notifications
 */
@RunWith(Suite::class)
@Suite.SuiteClasses(
    TaskManagementTest::class,
    AIConfigurationValidationTest::class,
    ReminderNotificationTest::class
)
class TaskManagementTestSuite