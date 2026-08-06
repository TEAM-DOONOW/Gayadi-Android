package com.gayadi.android.data.mapper

import com.gayadi.android.data.model.BasicInfoEntity
import com.gayadi.android.domain.model.BasicInfo

/** Converts a domain profile into its local data representation. */
fun BasicInfo.toEntity(): BasicInfoEntity = BasicInfoEntity(nickname, introduction)

/** Converts a local profile entity into the domain model. */
fun BasicInfoEntity.toDomain(): BasicInfo = BasicInfo(nickname, introduction)
