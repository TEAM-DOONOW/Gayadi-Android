package com.gayadi.android.data.datasource

import com.gayadi.android.data.model.LegalDocumentDto
import com.gayadi.android.data.model.NoticeDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

/** Public backend content, available before login. Requests run asynchronously. */
class RestPublicContentDataSource internal constructor(
    private val api: PublicContentApi,
) : NoticeDataSource, LegalDocumentDataSource {
    constructor(baseUrl: String) : this(createApi(baseUrl))

    override fun loadNotices(callback: (Result<List<NoticeDto>>) -> Unit) {
        val notices = mutableListOf<NoticeDto>()
        fun loadPage(offset: Int) {
            api.notices(PAGE_SIZE, offset).deliver(NOTICE_ERROR) { result ->
                result.fold(
                    onSuccess = { page ->
                        notices.addAll(page)
                        if (page.size == PAGE_SIZE) {
                            loadPage(offset + PAGE_SIZE)
                        } else {
                            callback(Result.success(notices.toList()))
                        }
                    },
                    onFailure = { callback(Result.failure(it)) },
                )
            }
        }
        loadPage(0)
    }

    override fun loadNotice(noticeId: String, callback: (Result<NoticeDto>) -> Unit) {
        if (!validId(noticeId)) {
            callback(Result.failure(IllegalArgumentException(NOTICE_ERROR)))
            return
        }
        api.notice(noticeId).deliver(NOTICE_ERROR, callback)
    }

    override fun loadDocument(documentId: String, callback: (Result<LegalDocumentDto>) -> Unit) {
        if (!validId(documentId)) {
            callback(Result.failure(IllegalArgumentException(DOCUMENT_ERROR)))
            return
        }
        api.document(documentId).deliver(DOCUMENT_ERROR, callback)
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val NOTICE_ERROR = "업데이트 소식을 불러오지 못했어요. 잠시 후 다시 시도해 주세요."
        const val DOCUMENT_ERROR = "문서를 불러오지 못했어요. 잠시 후 다시 시도해 주세요."

        // Content IDs are server slugs, never URL paths or queries.
        fun validId(id: String) = id.matches(Regex("[A-Za-z0-9_-]+"))

        fun createApi(baseUrl: String): PublicContentApi = Retrofit.Builder()
            .baseUrl(baseUrl.trimEnd('/') + "/")
            .client(
                OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .callTimeout(30, TimeUnit.SECONDS)
                    .build(),
            )
            .addConverterFactory(
                MoshiConverterFactory.create(
                    Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build(),
                ),
            )
            .build()
            .create(PublicContentApi::class.java)
    }
}

internal interface PublicContentApi {
    @GET("api/v1/notices")
    fun notices(@Query("limit") limit: Int, @Query("offset") offset: Int): Call<List<NoticeDto>>

    @GET("api/v1/notices/{noticeId}")
    fun notice(@Path("noticeId") noticeId: String): Call<NoticeDto>

    @GET("api/v1/legal-documents/{documentId}")
    fun document(@Path("documentId") documentId: String): Call<LegalDocumentDto>
}

private fun <T> Call<T>.deliver(message: String, callback: (Result<T>) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val body = response.body()
            val result = if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                response.errorBody()?.close()
                Result.failure(IllegalStateException(message))
            }
            callback(result)
        }

        override fun onFailure(call: Call<T>, error: Throwable) {
            // Never surface server response bodies, URLs, or parser diagnostics in the UI.
            callback(Result.failure(IllegalStateException(message)))
        }
    })
}
