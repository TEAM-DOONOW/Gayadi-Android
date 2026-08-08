package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.BasicInfoEntity
import com.gayadi.android.data.model.UserProfileEntity
import com.gayadi.android.domain.model.BasicInfo
import com.gayadi.android.domain.model.UserProfile

/** Converts a domain profile into its local data representation. */
fun BasicInfo.toEntity(): BasicInfoEntity = BasicInfoEntity(nickname, introduction)

/** Converts a local profile entity into the domain model. */
fun BasicInfoEntity.toDomain(): BasicInfo = BasicInfo(nickname, introduction)

fun UserProfile.toEntity(): UserProfileEntity = UserProfileEntity(
    nickname = nickname,
    introduction = introduction,
    resultCode = resultCode,
    travelStyleName = travelStyleName,
    characterKey = characterKey,
    strengths = strengths,
    weaknesses = weaknesses,
)

fun UserProfileEntity.toDomain(): UserProfile = UserProfile(
    nickname = nickname,
    introduction = introduction,
    resultCode = resultCode,
    travelStyleName = travelStyleName,
    characterKey = characterKey,
    strengths = strengths,
    weaknesses = weaknesses,
)
