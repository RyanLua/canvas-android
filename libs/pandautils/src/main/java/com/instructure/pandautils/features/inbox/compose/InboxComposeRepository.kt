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
package com.instructure.pandautils.features.inbox.compose

import com.instructure.canvasapi2.models.Attachment
import com.instructure.canvasapi2.models.CanvasContext
import com.instructure.canvasapi2.models.Conversation
import com.instructure.canvasapi2.models.Course
import com.instructure.canvasapi2.models.Group
import com.instructure.canvasapi2.models.Message
import com.instructure.canvasapi2.models.Recipient
import com.instructure.canvasapi2.utils.DataResult

interface InboxComposeRepository {
    suspend fun getCourses(forceRefresh: Boolean = false): DataResult<List<Course>>
    suspend fun getGroups(forceRefresh: Boolean = false): DataResult<List<Group>>
    suspend fun getRecipients(searchQuery: String, context: CanvasContext, forceRefresh: Boolean = false): DataResult<List<Recipient>>
    suspend fun createConversation(recipients: List<Recipient>, subject: String, message: String, context: CanvasContext, attachments: List<Attachment>, isIndividual: Boolean): DataResult<List<Conversation>>
    suspend fun addMessage(conversationId: Long, recipients: List<Recipient>, message: String, includedMessages: List<Message>, attachments: List<Attachment>, context: CanvasContext, ): DataResult<Conversation>
    suspend fun canSendToAll(context: CanvasContext): DataResult<Boolean>
}