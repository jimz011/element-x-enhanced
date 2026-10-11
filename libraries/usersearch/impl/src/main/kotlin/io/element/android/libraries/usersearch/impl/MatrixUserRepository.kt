/*
 * Copyright (c) 2025 Element Creations Ltd.
 * Copyright 2023-2025 New Vector Ltd.
 *
 * SPDX-License-Identifier: AGPL-3.0-only OR LicenseRef-Element-Commercial.
 * Please see LICENSE files in the repository root for full details.
 */

package io.element.android.libraries.usersearch.impl

import dev.zacsweers.metro.ContributesBinding
import io.element.android.libraries.di.SessionScope
import io.element.android.libraries.matrix.api.MatrixClient
import io.element.android.libraries.matrix.api.core.MatrixPatterns
import io.element.android.libraries.matrix.api.core.UserId
import io.element.android.libraries.matrix.api.room.RoomInfo
import io.element.android.libraries.matrix.api.room.getBestName
import io.element.android.libraries.matrix.api.room.toMatrixUser
import io.element.android.libraries.matrix.api.roomlist.RoomSummary
import io.element.android.libraries.matrix.api.user.MatrixUser
import io.element.android.libraries.usersearch.api.UserListDataSource
import io.element.android.libraries.usersearch.api.UserRepository
import io.element.android.libraries.usersearch.api.UserSearchResult
import io.element.android.libraries.usersearch.api.UserSearchResultState
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@ContributesBinding(SessionScope::class)
class MatrixUserRepository(
    private val client: MatrixClient,
    private val dataSource: UserListDataSource
) : UserRepository {
    override fun search(query: String): Flow<UserSearchResultState> = flow {
        val shouldQueryProfile = MatrixPatterns.isUserId(query) && !client.isMe(UserId(query))
        val shouldFetchSearchResults = query.length >= MINIMUM_SEARCH_LENGTH
        // If the search term is a MXID that's not ours, we'll show a 'fake' result for that user, then update it when we get search results.
        val fakeSearchResult = if (shouldQueryProfile) {
            UserSearchResult(MatrixUser(UserId(query)))
        } else {
            null
        }
        if (shouldQueryProfile || shouldFetchSearchResults) {
            emit(UserSearchResultState(isSearching = shouldFetchSearchResults, results = listOfNotNull(fakeSearchResult)))
        }
        if (shouldFetchSearchResults) {
            val results = fetchSearchResults(query, shouldQueryProfile)
            emit(results)
        }
    }

    private suspend fun fetchSearchResults(query: String, shouldQueryProfile: Boolean): UserSearchResultState {
        // Debounce
        delay(DEBOUNCE_TIME_MILLIS)
        val directoryResults = dataSource
            .search(query, MAXIMUM_SEARCH_RESULTS)
            .filter { !client.isMe(it.userId) }
            .map { UserSearchResult(it) }

        val results = (directoryResults + knownUsersMatching(query))
            .distinctBy { it.matrixUser.userId }
            .toMutableList()

        // If the query is another user's MXID and the result doesn't contain that user ID, query the profile information explicitly
        if (shouldQueryProfile && results.none { it.matrixUser.userId.value == query }) {
            results.add(
                0,
                dataSource.getProfile(UserId(query))
                    ?.let { UserSearchResult(it) }
                    ?: UserSearchResult(MatrixUser(UserId(query)), isUnresolved = true)
            )
        }

        return UserSearchResultState(results = results, isSearching = false)
    }

    /**
     * Matches against the other party of every DM we already know about locally, so search still
     * works for contacts the homeserver's own directory can never return.
     *
     * Synapse's `user_directory` deliberately excludes every appservice-registered user (see
     * `should_include_local_user_in_dir` in its source) -- that's every bridge ghost puppet
     * (WhatsApp, Signal, Discord, etc contacts) -- regardless of the `search_all_users` setting.
     * This is the same workaround classic Element Android's "Known Users" section relies on for
     * the identical server-side limitation.
     *
     * [MatrixClient.roomListService]'s `allRooms` is a singleton the SDK already keeps loaded for
     * other callers, so reading its latest snapshot here opens no new subscription for the list
     * itself.
     */
    private suspend fun knownUsersMatching(query: String): List<UserSearchResult> {
        val currentUserId = client.sessionId
        val dmSummaries = client.roomListService.allRooms.summaries.replayCache
            .lastOrNull()
            .orEmpty()
            .filter { it.isDm }

        // Each summary is resolved concurrently: cached-summary heroes are used when present (no
        // extra fetch), otherwise this falls back to opening the room and asking the SDK directly.
        // Doing that fallback for potentially every DM room is the deliberate cost of correctness
        // here -- see resolveDirectRoomMember()'s doc comment for why the cheap path alone isn't
        // reliable enough to skip it.
        val resolved = coroutineScope {
            dmSummaries
                .map { summary -> async { summary.toKnownContact(currentUserId) ?: summary.resolveDirectRoomMember(currentUserId) } }
                .awaitAll()
        }

        return resolved
            .filterNotNull()
            .distinctBy { it.userId }
            .filter { it.userId != currentUserId && !client.isMe(it.userId) }
            .filter { user ->
                user.userId.value.contains(query, ignoreCase = true) ||
                    user.displayName?.contains(query, ignoreCase = true) == true
            }
            .take(MAXIMUM_KNOWN_USERS_RESULTS)
            .map { UserSearchResult(it) }
    }

    /**
     * Resolves the other party of a DM room from its already-cached summary, without an extra
     * per-room fetch -- the cheap, common-case path.
     *
     * Uses [RoomInfo.allHeroes], not [RoomInfo.heroes] -- the latter is deliberately emptied out by
     * this app's own mapping layer whenever a DM has more than one non-self hero (see
     * `elementHeroes()`'s doc comment), which some bridges trigger by also joining a "self-puppet"
     * ghost representing our own account on that network alongside the real contact. But even
     * `allHeroes` isn't reliably populated for every DM in the cached room list summary in
     * practice, so a `null` result here doesn't mean there's no contact -- see
     * [resolveDirectRoomMember] for the fallback this feeds into.
     *
     * With more than one candidate, picking the first isn't reliable either -- the room's own name
     * is the trustworthy signal: it's set by the bridge from the real contact's profile and
     * consistently matches that contact's own display name exactly (verified directly against this
     * deployment's Signal and WhatsApp portals, where a self-puppet's or the bot's own name always
     * differs from the room name, but the real contact's own name matches it). So: prefer whichever
     * hero's own display name equals the room name, falling back to the first non-self hero if none
     * match, and always use the room's own name for the returned display name so matching and
     * display stay correct even in that fallback case.
     */
    private fun RoomSummary.toKnownContact(currentUserId: UserId): MatrixUser? {
        val candidates = info.allHeroes.filter { it.userId != currentUserId }
        val roomName = info.name?.takeIf { it.isNotBlank() }
        val hero = candidates.firstOrNull { it.displayName != null && it.displayName == roomName }
            ?: candidates.firstOrNull()
            ?: return null
        return hero.copy(displayName = roomName ?: hero.displayName)
    }

    /**
     * Authoritative fallback for when the cached summary alone couldn't resolve a DM's other
     * party: opens the room and actively fetches its member list via [io.element.android.libraries.matrix.api.room.BaseRoom.getMembers],
     * rather than [io.element.android.libraries.matrix.api.room.BaseRoom.getDirectRoomMember], which reads from a passively-cached
     * member list that in practice isn't always populated yet for a less-active DM (this was
     * observed specifically with a Signal contact that [toKnownContact] also couldn't resolve --
     * [getDirectRoomMember] alone still came back empty for it, so this fetches instead of just
     * reading the cache).
     *
     * [RoomMember.isServiceMember] gives an authoritative way to exclude the bridge bot here,
     * unlike [toKnownContact]'s room-name heuristic (which [MatrixUser] alone can't support, since
     * it doesn't carry that flag) -- but a self-puppet ghost isn't flagged as a service member
     * (it's a real, if puppeted, account), so the same room-name-match preference from
     * [toKnownContact] is still needed to pick the real contact over it when both remain.
     */
    private suspend fun RoomSummary.resolveDirectRoomMember(currentUserId: UserId): MatrixUser? {
        val members = client.getRoom(roomId)?.use { it.getMembers(limit = 5) }?.getOrNull().orEmpty()
        val roomName = info.name?.takeIf { it.isNotBlank() }
        val candidates = members.filter { it.userId != currentUserId && !it.isServiceMember }
        val member = candidates.firstOrNull { it.getBestName() == roomName }
            ?: candidates.firstOrNull()
            ?: return null
        val user = member.toMatrixUser()
        return if (roomName != null) user.copy(displayName = roomName) else user
    }

    companion object {
        private const val DEBOUNCE_TIME_MILLIS = 250L
        private const val MINIMUM_SEARCH_LENGTH = 3
        private const val MAXIMUM_SEARCH_RESULTS = 10L
        private const val MAXIMUM_KNOWN_USERS_RESULTS = 20
    }
}
