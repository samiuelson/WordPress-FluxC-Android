package org.wordpress.android.fluxc.release

import kotlinx.coroutines.experimental.runBlocking
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.TestUtils
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.list.ListDescriptor
import org.wordpress.android.fluxc.model.list.ListItemDataSource
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForRestSite
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.fluxc.store.ListStore.OnListChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostListPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ReleaseStack_PostListTest : ReleaseStack_WPComBase() {
    internal enum class TestEvent {
        NONE,
        FETCHED_FIRST_PAGE,
        LIST_STATE_CHANGED
    }

    @Inject internal lateinit var listStore: ListStore
    @Inject internal lateinit var postStore: PostStore

    private var nextEvent: TestEvent = TestEvent.NONE

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        mReleaseStackAppComponent.inject(this)
        // Register
        init()
        // Reset expected test event
        nextEvent = TestEvent.NONE
    }

    @Throws(InterruptedException::class)
    @Test
    fun fetchFirstPage() {
        val postListDescriptor = PostListDescriptorForRestSite(sSite)
        val dataSource = listItemDataSource { listDescriptor, _, offset ->
            assertEquals(TestEvent.LIST_STATE_CHANGED, nextEvent)
            // Assert that we are fetching the correct ListDescriptor
            assertEquals(postListDescriptor, listDescriptor)
            // We will do an actual fetch now, update the event so OnListChanged can check for it
            nextEvent = TestEvent.FETCHED_FIRST_PAGE
            // Fetch the post list for the given ListDescriptor and offset
            val fetchPostListPayload = FetchPostListPayload(postListDescriptor, offset)
            mDispatcher.dispatch(PostActionBuilder.newFetchPostListAction(fetchPostListPayload))
        }
        // Get the initial ListManager from ListStore and assert that everything is as expected
        val listManagerBefore = runBlocking {
            listStore.getListManager(postListDescriptor, null, dataSource)
        }
        assertEquals(0, listManagerBefore.size)
        assertFalse(listManagerBefore.isFetchingFirstPage)
        assertFalse(listManagerBefore.isLoadingMore)

        // The first expected event is the list change
        nextEvent = TestEvent.LIST_STATE_CHANGED
        // 2 events should happen in total:
        // First event will be for list state change and the second will be for completed fetch
        mCountDownLatch = CountDownLatch(2)
        // Call `refresh` on the ListManager which should trigger the state change and then fetch the list
        listManagerBefore.refresh()
        assertTrue(mCountDownLatch.await(TestUtils.DEFAULT_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS))

        // Assert that the expected event is updated within `ListItemDataSource.fetchList`
        assertEquals(TestEvent.FETCHED_FIRST_PAGE, nextEvent)
        // Retrieve the updated ListManager from ListStore and assert that we have data and the state is as expected
        val listManagerAfter = runBlocking {
            listStore.getListManager(postListDescriptor, null, dataSource)
        }
        assertFalse(listManagerAfter.size == 0)
        assertFalse(listManagerAfter.isFetchingFirstPage)
        assertFalse(listManagerAfter.isLoadingMore)
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onListChanged(event: OnListChanged) {
        event.error?.let {
            throw AssertionError("OnListChanged has error: " + it.type)
        }
        mCountDownLatch.countDown()
    }

    private fun listItemDataSource(fetchList: (ListDescriptor, Boolean, Int) -> Unit): ListItemDataSource<PostModel> =
            object : ListItemDataSource<PostModel> {
                override fun fetchItem(listDescriptor: ListDescriptor, remoteItemId: Long) {
                }

                override fun fetchList(listDescriptor: ListDescriptor, loadMore: Boolean, offset: Int) {
                    fetchList(listDescriptor, loadMore, offset)
                }

                override fun getItems(listDescriptor: ListDescriptor, remoteItemIds: List<Long>): Map<Long, PostModel> {
                    return emptyMap()
                }
            }
}
