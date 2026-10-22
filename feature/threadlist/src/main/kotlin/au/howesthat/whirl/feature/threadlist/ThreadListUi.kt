package au.howesthat.whirl.feature.threadlist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.howesthat.whirl.core.data.OpenThread
import au.howesthat.whirl.core.data.WatchAction
import au.howesthat.whirl.core.model.ForumRef
import au.howesthat.whirl.core.model.RowKind
import au.howesthat.whirl.core.ui.ActionsMenu
import au.howesthat.whirl.core.ui.EmptyState
import au.howesthat.whirl.core.ui.MenuAction
import au.howesthat.whirl.core.ui.menuClickable
import au.howesthat.whirl.core.ui.ErrorState
import au.howesthat.whirl.core.ui.WhirlThemeTokens
import au.howesthat.whirl.core.ui.errorMessage
import au.howesthat.whirl.core.ui.rememberOpenInBrowser
import au.howesthat.whirl.core.ui.timeAgo
import java.time.Instant

/** Navigation out of a thread list, implemented by the app's NavHost. */
class ThreadListNav(
    val openThread: (OpenThread.InApp) -> Unit,
    val openForum: (ForumRef) -> Unit,
    val openSearch: (SearchRoute) -> Unit,
)

/** Turns an [OpenThread] decision into navigation or a Custom Tab. */
@Composable
fun rememberThreadLauncher(nav: ThreadListNav): (OpenThread?) -> Unit {
    val browser = rememberOpenInBrowser()
    return remember(nav, browser) {
        { target ->
            when (target) {
                is OpenThread.InApp -> nav.openThread(target)
                is OpenThread.Browser -> browser(target.url)
                null -> Unit
            }
        }
    }
}

/**
 * A thread list with pull to refresh. With nothing cached, an error fills the screen (Retry, Open in browser);
 * with a cached list, the error shows above it and the list stays usable.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListBody(
    sections: List<Section>?,
    refreshing: Boolean,
    error: Throwable?,
    onRefresh: () -> Unit,
    viewModel: ListViewModel,
    nav: ThreadListNav,
    emptyText: String,
    modifier: Modifier = Modifier,
    /** The page to offer in the browser when the list fails; null hides the button. */
    browserUrl: String? = null,
    /** Cross-forum lists offer "Go to forum" on rows and make headers tappable. */
    showForum: Boolean = sections?.any { it.forum != null } == true,
    header: (LazyListScope.() -> Unit)? = null,
    footer: (LazyListScope.() -> Unit)? = null,
) {
    val launch = rememberThreadLauncher(nav)
    val browser = rememberOpenInBrowser()
    val now = Instant.now()
    // Read so rows recompose when a watch action changes what they offer.
    val watchedIds by viewModel.watchedIds.collectAsStateWithLifecycle()
    PullToRefreshBox(isRefreshing = refreshing, onRefresh = onRefresh, modifier = modifier.fillMaxSize()) {
        when {
            sections == null && error != null -> ErrorState(
                error,
                onRetry = onRefresh,
                modifier = Modifier.fillMaxWidth(),
                onOpenInBrowser = browserUrl?.let { url -> { browser(url) } },
            )
            else -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                header?.invoke(this)
                if (error != null) {
                    item(key = "error") {
                        Text(
                            errorMessage(error),
                            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.errorContainer).padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                if (sections != null && sections.all { it.items.isEmpty() }) {
                    item(key = "empty") {
                        EmptyState(
                            emptyText,
                            Modifier.fillMaxWidth(),
                            action = browserUrl?.let { url -> { OutlinedButton(onClick = { browser(url) }) { Text(stringResource(au.howesthat.whirl.core.ui.R.string.open_in_browser)) } } },
                        )
                    }
                }
                sections.orEmpty().forEachIndexed { index, section ->
                    section.forum?.let { forum ->
                        item(key = "section-$index") { SectionHeader(forum, onClick = { nav.openForum(forum) }) }
                    }
                    itemsIndexed(section.items, key = { _, item -> "$index-${item.id.value}" }) { rowIndex, item ->
                        ThreadGroupRow(
                            first = rowIndex == 0,
                            last = rowIndex == section.items.lastIndex,
                            topSpacing = rowIndex == 0 && section.forum == null,
                        ) {
                            ThreadRowItem(
                                item = item,
                                now = now,
                                onOpen = { launch(viewModel.open(item)) },
                                onLastPost = { launch(viewModel.lastPost(item)) },
                                onOpenInBrowser = { browser(viewModel.browserUrl(item)) },
                                onOpenForum = item.forum?.takeIf { showForum }?.let { forum -> { nav.openForum(forum.copy(groupId = null)) } },
                                watchActions = remember(item, watchedIds) { viewModel.watchActions(item) },
                                onWatchAction = { action -> viewModel.perform(action, item) },
                            )
                            if (rowIndex != section.items.lastIndex) {
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            }
                        }
                    }
                }
                footer?.invoke(this)
            }
        }
    }
}

@Composable
private fun SectionHeader(forum: ForumRef, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().semantics { heading() }.clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            forum.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Each row stays lazy while the outer edges form one continuous forum panel. */
@Composable
private fun ThreadGroupRow(first: Boolean, last: Boolean, topSpacing: Boolean, content: @Composable () -> Unit) {
    val corner = 18.dp
    val shape = RoundedCornerShape(
        topStart = if (first) corner else 0.dp, topEnd = if (first) corner else 0.dp,
        bottomStart = if (last) corner else 0.dp, bottomEnd = if (last) corner else 0.dp,
    )
    val border = MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier.padding(horizontal = 16.dp).padding(top = if (topSpacing) 12.dp else 0.dp, bottom = if (last) 8.dp else 0.dp)
            .drawWithContent {
                drawContent()
                val half = 0.5.dp.toPx()
                val radius = minOf(corner.toPx(), size.height / 2)
                val path = Path().apply {
                    moveTo(half, if (first) radius else 0f)
                    lineTo(half, if (last) size.height - radius else size.height)
                    if (last) {
                        quadraticTo(half, size.height - half, radius, size.height - half)
                        lineTo(size.width - radius, size.height - half)
                        quadraticTo(size.width - half, size.height - half, size.width - half, size.height - radius)
                    } else moveTo(size.width - half, size.height)
                    lineTo(size.width - half, if (first) radius else 0f)
                    if (first) {
                        quadraticTo(size.width - half, half, size.width - radius, half)
                        lineTo(radius, half)
                        quadraticTo(half, half, half, radius)
                    }
                }
                drawPath(path, border, style = Stroke(1.dp.toPx()))
            },
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
    ) { Column { content() } }
}

@Composable
fun ThreadRowItem(
    item: ThreadItem,
    now: Instant,
    onOpen: () -> Unit,
    onLastPost: () -> Unit,
    onOpenInBrowser: () -> Unit,
    onOpenForum: (() -> Unit)?,
    watchActions: List<WatchAction> = emptyList(),
    onWatchAction: (WatchAction) -> Unit = {},
) {
    var menu by remember { mutableStateOf(false) }
    val openable = item.openId != null
    val inactive = item.kind != RowKind.THREAD
    val unread = item.isUnread || item.unreadCount > 0
    val accent = MaterialTheme.colorScheme.primary
    val container = when {
        unread -> WhirlThemeTokens.unreadContainer
        item.isSticky -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val actions = watchActions.map { action -> MenuAction(watchActionLabel(action)) { onWatchAction(action) } } +
        MenuAction(stringResource(R.string.row_last_post), enabled = openable, onClick = onLastPost) +
        MenuAction(stringResource(au.howesthat.whirl.core.ui.R.string.open_in_browser), onClick = onOpenInBrowser) +
        listOfNotNull(onOpenForum?.let { MenuAction(stringResource(R.string.row_go_to_forum, item.forum?.title.orEmpty()), onClick = it) })
    Box {
        ListItem(
            modifier = Modifier
                .menuClickable(
                    actions,
                    enabled = openable,
                    onLongClick = { menu = true },
                    onClick = onOpen,
                    onLongClickLabel = stringResource(au.howesthat.whirl.core.ui.R.string.show_actions),
                )
                .alpha(if (inactive) 0.6f else 1f)
                .drawWithContent {
                    drawContent()
                    if (unread) {
                        drawRoundRect(
                            color = accent,
                            topLeft = Offset(if (layoutDirection == androidx.compose.ui.unit.LayoutDirection.Ltr) 0f else size.width - 3.dp.toPx(), 16.dp.toPx()),
                            size = Size(3.dp.toPx(), minOf(26.dp.toPx(), size.height - 24.dp.toPx())),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    }
                },
            colors = ListItemDefaults.colors(containerColor = container),
            overlineContent = labels(item)?.let { { Text(it, maxLines = 1, overflow = TextOverflow.Ellipsis) } },
            headlineContent = {
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                    fontStyle = if (inactive) FontStyle.Italic else null,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(details(item, now), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            },
            trailingContent = if (item.unreadCount > 0) {
                {
                    val unread = pluralStringResource(R.plurals.row_unread, item.unreadCount, item.unreadCount)
                    Surface(
                        modifier = Modifier.clearAndSetSemantics { contentDescription = unread },
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(stringResource(R.string.row_new_count, item.unreadCount), Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            } else {
                null
            },
        )
        ActionsMenu(menu, onDismiss = { menu = false }, actions)
    }
}

@Composable
private fun watchActionLabel(action: WatchAction): String = stringResource(
    when (action) {
        WatchAction.WATCH -> R.string.row_watch
        WatchAction.MARK_READ -> R.string.row_mark_read
        WatchAction.UNWATCH -> R.string.row_unwatch
    },
)

@Composable
private fun labels(item: ThreadItem): String? = listOfNotNull(
    stringResource(R.string.row_sticky).takeIf { item.isSticky },
    stringResource(R.string.row_closed).takeIf { item.isClosed },
    stringResource(R.string.row_moved).takeIf { item.kind == RowKind.MOVED },
    stringResource(R.string.row_deleted).takeIf { item.kind == RowKind.DELETED },
    item.group,
).joinToString(" · ").ifEmpty { null }

@Composable
private fun details(item: ThreadItem, now: Instant): String {
    if (item.kind != RowKind.THREAD) return item.note.orEmpty()
    val parts = mutableListOf<String>()
    item.replies?.let { parts += pluralStringResource(R.plurals.row_replies, it, it) }
    item.pageCount?.takeIf { it > 1 }?.let { parts += pluralStringResource(R.plurals.row_pages, it, it) }
    val time = item.lastPostedAt?.let { timeAgo(it, now) } ?: item.lastPostedText
    when {
        item.lastPoster != null && time != null -> parts += stringResource(R.string.row_last_by_at, item.lastPoster, time)
        item.lastPoster != null -> parts += stringResource(R.string.row_last_by, item.lastPoster)
        time != null -> parts += time
    }
    return parts.joinToString(" · ")
}

/** Title plus "Updated N ago" for a list's top bar. */
@Composable
fun ListTitle(title: String, updatedAt: Instant?, subtitle: String? = null) {
    Column {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val line = listOfNotNull(subtitle, updatedAt?.let { timeAgoUpdated(it) }).joinToString(" · ")
        if (line.isNotEmpty()) {
            Text(line, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        }
    }
}

@Composable
private fun timeAgoUpdated(at: Instant): String = stringResource(R.string.updated_ago, timeAgo(at, Instant.now()))
