package com.gorilla.music.data.repo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistRepositoryTest {

    @Test
    fun favoritesPlaylistNamesAreReservedForTheSystemCollection() {
        assertTrue(isFavoritesPlaylistName("Favorites"))
        assertTrue(isFavoritesPlaylistName("Favourites"))
        assertTrue(isFavoritesPlaylistName("fAvOuRiTeS"))
    }

    @Test
    fun regularPlaylistNamesRemainVisible() {
        assertFalse(isFavoritesPlaylistName("Favorite Albums"))
        assertFalse(isFavoritesPlaylistName("Road Trip"))
    }
}
