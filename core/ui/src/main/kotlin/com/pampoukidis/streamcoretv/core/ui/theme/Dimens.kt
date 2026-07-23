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
        object Screen {
            val HorizontalPadding = 20.dp
            val VerticalPadding = Spacing.Medium
        }

        object Login {
            val LogoWidth = 120.dp
            val CardMaxWidth = 420.dp
        }

        object Browse {
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

        object Profiles {
            val AvatarSize = 104.dp
            val PreviewGridHeight = 360.dp
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
        }

        object Profiles {
            val PanelWidth = 260.dp
            val GridMinCellWidth = 180.dp
        }
    }

    object Tv {
        object Screen {
            val HorizontalPadding = 32.dp
            val VerticalPadding = 32.dp
        }

        object Panel {
            val Width = 420.dp
            val Padding = Spacing.Large
        }

        object Focus {
            val BorderWidth = Stroke.Default
            val BorderPadding = Stroke.Default
        }

        object Browse {
            val FeaturedCardWidth = 440.dp
            val PosterCardWidth = 220.dp
            val LandscapeCardWidth = 320.dp
            val TopTenCardWidth = 250.dp
        }

        object Details {
            val RecommendationCardWidth = 320.dp
        }

        object Profiles {
            val GridMinCellWidth = 220.dp
            val AvatarSize = 96.dp
        }
    }
}
