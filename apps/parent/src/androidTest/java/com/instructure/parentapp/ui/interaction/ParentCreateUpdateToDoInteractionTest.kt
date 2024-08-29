/*
 * Copyright (C) 2024 - present Instructure, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package com.instructure.parentapp.ui.interaction

import androidx.compose.ui.platform.ComposeView
import androidx.test.espresso.matcher.ViewMatchers
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.instructure.canvas.espresso.common.interaction.CreateUpdateToDoInteractionTest
import com.instructure.canvas.espresso.mockCanvas.MockCanvas
import com.instructure.canvas.espresso.mockCanvas.init
import com.instructure.canvasapi2.models.User
import com.instructure.parentapp.BuildConfig
import com.instructure.parentapp.features.login.LoginActivity
import com.instructure.parentapp.ui.pages.DashboardPage
import com.instructure.parentapp.utils.ParentActivityTestRule
import com.instructure.parentapp.utils.tokenLogin
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.Matchers

@HiltAndroidTest
class ParentCreateUpdateToDoInteractionTest : CreateUpdateToDoInteractionTest() {

    override val isTesting = BuildConfig.IS_TESTING

    override val activityRule = ParentActivityTestRule(LoginActivity::class.java)

    private val dashboardPage = DashboardPage()

    override fun goToCreateToDo(data: MockCanvas) {
        val parent = data.parents.first()
        val token = data.tokenFor(parent)!!
        tokenLogin(data.domain, token, parent)

        dashboardPage.clickCalendar()

        composeTestRule.waitForIdle()
        calendarScreenPage.clickOnAddButton()
        calendarScreenPage.clickAddTodo()
    }

    override fun goToEditToDo(data: MockCanvas) {
        val parent = data.parents.first()
        val token = data.tokenFor(parent)!!
        tokenLogin(data.domain, token, parent)

        dashboardPage.clickCalendar()

        val todo = data.todos.first()

        composeTestRule.waitForIdle()
        calendarScreenPage.clickOnItem(todo.plannable.title)
        calendarToDoDetailsPage.clickToolbarMenu()
        calendarToDoDetailsPage.clickEditMenu()
    }

    override fun initData(): MockCanvas {
        val data = MockCanvas.init(
            parentCount = 1,
            studentCount = 1,
            teacherCount = 0,
            courseCount = 1,
            favoriteCourseCount = 1
        )
        data.currentUser = data.parents.first()
        return data
    }

    override fun getLoggedInUser(): User = MockCanvas.data.parents[0]

    override fun enableAndConfigureAccessibilityChecks() {
        extraAccessibilitySupressions = Matchers.allOf(
            AccessibilityCheckResultUtils.matchesCheck(
                SpeakableTextPresentCheck::class.java
            ),
            AccessibilityCheckResultUtils.matchesViews(
                ViewMatchers.withParent(
                    ViewMatchers.withClassName(
                        Matchers.equalTo(ComposeView::class.java.name)
                    )
                )
            )
        )

        super.enableAndConfigureAccessibilityChecks()
    }
}
