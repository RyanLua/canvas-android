/*
 * Copyright (C) 2024 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.parentapp.features.courses.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Tab
import com.instructure.canvasapi2.utils.weave.catch
import com.instructure.canvasapi2.utils.weave.tryLaunch
import com.instructure.pandautils.utils.orDefault
import com.instructure.pandautils.utils.studentColor
import com.instructure.parentapp.util.ParentPrefs
import com.instructure.parentapp.util.navigation.Navigation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class CourseDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: CourseDetailsRepository,
    private val parentPrefs: ParentPrefs
) : ViewModel() {

    private val courseId = savedStateHandle.get<Long>(Navigation.COURSE_ID).orDefault()

    private val _uiState = MutableStateFlow(CourseDetailsUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<CourseDetailsViewModelAction>()
    val events = _events.receiveAsFlow()

    init {
        loadData()
    }

    private fun loadData(forceRefresh: Boolean = false) {
        viewModelScope.tryLaunch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    studentColor = parentPrefs.currentStudent.studentColor
                )
            }

            val course = repository.getCourse(courseId, forceRefresh)
            val tabs = repository.getCourseTabs(courseId, forceRefresh)

            val hasHomePageAsFrontPage = course.homePage == Course.HomePage.HOME_WIKI

            val showSyllabusTab = !course.syllabusBody.isNullOrEmpty() &&
                    (course.homePage == Course.HomePage.HOME_SYLLABUS ||
                            (!hasHomePageAsFrontPage && tabs.any { it.tabId == Tab.SYLLABUS_ID }))

            val showSummary = showSyllabusTab && course.settings?.courseSummary.orDefault()

            val tabTypes = buildList {
                add(TabType.GRADES)
                if (hasHomePageAsFrontPage) add(TabType.FRONT_PAGE)
                if (showSyllabusTab) add(TabType.SYLLABUS)
                if (showSummary) add(TabType.SUMMARY)
            }

            _uiState.update {
                it.copy(
                    courseName = course.name,
                    isLoading = false,
                    tabs = tabTypes
                )
            }
        } catch {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isError = true
                )
            }
        }
    }

    fun handleAction(action: CourseDetailsAction) {
        when (action) {
            is CourseDetailsAction.Refresh -> loadData(forceRefresh = true)

            is CourseDetailsAction.SendAMessage -> {
                viewModelScope.launch {
                    _events.send(CourseDetailsViewModelAction.NavigateToComposeMessageScreen)
                }
            }

            is CourseDetailsAction.NavigateToAssignmentDetails -> {
                viewModelScope.launch {
                    _events.send(CourseDetailsViewModelAction.NavigateToAssignmentDetails(action.id))
                }
            }
        }
    }
}
