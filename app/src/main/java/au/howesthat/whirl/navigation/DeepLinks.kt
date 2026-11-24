package au.howesthat.whirl.navigation

import android.net.Uri
import au.howesthat.whirl.core.data.ThreadPageRepository
import au.howesthat.whirl.core.data.WhirlpoolLink
import au.howesthat.whirl.feature.thread.ThreadRoute
import au.howesthat.whirl.feature.threadlist.ForumRoute
import au.howesthat.whirl.feature.user.UserRoute
import kotlinx.coroutines.CancellationException

/** Where an incoming deep link should open. */
sealed interface DeepLinkTarget {
    data class Route(val route: Any) : DeepLinkTarget
    data class Browser(val url: String) : DeepLinkTarget
}

/**
 * The immediate route for a link the app was opened with: `*.whirlpool.net.au` (`/thread/`, `/archive/`, `/forum/`,
 * `/user/`, `/forum-replies.cfm`, `/forum-threads.cfm` — see the manifest's intent filters) or Whirldroid's old
 * `whirldroid-thread://com.gregdev.whirldroid?threadid=<id>`.
 * Null for links that require network resolution (`whrl.pl`, reply locators) or should open in the browser.
 */
fun routeForLink(uri: Uri): Any? {
    if (uri.scheme == LEGACY_DEEP_LINK_SCHEME) {
        val id = uri.getQueryParameter("threadid")?.takeIf { it.isNotEmpty() && it.all(Char::isLetterOrDigit) } ?: return null
        return ThreadRoute(id)
    }
    return routeForLink(uri.toString())
}

fun routeForLink(url: String): Any? = routeFor(WhirlpoolLink.parse(url))

private fun routeFor(link: WhirlpoolLink): Any? = when (link) {
    is WhirlpoolLink.Thread -> ThreadRoute(
        link.id.value,
        page = link.page ?: 1,
        scrollToPost = link.anchor?.takeIf { !link.isReplyIdAnchor }?.toInt(),
        scrollToReply = link.anchor?.takeIf { link.isReplyIdAnchor },
    )
    is WhirlpoolLink.Archive -> ThreadRoute(link.id.value)
    // The forum screen falls back to the title on the page.
    is WhirlpoolLink.Forum -> ForumRoute(link.id, title = "", groupId = link.groupId)
    is WhirlpoolLink.User -> UserRoute(link.id)
    is WhirlpoolLink.Reply, is WhirlpoolLink.Short, is WhirlpoolLink.External -> null
}

/**
 * Resolves an HTTP(S) deep link into either an in-app route or a browser fallback.
 * Short links (`whrl.pl/<code>`) and reply locators (`forum-replies.cfm?r=`, `/forum/?action=replies&r=`) are followed
 * over the network; if resolution fails or ends outside supported Whirlpool pages, falls back to the browser.
 */
suspend fun resolveDeepLink(
    url: String,
    resolveShort: suspend (String) -> WhirlpoolLink,
    locateReply: suspend (Long) -> ThreadPageRepository.Located,
): DeepLinkTarget = resolveParsedLink(
    link = WhirlpoolLink.parse(url),
    fallbackUrl = url,
    resolveShort = resolveShort,
    locateReply = locateReply,
)

private suspend fun resolveParsedLink(
    link: WhirlpoolLink,
    fallbackUrl: String,
    resolveShort: suspend (String) -> WhirlpoolLink,
    locateReply: suspend (Long) -> ThreadPageRepository.Located,
    allowShort: Boolean = true,
): DeepLinkTarget {
    routeFor(link)?.let { return DeepLinkTarget.Route(it) }
    return when (link) {
        is WhirlpoolLink.Reply -> {
            val located = try {
                locateReply(link.replyId)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            if (located != null) {
                DeepLinkTarget.Route(ThreadRoute(located.threadId.value, located.page, scrollToPost = located.postNumber))
            } else {
                DeepLinkTarget.Browser(fallbackUrl)
            }
        }
        is WhirlpoolLink.Short -> {
            if (!allowShort) {
                DeepLinkTarget.Browser(fallbackUrl)
            } else {
                val target = try {
                    resolveShort(link.url)
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
                if (target == null || target is WhirlpoolLink.Short) {
                    DeepLinkTarget.Browser(fallbackUrl)
                } else {
                    resolveParsedLink(
                        link = target,
                        fallbackUrl = (target as? WhirlpoolLink.External)?.url ?: link.url,
                        resolveShort = resolveShort,
                        locateReply = locateReply,
                        allowShort = false,
                    )
                }
            }
        }
        is WhirlpoolLink.External -> DeepLinkTarget.Browser(link.url)
        is WhirlpoolLink.Thread, is WhirlpoolLink.Archive, is WhirlpoolLink.Forum, is WhirlpoolLink.User ->
            error("Handled by routeFor above")
    }
}

internal const val LEGACY_DEEP_LINK_SCHEME = "whirldroid-thread"
