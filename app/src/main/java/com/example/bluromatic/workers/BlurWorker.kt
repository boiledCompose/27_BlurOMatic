package com.example.bluromatic.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.bluromatic.DELAY_TIME_MILLIS
import com.example.bluromatic.KEY_BLUR_LEVEL
import com.example.bluromatic.KEY_IMAGE_URI
import com.example.bluromatic.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private const val TAG = "BlurWorker"

class BlurWorker (context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val resourceUri = inputData.getString(KEY_IMAGE_URI)
        val blurLevel = inputData.getInt(KEY_BLUR_LEVEL, 1)

        makeStatusNotification(
            applicationContext.resources.getString(R.string.blurring_image),
            applicationContext
        )
        return withContext(Dispatchers.IO) {
            //이 worker는 매우 빠르게 실행되므로, 코드에 지연을 추가하여 더 느리게 실행되는 작업을 에뮬레이션하는 것이 좋습니다.
            //이 호출은 Codelab에서 알림 메시지 간 지연을 제공하기 위한 목적으로만 사용됩니다.
            delay(DELAY_TIME_MILLIS)

            return@withContext try {
                require(!resourceUri.isNullOrBlank()){
                    val errorMessage = applicationContext.resources.getString(R.string.invalid_input_uri)
                    Log.e(TAG, errorMessage)
                    errorMessage
                }
                val resolver = applicationContext.contentResolver

                val picture = BitmapFactory.decodeResource(
                    applicationContext.resources,
                    R.drawable.android_cupcake
                )

                val output = blurBitmap(picture, blurLevel)

                val outputUri = writeBitmapToFile(applicationContext, output)

                makeStatusNotification(
                    "Output is $outputUri",
                    applicationContext
                )
                val outputData = workDataOf(KEY_IMAGE_URI to outputUri.toString())
                Result.success(outputData)
            } catch (throwable: Throwable) {
                Log.e(
                    TAG,
                    applicationContext.resources.getString(R.string.error_applying_blur),
                    throwable
                )
                Result.failure()
            }
        }
    }

}