package io.github.youndie.toon.parity

import io.github.youndie.toon.component.ToonNavItem
import io.github.youndie.toon.icon.ToonIcons

/** B-27: the MVP page's four destinations (`NAV`), for the navigation bar's fixtures and frame test. */
internal object NavFrames {
    val ITEMS =
        listOf(
            ToonNavItem(ToonIcons.Home, "Home"),
            ToonNavItem(ToonIcons.Search, "Explore"),
            ToonNavItem(ToonIcons.Heart, "Saved"),
            ToonNavItem(ToonIcons.Person, "Profile"),
        )
}
