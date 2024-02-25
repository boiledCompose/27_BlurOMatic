## WorkManager

WorkManager는 상황별 실행과 보장된 실행을 조합하여 적용해야 하는 백그라운드 작업을 위한 아키텍처 구성요소이다.

- 상황별 실행이란 최대한 빨리 백그라운드 작업을 실행하는 것
- 보장된 실행이란 사용자가 앱을 벗어난 경우를 비롯한 다양한 상황에서 로직을 처리하는 것

### WorkManager 종속 항목

WorkManager는 다음과 같은 종속 항목이 필요하다.

```kotlin
dependencies {
    // WorkManager dependency
    implementation("androidx.work:work-runtime-ktx:2.8.1")  
}
```

### BlurWorker 클래스 생성

1. `worker`는 백그라운드 스레드에서 동기식으로 작업을 실행하는 클래스이다.  
비동기식 작업을 하기 위해선 Kotlin의 코루틴과 상호 운용되는 `CoroutineWorker`를 사용할 수 있다. `CoroutineWorker`안에는 `doWork()`메서드가 필수적으로 재정의되어야 한다.

```kotlin
class BlurWorker(context: Context, params: WorkerParameters) : CouroutineWorker(context, params) {
    override suspend fun doWork(): Result { ... }
}
```

2. WorkManager는 `Result.success()` 및 `Result.failure()`를 사용하여 실행 중인 작업 요청의 최종 상태를 나타낸다. 따라서 `return try ..catch` 코드 블록을 추가한다.
```kotlin
//inside doWork()
return try {
    Result.success()
} catch(throwable: Throwable) {
    Result.failure()
}
```

3. 변환하고자 하는 이미지를 비트맵으로 변환하여 변수에 저장한다.
   이후에 호출되는 `blurBitmap()`은 비트맵을 블러 처리하는 메서드이고,
   `writeBitmapToFile()`은 비트앱을 시스템에 저장하고 Uri를 반환하는 메서드이다.
```kotlin
//inside try block
val picture = BitmapFactory.decodeResource(
                applicationContext.resources,
                R.drawable.android_cupcake
)

//output's type: Bitmap
val output = blurBitmap(picture, 1)
val outputUri = writeBitmapToFile(applicationContext, output)

Result.Success()
```

4. `catch` 블록엔 에러 메세지를 기록한다.
```kotlin
//inside doWork()
} catch (throwable: Throwable) {
    Log.e(
    TAG,
    applicationContext.resources.getString(R.string.error_applying_blur),
        throwable
    )

    Result.failure()
}
```

5. `CoroutineWorker`는 기본적으로 `Dispatchers.Default`로 실행된다. `withContext()`를 호출하고 원하는 디스패처를 전달하여 변경할 수 있다.
```kotlin
//inside doWork(), wrapping try-catch block
return withContext(Dispatchers.IO) {
    ...
}

6. 람다 함수 내에서 return을 호출할 수 없어서 `try-catch` 블록의 return에서 에러가 발생한다. 이러한 경우 상태를 직접 표시하는 라벨을 부착한다.
```kotlin
return withContext(Dispatchers.IO) {
    return@withContext try{
        ...
    } catch ...
}
```

7. worker는 매우 빨리 실행된다. 따라서 약간의 지연을 추가해서 작업의 속도를 늦추도록 한다.
```kotlin
return withContext(Dispatchers.IO) {
    delay(DELAY_TIME_MILLIS)
    ...
}
```

### WorkRequest 추가
WorkRequest에는 2가지 유형이 있다. 이 예제에선 첫 번째 유형을 사용한다.

- `OneTimeWorkRequest`: 한 번만 실행된다.
- `PeriodicWorkRequest`: 일정 주기로 반복적으로 실행된다.

1. `OneTimeWorkerRequestBuilder`라는 확장 함수를 사용해 worker용 `OneTimeWorkRequest` 객체를 만든다.
```kotlin
fun applyBlur(blurLevel: Int){
    val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()
}
```

2. `workManager` 객체의 `enqueue` 메서드를 통해 `WorkRequest`를 실행 큐에 넣는다.
```kotlin
val workManager = WorkManager.getInstance(context)

fun applyBlur(blurLevel: Int){
    val blurBuilder = OneTimeWorkRequestBuilder<BlurWorker>()
    
    workManager.enqueue(blurBuilder.build())
}
```



