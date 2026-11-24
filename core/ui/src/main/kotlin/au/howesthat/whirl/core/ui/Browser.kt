package au.howesthat.whirl.core.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsClient
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri

/**
 * Opens [url] in a Custom Tab (the user's browser, with its Whirlpool login), falling back to a regular browser
 * activity. Pins the target to a browser package so URLs claimed by Whirl (`whrl.pl`, `*.whirlpool.net.au`) never
 * bounce back into the app. Used for private forums, posting and "Open in browser".
 */
fun openInBrowser(context: Context, url: String) {
    val uri = url.toUri()
    val customTabsPackage = CustomTabsClient.getPackageName(context, null)
    if (customTabsPackage != null) {
        try {
            val customTabs = CustomTabsIntent.Builder().setShowTitle(true).build()
            customTabs.intent.setPackage(customTabsPackage)
            customTabs.launchUrl(context, uri)
            return
        } catch (_: ActivityNotFoundException) {
            // Fall through to a regular browser intent.
        }
    }
    val intent = Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browserPackage = defaultBrowserPackage(context)
    if (browserPackage != null) {
        intent.setPackage(browserPackage)
    } else if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName == context.packageName) {
        Toast.makeText(context, R.string.no_browser, Toast.LENGTH_SHORT).show()
        return
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, R.string.no_browser, Toast.LENGTH_SHORT).show()
    }
}

private fun defaultBrowserPackage(context: Context): String? {
    val probe = Intent(Intent.ACTION_VIEW, "https://example.com".toUri())
        .addCategory(Intent.CATEGORY_BROWSABLE)
    val pm = context.packageManager
    val resolved = pm.resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
        ?.activityInfo?.packageName
        ?.takeIf { it != context.packageName && it != "android" }
    if (resolved != null) return resolved
    return pm.queryIntentActivities(probe, PackageManager.MATCH_ALL)
        .asSequence()
        .map { it.activityInfo.packageName }
        .firstOrNull { it != context.packageName && it != "android" }
}

/** [openInBrowser] bound to the current context. */
@Composable
fun rememberOpenInBrowser(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) { { url: String -> openInBrowser(context, url) } }
}
