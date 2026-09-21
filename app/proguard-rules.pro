# Moshi uses reflection for these public API response models. Keep only those
# model contracts; Compose and the rest of the app remain fully optimizable.
-keep class com.gayadi.android.data.model.SurveyDefinitionDto { *; }
-keep class com.gayadi.android.data.model.SurveyResultDto { *; }
-keep class com.gayadi.android.data.model.CompatibleTravelTypeDto { *; }
-keep class com.gayadi.android.data.model.TravelRoleDto { *; }
-keep class com.gayadi.android.data.model.SurveyQuestionDto { *; }
-keep class com.gayadi.android.data.model.SurveyOptionDto { *; }
-keep class com.gayadi.android.data.model.NoticeDto { *; }
-keep class com.gayadi.android.data.model.NoticeSectionDto { *; }
-keep class com.gayadi.android.data.model.LegalDocumentDto { *; }
-keep class com.gayadi.android.data.model.LegalDocumentSectionDto { *; }
