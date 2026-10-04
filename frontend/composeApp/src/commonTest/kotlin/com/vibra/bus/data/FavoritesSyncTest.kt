package com.vibra.bus.data

import com.vibra.bus.data.api.FavoriteStopDto
import com.vibra.bus.data.api.FavoriteStopsResponse
import com.vibra.bus.data.repository.FavoritesRemote
import com.vibra.bus.data.repository.FavoritesRepository
import com.vibra.bus.data.repository.FavoritesStorage
import com.vibra.bus.util.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import kotlin.test.Test
import kotlin.test.assertEquals

/** Ejecuta una funcion suspend que no se suspende de verdad (fakes sincronos) sin coroutines-test. */
private fun <T> runNow(block: suspend () -> T): T {
    var result: Result<T>? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { result = it })
    return result!!.getOrThrow()
}

private class FakeStorage : FavoritesStorage {
    override var ids = ""
    override var pendingAdds = ""
    override var pendingRemoves = ""
}

private class FakeRemote : FavoritesRemote {
    var server = mutableSetOf<Int>()
    var addResult: (Int) -> ApiResult<Unit> = { server.add(it); ApiResult.Success(Unit) }
    var removeResult: (Int) -> ApiResult<Unit> = { server.remove(it); ApiResult.Success(Unit) }
    var listResult: () -> ApiResult<FavoriteStopsResponse> = {
        ApiResult.Success(FavoriteStopsResponse(true, server.map { FavoriteStopDto(it) }))
    }
    override suspend fun list() = listResult()
    override suspend fun add(stopId: Int) = addResult(stopId)
    override suspend fun remove(stopId: Int) = removeResult(stopId)
}

class FavoritesSyncTest {
    private fun repo(storage: FavoritesStorage, remote: FavoritesRemote) =
        FavoritesRepository(storage, remote, CoroutineScope(Dispatchers.Unconfined))

    @Test fun successfulSyncClearsPendingAndAdoptsServerList() {
        val storage = FakeStorage(); val remote = FakeRemote().apply { server.add(5) }
        val r = repo(storage, remote)
        r.toggle(1)
        assertEquals("", storage.pendingAdds)
        assertEquals(setOf(5, 1), r.ids.value)
    }

    @Test fun unauthorizedKeepsPendingAndLocalFavorite() {
        val storage = FakeStorage(); val remote = FakeRemote()
        remote.addResult = { ApiResult.HttpError(401, "Unauthorized") }
        val r = repo(storage, remote)
        r.toggle(7)
        assertEquals("7", storage.pendingAdds)
        assertEquals(setOf(7), r.ids.value)
        // Tras volver a iniciar sesion el reintento lo sube.
        remote.addResult = { remote.server.add(it); ApiResult.Success(Unit) }
        runNow { r.sync() }
        assertEquals("", storage.pendingAdds)
        assertEquals(setOf(7), remote.server)
    }

    @Test fun serverErrorAndMissingEndpointKeepPending() {
        for (code in listOf(404, 405, 500, 503)) {
            val storage = FakeStorage(); val remote = FakeRemote()
            remote.addResult = { ApiResult.HttpError(code, "x") }
            val r = repo(storage, remote)
            r.toggle(3)
            assertEquals("3", storage.pendingAdds, "codigo $code")
            assertEquals(setOf(3), r.ids.value, "codigo $code")
        }
    }

    @Test fun networkErrorKeepsPending() {
        val storage = FakeStorage(); val remote = FakeRemote()
        remote.addResult = { ApiResult.NetworkError("offline") }
        val r = repo(storage, remote)
        r.toggle(4)
        assertEquals("4", storage.pendingAdds)
    }

    @Test fun validationErrorDiscardsPendingChange() {
        val storage = FakeStorage(); val remote = FakeRemote()
        remote.addResult = { ApiResult.HttpError(422, "invalid") }
        val r = repo(storage, remote)
        r.toggle(9)
        assertEquals("", storage.pendingAdds)
    }

    @Test fun removalIsSyncedAndServerListWinsAfterwards() {
        val storage = FakeStorage().apply { ids = "1,2" }
        val remote = FakeRemote().apply { server.addAll(listOf(1, 2)) }
        val r = repo(storage, remote)
        r.toggle(1)
        assertEquals(setOf(2), remote.server)
        assertEquals(setOf(2), r.ids.value)
        assertEquals("", storage.pendingRemoves)
    }

    @Test fun pendingChangesSurviveRestart() {
        val storage = FakeStorage(); val remote = FakeRemote()
        remote.addResult = { ApiResult.NetworkError("offline") }
        repo(storage, remote).toggle(8)
        val restarted = repo(storage, FakeRemote())
        runNow { restarted.sync() }
        assertEquals("", storage.pendingAdds)
        assertEquals(setOf(8), restarted.ids.value)
    }
}
