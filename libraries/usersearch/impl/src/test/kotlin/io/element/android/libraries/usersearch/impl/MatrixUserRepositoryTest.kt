/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import io.element.android.libraries.matrix.api.core.RoomId
import io.element.android.libraries.matrix.api.core.SessionId
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.toMatrixUser
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.matrix.test.A_USER_ID
import io.element.android.libraries.matrix.test.A_USER_NAME
import io.element.android.libraries.matrix.test.FakeMatrixClient
import io.element.android.libraries.matrix.test.room.FakeBaseRoom
import io.element.android.libraries.matrix.test.room.aRoomInfo
import io.element.android.libraries.matrix.test.room.aRoomMember
import io.element.android.libraries.matrix.test.room.aRoomSummary
import io.element.android.libraries.matrix.test.roomlist.FakeDynamicRoomList
import io.element.android.libraries.matrix.test.roomlist.FakeRoomListService
import io.element.android.libraries.matrix.ui.components.aMatrixUserList
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.test.FakeUserListDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test

private val SESSION_ID = SessionId("@current-user:example.com")

internal class MatrixUserRepositoryTest {
    @Test
    fun `search - emits nothing if the search query is too short`() = runTest {
        val dataSource = FakeUserListDataSource()
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search("x")

        result.test {
            awaitComplete()
        }
    }

    @Test
    fun `search - returns empty list if no results are found`() = runTest {
        val dataSource = FakeUserListDataSource()
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search("some query")

        result.test {
            awaitItem().also {
                assertThat(it.isSearching).isTrue()
                assertThat(it.results).isEmpty()
            }
            awaitItem().also {
                assertThat(it.isSearching).isFalse()
                assertThat(it.results).isEmpty()
            }
            awaitComplete()
        }
    }

    @Test
    fun `search - returns users if results are found`() = runTest {
        val dataSource = FakeUserListDataSource()
        dataSource.givenSearchResult(aMatrixUserList())
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search("some query")

        result.test {
            awaitItem().also {
                assertThat(it.isSearching).isTrue()
                assertThat(it.results).isEmpty()
            }
            awaitItem().also {
                assertThat(it.isSearching).isFalse()
                assertThat(it.results).isEqualTo(aMatrixUserList().toUserSearchResults())
            }
            awaitComplete()
        }
    }

    @Test
    fun `search - immediately returns placeholder if search is mxid`() = runTest {
        val dataSource = FakeUserListDataSource()
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search(A_USER_ID.value)

        result.test {
            awaitItem().also {
                assertThat(it.isSearching).isTrue()
                assertThat(it.results).isEqualTo(listOf(placeholderResult()))
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `search - doesn't return placeholder if search is the local user's mxid`() = runTest {
        val dataSource = FakeUserListDataSource()
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search(SESSION_ID.value)

        result.test {
            awaitItem().also {
                assertThat(it.isSearching).isTrue()
                assertThat(it.results).isEmpty()
            }
            cancelAndConsumeRemainingEvents()
        }
    }

    @Test
    fun `search - filters out results with the local user's mxid`() = runTest {
        val searchResults = aMatrixUserList() + MatrixUser(userId = SESSION_ID, displayName = A_USER_NAME)
        val dataSource = FakeUserListDataSource()
        dataSource.givenSearchResult(searchResults)
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search("some text")

        result.test {
            skipItems(1)
            assertThat(awaitItem().results).isEqualTo(aMatrixUserList().toUserSearchResults())
            awaitComplete()
        }
    }

    @Test
    fun `search - does not change results if they contain searched mxid`() = runTest {
        val searchResults = aMatrixUserListWithoutUserId(A_USER_ID) + MatrixUser(userId = A_USER_ID, displayName = A_USER_NAME)
        val dataSource = FakeUserListDataSource()
        dataSource.givenSearchResult(searchResults)
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search(A_USER_ID.value)

        result.test {
            skipItems(1)
            assertThat(awaitItem().results).isEqualTo(searchResults.toUserSearchResults())
            awaitComplete()
        }
    }

    @Test
    fun `search - gets profile results if searched mxid not in results`() = runTest {
        val userProfile = MatrixUser(userId = A_USER_ID, displayName = A_USER_NAME)
        val searchResults = aMatrixUserListWithoutUserId(A_USER_ID)

        val dataSource = FakeUserListDataSource()
        dataSource.givenSearchResult(searchResults)
        dataSource.givenUserProfile(userProfile)
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search(A_USER_ID.value)

        result.test {
            skipItems(1)
            assertThat(awaitItem().results).isEqualTo((listOf(userProfile) + searchResults).toUserSearchResults())
            awaitComplete()
        }
    }

    @Test
    fun `search - doesn't add profile results if searched mxid is local user and not in results`() = runTest {
        val userProfile = MatrixUser(userId = A_USER_ID, displayName = A_USER_NAME)
        val searchResults = aMatrixUserListWithoutUserId(SESSION_ID)

        val dataSource = FakeUserListDataSource()
        dataSource.givenSearchResult(searchResults)
        dataSource.givenUserProfile(userProfile)
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search(SESSION_ID.value)

        result.test {
            skipItems(1)
            assertThat(awaitItem().results).isEqualTo(searchResults.toUserSearchResults())
            awaitComplete()
        }
    }

    @Test
    fun `search - returns unresolved user if profile can't be loaded`() = runTest {
        val searchResults = aMatrixUserListWithoutUserId(A_USER_ID)

        val dataSource = FakeUserListDataSource()
        dataSource.givenSearchResult(searchResults)
        dataSource.givenUserProfile(null)
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID), dataSource)

        val result = repository.search(A_USER_ID.value)

        result.test {
            skipItems(1)
            assertThat(awaitItem().results).isEqualTo(listOf(placeholderResult(isUnresolved = true)) + searchResults.toUserSearchResults())
            awaitComplete()
        }
    }

    @Test
    fun `search - falls back to known DM contacts the directory doesn't return`() = runTest {
        // Simulates Synapse's own user_directory excluding an appservice-registered bridge ghost:
        // the directory data source returns nothing, but the app already knows this contact
        // locally because we share a DM room with them.
        val bridgeGhost = MatrixUser(userId = UserId("@signal_abc123:example.com"), displayName = "Chantal")
        val roomListService = FakeRoomListService(
            allRooms = FakeDynamicRoomList(
                summaries = MutableStateFlow(
                    listOf(aRoomSummary(info = aRoomInfo(name = "Chantal", isDm = true, heroes = listOf(bridgeGhost))))
                ),
            ),
        )
        val dataSource = FakeUserListDataSource()
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID, roomListService = roomListService), dataSource)

        val result = repository.search("chantal")

        result.test {
            awaitItem().also {
                assertThat(it.isSearching).isTrue()
                assertThat(it.results).isEmpty()
            }
            awaitItem().also {
                assertThat(it.isSearching).isFalse()
                assertThat(it.results).isEqualTo(listOf(UserSearchResult(bridgeGhost)))
            }
            awaitComplete()
        }
    }

    @Test
    fun `search - picks the real contact over a self-puppet ghost sharing the same DM`() = runTest {
        // Some bridges (observed with Signal specifically) list more than one non-self hero in a
        // DM's room summary: a "self-puppet" ghost representing our own account on that network,
        // alongside the real contact's ghost. Naively taking the first hero can pick the wrong one
        // -- the room's own name is the reliable signal, since the bridge sets it from the real
        // contact's profile, not the self-puppet's.
        val selfPuppetGhost = MatrixUser(userId = UserId("@signal_selfpuppet:example.com"), displayName = "Jimmy's Signal")
        val realContactGhost = MatrixUser(userId = UserId("@signal_realcontact:example.com"), displayName = "Priya")
        val roomListService = FakeRoomListService(
            allRooms = FakeDynamicRoomList(
                summaries = MutableStateFlow(
                    listOf(
                        aRoomSummary(
                            info = aRoomInfo(
                                name = "Priya",
                                isDm = true,
                                // Mirrors production for a DM with more than one non-self hero: `heroes` (see
                                // RoomInfo.elementHeroes()) is emptied out entirely, so only `allHeroes` has
                                // anything to go on. Self-puppet listed first on purpose, so a naive "first
                                // hero" pick from `allHeroes` would also fail this test.
                                heroes = emptyList(),
                                allHeroes = listOf(selfPuppetGhost, realContactGhost),
                            )
                        )
                    )
                ),
            ),
        )
        val dataSource = FakeUserListDataSource()
        val repository = MatrixUserRepository(FakeMatrixClient(SESSION_ID, roomListService = roomListService), dataSource)

        val result = repository.search("priya")

        result.test {
            skipItems(1)
            assertThat(awaitItem().results).isEqualTo(listOf(UserSearchResult(realContactGhost)))
            awaitComplete()
        }
    }

    @Test
    fun `search - falls back to opening the room when the cached summary has no heroes at all`() = runTest {
        // In practice a DM's cached room list summary doesn't always carry hero info, even for a
        // single-hero DM (observed with some Signal contacts specifically) -- so `allHeroes` being
        // empty doesn't mean there's no contact to find, and the repository must fall back to
        // actively fetching the room's member list via getMembers(). The bridge bot is included in
        // the fetched members (isServiceMember = true) to prove it gets filtered out correctly.
        val roomId = RoomId("!priyaroom:example.com")
        val realContactMember = aRoomMember(userId = UserId("@signal_realcontact:example.com"), displayName = "Priya")
        val bridgeBotMember = aRoomMember(userId = UserId("@signalbot:example.com"), displayName = "Signal Bridge", isServiceMember = true)
        val roomListService = FakeRoomListService(
            allRooms = FakeDynamicRoomList(
                summaries = MutableStateFlow(
                    listOf(
                        aRoomSummary(
                            info = aRoomInfo(id = roomId, name = "Priya", isDm = true, heroes = emptyList(), allHeroes = emptyList()),
                        )
                    )
                ),
            ),
        )
        val matrixClient = FakeMatrixClient(SESSION_ID, roomListService = roomListService).apply {
            givenGetRoomResult(
                roomId,
                FakeBaseRoom(roomId = roomId, getMembersResult = { Result.success(listOf(bridgeBotMember, realContactMember)) }),
            )
        }
        val dataSource = FakeUserListDataSource()
        val repository = MatrixUserRepository(matrixClient, dataSource)

        val result = repository.search("priya")

        result.test {
            skipItems(1)
            assertThat(awaitItem().results).isEqualTo(listOf(UserSearchResult(realContactMember.toMatrixUser().copy(displayName = "Priya"))))
            awaitComplete()
        }
    }

    private fun aMatrixUserListWithoutUserId(userId: UserId) = aMatrixUserList().filterNot { it.userId == userId }

    private fun List<MatrixUser>.toUserSearchResults() = map { UserSearchResult(it) }

    private fun placeholderResult(id: UserId = A_USER_ID, isUnresolved: Boolean = false) = UserSearchResult(MatrixUser(id), isUnresolved = isUnresolved)
}
