package au.howesthat.whirl.navigation

import au.howesthat.whirl.core.data.ThreadPageRepository
import au.howesthat.whirl.core.data.WhirlpoolLink
import au.howesthat.whirl.core.model.ThreadId
import au.howesthat.whirl.feature.thread.ThreadRoute
import au.howesthat.whirl.feature.threadlist.ForumRoute
import au.howesthat.whirl.feature.user.UserRoute
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.io.IOException
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinksTest {
    private val f = "https://forums.whirlpool.net.au"

    @Test
    fun `whirlpool pages map to routes`() {
        assertEquals(ThreadRoute("3pxj858k"), routeForLink("$f/thread/3pxj858k"))
        assertEquals(ThreadRoute("3pxj858k", 2, scrollToPost = 21), routeForLink("$f/thread/3pxj858k?p=2#r21"))
        assertEquals(ThreadRoute("3pxj858k", 1, scrollToReply = 75543951), routeForLink("$f/thread/3pxj858k?ux=915415#r75543951"))
        assertEquals(ThreadRoute("1000422", 3), routeForLink("http://forums.whirlpool.net.au/forum-replies.cfm?t=1000422&p=3"))
        assertEquals(ThreadRoute("1000422"), routeForLink("$f/archive/1000422"))
        assertEquals(ForumRoute(7, "", 179), routeForLink("$f/forum/7?g=179"))
        assertEquals(ForumRoute(7, "", 179), routeForLink("$f/forum-threads.cfm?f=7&g=179"))
        assertEquals(UserRoute(30703), routeForLink("$f/user/30703"))
        assertEquals(ThreadRoute("3pxj858k"), routeForLink("https://www.whirlpool.net.au/thread/3pxj858k"))
        assertEquals(ThreadRoute("3pxj858k"), routeForLink("https://m.whirlpool.net.au/thread/3pxj858k"))
    }

    @Test
    fun `links that must stay in the browser`() {
        assertNull(routeForLink("$f/forum/?action=reply&r=75543951"))
        assertNull(routeForLink("$f/forum/?action=replies&r=75543951"))
        assertNull(routeForLink("https://whirlpool.net.au/wiki/nbn"))
        assertNull(routeForLink("https://www.whirlpool.net.au/wiki/nbn"))
        assertNull(routeForLink("https://whrl.pl/RgPkJ5"))
    }

    @Test
    fun `short links resolve to in-app routes or fall back to browser`() = runTest {
        val noLocate: suspend (Long) -> ThreadPageRepository.Located = { error("unexpected locate") }

        val threadTarget = resolveDeepLink(
            url = "https://whrl.pl/RgPkJ5",
            resolveShort = { WhirlpoolLink.parse("$f/thread/3pxj858k?p=2#r21") },
            locateReply = noLocate,
        )
        assertEquals(DeepLinkTarget.Route(ThreadRoute("3pxj858k", 2, scrollToPost = 21)), threadTarget)

        val replyTarget = resolveDeepLink(
            url = "https://whrl.pl/RgPkJ5",
            resolveShort = { WhirlpoolLink.Reply(75543951) },
            locateReply = { ThreadPageRepository.Located(ThreadId("3pxj858k"), 2, 21) },
        )
        assertEquals(DeepLinkTarget.Route(ThreadRoute("3pxj858k", 2, scrollToPost = 21)), replyTarget)

        val externalTarget = resolveDeepLink(
            url = "https://whrl.pl/RgPkJ5",
            resolveShort = { WhirlpoolLink.External("https://whirlpool.net.au/wiki/nbn") },
            locateReply = noLocate,
        )
        assertEquals(DeepLinkTarget.Browser("https://whirlpool.net.au/wiki/nbn"), externalTarget)

        val failedTarget = resolveDeepLink(
            url = "https://whrl.pl/RgPkJ5",
            resolveShort = { throw IOException("offline") },
            locateReply = noLocate,
        )
        assertEquals(DeepLinkTarget.Browser("https://whrl.pl/RgPkJ5"), failedTarget)

        val loopTarget = resolveDeepLink(
            url = "https://whrl.pl/RgPkJ5",
            resolveShort = { WhirlpoolLink.Short("https://whrl.pl/Other") },
            locateReply = noLocate,
        )
        assertEquals(DeepLinkTarget.Browser("https://whrl.pl/RgPkJ5"), loopTarget)
    }

    @Test
    fun `reply locator deep links resolve or fall back to browser`() = runTest {
        val noShort: suspend (String) -> WhirlpoolLink = { error("unexpected short") }
        val url = "$f/forum-replies.cfm?r=75543951"

        assertEquals(
            DeepLinkTarget.Route(ThreadRoute("3pxj858k", 2, scrollToPost = 21)),
            resolveDeepLink(url, noShort) { ThreadPageRepository.Located(ThreadId("3pxj858k"), 2, 21) },
        )
        assertEquals(
            DeepLinkTarget.Browser(url),
            resolveDeepLink(url, noShort) { throw IOException("offline") },
        )
    }
}
