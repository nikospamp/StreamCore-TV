package com.pampoukidis.streamcoretv.core.ui.components

import com.pampoukidis.streamcoretv.core.model.auth.ProfileAvatarCatalog
import com.pampoukidis.streamcoretv.core.ui.R

enum class StreamCoreProfileAvatar(
    val assetId: String,
    val drawableRes: Int,
) {
    Avatar01(
        assetId = ProfileAvatarCatalog.Avatar01,
        drawableRes = R.drawable.profile_avatar_01,
    ),
    Avatar03(
        assetId = ProfileAvatarCatalog.Avatar03,
        drawableRes = R.drawable.profile_avatar_03,
    ),
    Avatar04(
        assetId = ProfileAvatarCatalog.Avatar04,
        drawableRes = R.drawable.profile_avatar_04,
    ),
    Avatar05(
        assetId = ProfileAvatarCatalog.Avatar05,
        drawableRes = R.drawable.profile_avatar_05,
    ),
    Avatar06(
        assetId = ProfileAvatarCatalog.Avatar06,
        drawableRes = R.drawable.profile_avatar_06,
    ),
    Avatar07(
        assetId = ProfileAvatarCatalog.Avatar07,
        drawableRes = R.drawable.profile_avatar_07,
    ),
    Avatar08(
        assetId = ProfileAvatarCatalog.Avatar08,
        drawableRes = R.drawable.profile_avatar_08,
    ),
    Avatar09(
        assetId = ProfileAvatarCatalog.Avatar09,
        drawableRes = R.drawable.profile_avatar_09,
    ),
    Avatar10(
        assetId = ProfileAvatarCatalog.Avatar10,
        drawableRes = R.drawable.profile_avatar_10,
    ),
    Avatar11(
        assetId = ProfileAvatarCatalog.Avatar11,
        drawableRes = R.drawable.profile_avatar_11,
    ),
    Avatar12(
        assetId = ProfileAvatarCatalog.Avatar12,
        drawableRes = R.drawable.profile_avatar_12,
    ),
    Avatar13(
        assetId = ProfileAvatarCatalog.Avatar13,
        drawableRes = R.drawable.profile_avatar_13,
    ),
    Avatar14(
        assetId = ProfileAvatarCatalog.Avatar14,
        drawableRes = R.drawable.profile_avatar_14,
    ),
    Avatar15(
        assetId = ProfileAvatarCatalog.Avatar15,
        drawableRes = R.drawable.profile_avatar_15,
    ),
    Avatar16(
        assetId = ProfileAvatarCatalog.Avatar16,
        drawableRes = R.drawable.profile_avatar_16,
    ),
    Avatar17(
        assetId = ProfileAvatarCatalog.Avatar17,
        drawableRes = R.drawable.profile_avatar_17,
    ),
    Avatar18(
        assetId = ProfileAvatarCatalog.Avatar18,
        drawableRes = R.drawable.profile_avatar_18,
    ),
    Avatar19(
        assetId = ProfileAvatarCatalog.Avatar19,
        drawableRes = R.drawable.profile_avatar_19,
    ),
    Avatar20(
        assetId = ProfileAvatarCatalog.Avatar20,
        drawableRes = R.drawable.profile_avatar_20,
    ),
    Avatar21(
        assetId = ProfileAvatarCatalog.Avatar21,
        drawableRes = R.drawable.profile_avatar_21,
    ),

    ;

    companion object {
        fun fromAssetId(assetId: String): StreamCoreProfileAvatar? {
            return entries.firstOrNull { it.assetId == assetId }
        }

        fun fallbackFor(assetId: String): StreamCoreProfileAvatar {
            return fromAssetId(assetId) ?: entries[
                Math.floorMod(assetId.hashCode(), entries.size)
            ]
        }
    }
}
