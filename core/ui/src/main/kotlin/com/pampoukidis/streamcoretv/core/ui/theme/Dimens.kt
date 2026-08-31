package com.pampoukidis.streamcoretv.core.ui.theme

import androidx.compose.ui.unit.dp

@Suppress("MayBeConstant")
object StreamCoreDimens {
    object Spacing {
        val Tiny = 4.dp
        val Small = 8.dp
        val Medium = 12.dp
        val Large = 16.dp
        val ExtraLarge = 24.dp
    }

    object Stroke {
        val Thin = 1.4.dp
        val Icon = 1.8.dp
        val Default = 2.dp
        val Progress = 3.dp
    }

    object Icon {
        val Small = 14.dp
        val Medium = 18.dp
        val Standard = 24.dp
        val Large = 32.dp
        val TouchTarget = 48.dp
        val Loading = 20.dp
        val ArtworkFallback = 160.dp
    }

    object Elevation {
        val Low = 2.dp
        val Medium = 8.dp
    }

    object Indicator {
        val DotSize = 5.dp
        val SelectedDotWidth = 18.dp
        val DotSpacing = 5.dp
    }

    object Carousel {
        val PreviewHeight = 240.dp
    }

    object Artwork {
        val PosterAspectRatio = 2f / 3f
        val LandscapeAspectRatio = 16f / 9f
        val ContentPadding = Spacing.Small
        val ProgressHeight = Stroke.Progress
        val ProgressInset = Spacing.Small
    }

    object Button {
        val MinHeight = 52.dp
        val CompactHeight = 38.dp
        val CompactHorizontalPadding = Spacing.Large
        val LoadingIndicatorSize = Icon.Loading
        val LoadingIndicatorStrokeWidth = Stroke.Default
    }

    object Chip {
        val IndicatorSize = Icon.Small
        val MinHeight = Icon.Large
        val ContentHorizontalPadding = Spacing.Medium
        val ContentVerticalPadding = Spacing.Small
        val PreviewHeight = Icon.TouchTarget
    }

    object Motion {
        val EntranceOffset = 80.dp
    }

    object Form {
        val ContentMaxWidth = 520.dp
        val WideContentMaxWidth = 720.dp
        val FieldMinWidth = 280.dp
        val FieldMaxWidth = 460.dp
    }

    object Mobile {
        object Navigation {
            val MaxWidth = 360.dp
            val Height = 68.dp
            val InnerPadding = Spacing.Tiny
            val BottomContentClearance = 108.dp
        }

        object Screen {
            val HorizontalPadding = 20.dp
            val VerticalPadding = Spacing.Medium
        }

        object Login {
            val LogoWidth = 120.dp
            val CardMaxWidth = 420.dp
        }

        object Browse {
            val HeroMinHeight = 280.dp
            val SectionSpacing = Spacing.ExtraLarge
            val RowSpacing = Spacing.Medium
            val ContinueWatchingWidth = 192.dp
            val PosterWidth = 120.dp
            val LandscapeWidth = 192.dp
            val TopTenShelfPadding = Spacing.Large
            val TopTenRowSpacing = Spacing.Large
            val TopTenWidth = 160.dp
            val TopTenHeight = 180.dp
            val TopTenPosterWidth = 120.dp
        }

        object Details {
            val RecommendationCardWidth = 142.dp
        }

        object Library {
            val LoadingTitleWidth = 132.dp
            val LoadingTitleHeight = 20.dp
            val LoadingPosterHeight = 180.dp
            val LoadingLandscapeHeight = 108.dp
        }

        object Player {
            val SeekFeedbackHorizontalOffset = 72.dp
            val OverlayHorizontalPadding = Screen.HorizontalPadding
            val OverlayVerticalPadding = 10.dp
            val TimelineBufferedTrackHeight = Spacing.Tiny
            val TimelineActiveTrackHeight = Spacing.Large
            val FilmstripFocusedFrameWidth = 116.dp
            val FilmstripFrameWidth = 96.dp
            val FilmstripFocusedFrameHeight = 70.dp
            val FilmstripFrameHeight = 58.dp
            val ErrorMaxWidth = 420.dp
            val LargeControlSize = 64.dp
            val LargeControlIconSize = 36.dp
            val SettingsHeaderHeight = 96.dp
            val SettingsRowHeight = 72.dp
            val SettingsSelectionRowHeight = 68.dp
            val SettingsIconContainerSize = 40.dp
            val SettingsIconSize = Icon.Standard
            val SettingsDividerStartPadding = 80.dp
        }

        object Search {
            val FieldHeight = Button.MinHeight
            val TrendingArtworkWidth = 112.dp
        }

        object Profiles {
            val AvatarSize = 104.dp
            val PreviewGridHeight = 360.dp
            val AvatarPickerMaxWidth = 400.dp
            val AvatarPickerGridMaxHeight = 420.dp
            val AvatarPickerItemSize = 64.dp
            val SelectedBadgeSize = 22.dp
            val BadgeOffset = Stroke.Default
            val HeaderHeight = 56.dp
            val HeaderSideClearance = 76.dp
            val EditorTopBarSideWidth = 72.dp
            val EditorAvatarSize = 120.dp
            val EditorAvatarContainerSize = 132.dp
            val EditorBadgeSize = 36.dp
            val TileMinWidth = 136.dp
            val TileHeight = 148.dp
            val GridBottomPadding = Icon.Large
            val LoadingLabelWidth = 72.dp
            val LoadingLabelHeight = Icon.Small
            val SelectionProgressSize = 28.dp
            val TileActionOffsetX = Spacing.Small
            val TileActionOffsetY = Spacing.Tiny
            val TileActionSize = Icon.Large
            val TileEditIconSize = Spacing.Large
        }
    }

    object Tablet {
        object Screen {
            val HorizontalPadding = 32.dp
            val VerticalPadding = Spacing.ExtraLarge
        }

        object Login {
            val CardMinWidth = 360.dp
            val CardMaxWidth = 460.dp
        }

        object Browse {
            val SectionSpacing = Spacing.ExtraLarge
            val RowSpacing = Spacing.Medium
            val HeroHeight = 360.dp
            val ExpandedHeroAspectRatio = 4f
            val BookmarkPanelWidth = 206.dp
            val BookmarkThumbnailWidth = 82.dp
            val BookmarkThumbnailHeight = 56.dp
            val PosterWidth = 180.dp
            val LandscapeWidth = 320.dp
            val TopTenWidth = 220.dp
            val TopTenPosterWidth = 180.dp
        }

        object Details {
            val RecommendationCardWidth = 190.dp
            val ActionsMaxWidth = 420.dp
        }

        object Profiles {
            val PanelWidth = 260.dp
            val GridMinCellWidth = 180.dp
        }
    }

    object Tv {
        object Navigation {
            val CollapsedWidth = 72.dp
            val ExpandedWidth = 248.dp
            val ContentStartPadding = CollapsedWidth + Screen.HorizontalPadding
            val ItemHeight = 52.dp
        }

        object Screen {
            val HorizontalPadding = 32.dp
            val VerticalPadding = 32.dp
        }

        object Panel {
            val Width = 420.dp
            val Padding = Spacing.Large
        }

        object Loading {
            val TitleWidth = 180.dp
            val TitleHeight = Icon.Standard
        }

        object Focus {
            val BorderWidth = Stroke.Default
            val BorderPadding = Stroke.Default
        }

        object Browse {
            val HeroHeight = 360.dp
            val FeaturedCardWidth = 360.dp
            val PosterCardWidth = 160.dp
            val LandscapeCardWidth = 248.dp
            val TopTenCardWidth = 176.dp
        }

        object Search {
            val FieldMaxWidth = 560.dp
            val RecentItemWidth = 220.dp
        }

        object Details {
            val RecommendationCardWidth = 248.dp
        }

        object Player {
            val FilmstripFocusedFrameWidth = 176.dp
            val FilmstripFrameWidth = 144.dp
            val FilmstripFocusedFrameHeight = 100.dp
            val FilmstripFrameHeight = 84.dp
            val SettingsPanelWidth = 560.dp
            val SettingsPanelMaxHeight = 760.dp
            val ErrorMaxWidth = 520.dp
        }

        object Profiles {
            val AvatarSize = 128.dp
            val TileWidth = 168.dp
            val TileHeight = 184.dp
            val FocusContainerSize = AvatarSize + 12.dp
            val HeaderHeight = 64.dp
            val HeaderSideClearance = 160.dp
            val LoadingTileHeight = 176.dp
            val LoadingLabelWidth = 88.dp
            val LoadingLabelHeight = Icon.Small
            val SelectionProgressSize = 36.dp
            val BadgeOffsetX = Spacing.Tiny
            val BadgeOffsetY = Stroke.Default
            val BadgeSize = 36.dp
            val EditIconSize = Icon.Medium
        }
    }
}
