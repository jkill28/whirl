package au.howesthat.whirl.core.data

import au.howesthat.whirl.core.data.OpenThread.Browser
import au.howesthat.whirl.core.data.OpenThread.InApp
import au.howesthat.whirl.core.data.ThreadPageRepository.Companion.LAST_PAGE
import au.howesthat.whirl.core.model.ThreadId
import org.junit.Test
import kotlin.test.assertEquals

class ThreadOpenerTest {
    private val opener = ThreadOpener()
    private val id = ThreadId("3pxj858k")
    private val defaults = Settings()
    private val watched = WatchedPosition(lastPage = 3, lastReadPost = 47)

    private fun open(from: OpenedFrom, settings: Settings = defaults, private: Boolean = false, w: WatchedPosition? = null) =
        opener.open(id, 92, private, from, settings, w)

    @Test
    fun `watched threads open at first unread, top of last read page, or last post`() {
        assertEquals(InApp(id, 3, 47, 92, fromWatched = true), open(OpenedFrom.WATCHED, w = watched))
        assertEquals(
            InApp(id, 3, null, 92, fromWatched = true),
            open(OpenedFrom.WATCHED, defaults.copy(watchedThreadOpen = WatchedThreadOpen.TOP_OF_PAGE), w = watched),
        )
        assertEquals(
            InApp(id, LAST_PAGE, null, 92, fromWatched = true, scrollToLastPost = true),
            open(OpenedFrom.WATCHED, defaults.copy(watchedThreadOpen = WatchedThreadOpen.LAST_POST), w = watched),
        )
        assertEquals(InApp(id, 1, null, 92, fromWatched = true), open(OpenedFrom.WATCHED, w = WatchedPosition(1, 0)), "nothing read yet")
    }

    @Test
    fun `recent threads open at the last page unless set to the top`() {
        assertEquals(InApp(id, LAST_PAGE, forumId = 92), open(OpenedFrom.RECENT))
        assertEquals(InApp(id, 1, forumId = 92), open(OpenedFrom.RECENT, defaults.copy(loadRecentAtTop = true)))
    }

    @Test
    fun `public list threads open at first page, last page, or last post`() {
        assertEquals(InApp(id, 1, forumId = 92), open(OpenedFrom.LIST))
        assertEquals(
            InApp(id, LAST_PAGE, forumId = 92),
            open(OpenedFrom.LIST, defaults.copy(publicThreadOpen = PublicThreadOpen.LAST_PAGE)),
        )
        assertEquals(
            InApp(id, LAST_PAGE, forumId = 92, scrollToLastPost = true),
            open(OpenedFrom.LIST, defaults.copy(publicThreadOpen = PublicThreadOpen.LAST_POST)),
        )
    }

    @Test
    fun `private forum threads open in the browser`() {
        assertEquals(Browser("https://forums.whirlpool.net.au/thread/3pxj858k"), open(OpenedFrom.LIST, private = true))
        assertEquals(
            Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=-1#bottom"),
            open(OpenedFrom.LIST, defaults.copy(loadPrivateAtBottom = true), private = true),
        )
        assertEquals(Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=3#r47"), open(OpenedFrom.WATCHED, private = true, w = watched))
        assertEquals(
            Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=3"),
            open(OpenedFrom.WATCHED, defaults.copy(watchedThreadOpen = WatchedThreadOpen.TOP_OF_PAGE), private = true, w = watched),
        )
        assertEquals(
            Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=-1#bottom"),
            open(OpenedFrom.WATCHED, defaults.copy(watchedThreadOpen = WatchedThreadOpen.LAST_POST), private = true, w = watched),
        )
    }

    @Test
    fun `open in browser applies everywhere`() {
        val browser = defaults.copy(openThreadsInBrowser = true)
        assertEquals(Browser("https://forums.whirlpool.net.au/thread/3pxj858k"), open(OpenedFrom.LIST, browser))
        assertEquals(
            Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=-1"),
            open(OpenedFrom.LIST, browser.copy(publicThreadOpen = PublicThreadOpen.LAST_PAGE)),
        )
        assertEquals(
            Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=-1#bottom"),
            open(OpenedFrom.LIST, browser.copy(publicThreadOpen = PublicThreadOpen.LAST_POST)),
        )
        assertEquals(Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=-1#bottom"), open(OpenedFrom.RECENT, browser))
        assertEquals(Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=3#r47"), open(OpenedFrom.WATCHED, browser, w = watched))
        assertEquals(
            Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=-1#bottom"),
            open(OpenedFrom.WATCHED, browser.copy(watchedThreadOpen = WatchedThreadOpen.LAST_POST), w = watched),
        )
    }

    @Test
    fun `jump to last post`() {
        assertEquals(InApp(id, LAST_PAGE, forumId = 92, scrollToLastPost = true), opener.lastPost(id, 92, false, defaults))
        assertEquals(Browser("https://forums.whirlpool.net.au/thread/3pxj858k?p=-1#bottom"), opener.lastPost(id, 35, true, defaults))
    }
}
