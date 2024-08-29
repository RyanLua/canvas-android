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
package com.instructure.parentapp.features.calendartodo

import com.instructure.canvasapi2.apis.CourseAPI
import com.instructure.canvasapi2.apis.PlannerAPI
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Enrollment
import com.instructure.canvasapi2.models.User
import com.instructure.canvasapi2.utils.DataResult
import com.instructure.canvasapi2.utils.LinkHeaders
import com.instructure.parentapp.util.ParentPrefs
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ParentCreateUpdateToDoRepositoryTest {

    private val coursesApi: CourseAPI.CoursesInterface = mockk(relaxed = true)
    private val parentPrefs: ParentPrefs = mockk(relaxed = true)
    private val plannerApi: PlannerAPI.PlannerInterface = mockk(relaxed = true)

    private val repository = ParentCreateUpdateToDoRepository(coursesApi, parentPrefs, plannerApi)

    @Test
    fun `Returns empty list when request fails`() = runTest {
        coEvery { coursesApi.firstPageObserveeCourses(any()) } returns DataResult.Fail()

        val courses = repository.getCourses()

        assertEquals(emptyList<Course>(), courses)
    }

    @Test
    fun `Returns courses filtered by enrollment type and observee id`() = runTest {
        val courses = listOf(
            Course(id = 1),
            Course(id = 2, enrollments = mutableListOf(Enrollment(type = Enrollment.EnrollmentType.Teacher, userId = 55))),
            Course(id = 3, enrollments = mutableListOf(Enrollment(type = Enrollment.EnrollmentType.Observer, userId = 77))),
            Course(id = 4, enrollments = mutableListOf(Enrollment(type = Enrollment.EnrollmentType.Observer, userId = 55))),
        )

        coEvery { parentPrefs.currentStudent } returns User(id = 55)
        coEvery { coursesApi.firstPageObserveeCourses(any()) } returns DataResult.Success(courses)

        val result = repository.getCourses()

        assertEquals(courses.takeLast(1), result)
    }

    @Test
    fun `Returns depaginated result when has next page`() = runTest {
        val courses1 = listOf(Course(id = 4, enrollments = mutableListOf(Enrollment(type = Enrollment.EnrollmentType.Observer, userId = 55))))
        val courses2 = listOf(Course(id = 5, enrollments = mutableListOf(Enrollment(type = Enrollment.EnrollmentType.Observer, userId = 55))))

        coEvery { parentPrefs.currentStudent } returns User(id = 55)
        coEvery { coursesApi.firstPageObserveeCourses(any()) } returns DataResult.Success(courses1, linkHeaders = LinkHeaders(nextUrl = "next"))
        coEvery { coursesApi.next("next", any()) } returns DataResult.Success(courses2)

        val result = repository.getCourses()

        assertEquals(courses1 + courses2, result)
    }
}