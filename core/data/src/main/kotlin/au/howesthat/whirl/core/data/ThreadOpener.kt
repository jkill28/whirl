package au.howesthat.whirl.core.data

import au.howesthat.whirl.core.model.ThreadId
import au.howesthat.whirl.core.network.WhirlpoolUrls

/** Where tapping a thread goes. */
sealed interface OpenThread {
    /**
     * [page] may be [ThreadPageRepository.LAST_PAGE]; [scrollToPost] is a post number (1-based in the thread);
     * [scrollToLastPost] scrolls to the last post on [page]; [fromWatched] offers Mark as read and Unwatch, and
     * "back to list" after marking read.
     */
    data class InApp(
        val id: ThreadId,
        val page: Int,
        val scrollToPost: Int? = null,
        val forumId: Int? = null,
        val fromWatched: Boolean = false,
        val scrollToLastPost: Boolean = false,
    ) : OpenThread
    data class Browser(val url: String) : OpenThread
}

/** The list a thread was opened from, which decides the opening position. */
enum class OpenedFrom { WATCHED, RECENT, LIST }

/** Read position of a watched thread (`LASTPAGE`, `LASTREAD`). */
data class WatchedPosition(val lastPage: Int, val lastReadPost: Int)

/**
 * The opening rules of 2.14.12 `ForumPageFragment.openThread` (PLAN.md §1.1), extended with "last post":
 * - watched → `LASTPAGE` scrolled to the last read post, top of `LASTPAGE`, or last page scrolled to the last post;
 * - recent → last page unless "open recent at top";
 * - other lists → first page, last page, or last post (public forums), or last page if "open private at bottom";
 * - private forums, and everything when "open threads in the browser" is on → the browser, at the same position
 *   (`?p=N#rM` for watched, `?p=-1` for last page, `?p=-1#bottom` for last post).
 */
class ThreadOpener(private val urls: WhirlpoolUrls = WhirlpoolUrls()) {

    fun open(
        id: ThreadId,
        forumId: Int?,
        isPrivateForum: Boolean,
        from: OpenedFrom,
        settings: Settings,
        watched: WatchedPosition? = null,
    ): OpenThread {
        if (isPrivateForum || settings.openThreadsInBrowser) {
            val url = when (from) {
                OpenedFrom.WATCHED -> when {
                    settings.watchedThreadOpen == WatchedThreadOpen.LAST_POST -> lastPostUrl(id)
                    watched != null && settings.watchedThreadOpen == WatchedThreadOpen.TOP_OF_PAGE ->
                        urls.thread(id, watched.lastPage)
                    watched != null ->
                        urls.thread(id, watched.lastPage).newBuilder().fragment("r${watched.lastReadPost}").build()
                    else -> urls.thread(id)
                }
                OpenedFrom.RECENT -> if (!settings.loadRecentAtTop) lastPostUrl(id) else urls.thread(id)
                OpenedFrom.LIST -> if (isPrivateForum) {
                    if (settings.loadPrivateAtBottom) lastPostUrl(id) else urls.thread(id)
                } else {
                    when (settings.publicThreadOpen) {
                        PublicThreadOpen.FIRST_PAGE -> urls.thread(id)
                        PublicThreadOpen.LAST_PAGE -> urls.thread(id, ThreadPageRepository.LAST_PAGE)
                        PublicThreadOpen.LAST_POST -> lastPostUrl(id)
                    }
                }
            }
            return OpenThread.Browser(url.toString())
        }
        return when (from) {
            OpenedFrom.WATCHED -> when {
                settings.watchedThreadOpen == WatchedThreadOpen.LAST_POST -> OpenThread.InApp(
                    id,
                    page = ThreadPageRepository.LAST_PAGE,
                    forumId = forumId,
                    fromWatched = true,
                    scrollToLastPost = true,
                )
                watched != null -> OpenThread.InApp(
                    id,
                    page = watched.lastPage,
                    scrollToPost = watched.lastReadPost.takeIf { it > 0 && settings.watchedThreadOpen == WatchedThreadOpen.FIRST_UNREAD },
                    forumId = forumId,
                    fromWatched = true,
                )
                else -> OpenThread.InApp(id, 1, forumId = forumId, fromWatched = true)
            }
            OpenedFrom.RECENT -> OpenThread.InApp(
                id,
                page = if (settings.loadRecentAtTop) 1 else ThreadPageRepository.LAST_PAGE,
                forumId = forumId,
            )
            OpenedFrom.LIST -> when (settings.publicThreadOpen) {
                PublicThreadOpen.FIRST_PAGE -> OpenThread.InApp(id, 1, forumId = forumId)
                PublicThreadOpen.LAST_PAGE -> OpenThread.InApp(id, ThreadPageRepository.LAST_PAGE, forumId = forumId)
                PublicThreadOpen.LAST_POST -> OpenThread.InApp(
                    id,
                    ThreadPageRepository.LAST_PAGE,
                    forumId = forumId,
                    scrollToLastPost = true,
                )
            }
        }
    }

    /** "Jump to last post": the last page scrolled to its last post, in the browser for private forums. */
    fun lastPost(id: ThreadId, forumId: Int?, isPrivateForum: Boolean, settings: Settings): OpenThread =
        if (isPrivateForum || settings.openThreadsInBrowser) {
            OpenThread.Browser(lastPostUrl(id).toString())
        } else {
            OpenThread.InApp(id, ThreadPageRepository.LAST_PAGE, forumId = forumId, scrollToLastPost = true)
        }

    fun browserUrl(id: ThreadId): String = urls.thread(id).toString()

    private fun lastPostUrl(id: ThreadId) =
        urls.thread(id, ThreadPageRepository.LAST_PAGE).newBuilder().fragment("bottom").build()
}
